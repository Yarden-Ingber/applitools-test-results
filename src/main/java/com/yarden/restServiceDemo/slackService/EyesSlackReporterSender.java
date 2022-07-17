package com.yarden.restServiceDemo.slackService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.htmlReportService.HtmlReport;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.awsS3Service.AwsS3Provider;
import com.yarden.restServiceDemo.firebaseService.FirebaseResultsJsonsService;
import com.yarden.restServiceDemo.mailService.MailSender;
import com.yarden.restServiceDemo.pojos.ReportData;
import com.yarden.restServiceDemo.pojos.SlackReportNotificationJson;
import com.yarden.restServiceDemo.reportService.EyesReportService;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import javassist.NotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class EyesSlackReporterSender {

    private SlackReportNotificationJson requestJson;
    private static final String EndedTestTasksCounterFile = "EndedTestTasksCounterFile.txt";
    private static final String SplitString = "Split;Sign";
    private static final int NumOfTestTasks = Integer.parseInt(Enums.EnvVariables.EyesTestTasksCount.value);

    public void send(String json) throws IOException, MailjetSocketTimeoutException, MailjetException {
        requestJson = new Gson().fromJson(json, SlackReportNotificationJson.class);
        dumpResultsFromFirebaseToSheet(requestJson);
        if (requestJson.getId() == null || requestJson.getId().isEmpty()) {
            requestJson.setId(UUID.randomUUID().toString().substring(0, 6));
        }
        if (isAllTestsEnded()) {
            sendReport();
        }
        updateEndTasksCounterFile();
    }

    public synchronized void resetEndTasksCounter(){
        wrtieNewEndTasksCounter(new EndTasksCounterObject("0", 0));
    }

    private void dumpResultsFromFirebaseToSheet(SlackReportNotificationJson requestJson) throws IOException {
        for (Enums.EyesSheetTabsNames group : Enums.EyesSheetTabsNames.values()) {
            try {
                FirebaseResultsJsonsService.dumpMappedRequestsToFirebase();
                new EyesReportService().postResults(FirebaseResultsJsonsService.getCurrentEyesRequestFromFirebase(requestJson.getId(), group.value));
            } catch (NotFoundException e) {
                Logger.error("EyesSlackReporterSender: Failed to dump request from firebase to sheet for group: " + group + " id: " + requestJson.getId());
            }
        }
        SheetData.writeAllTabsToSheet();
    }

    private void sendReport() throws IOException, MailjetSocketTimeoutException, MailjetException{
        HTMLTableBuilder highLevelReportTable = getHighLevelReportTable();
        ReportData reportData = new ReportData()
                .setReportTextPart("A new version of Eyes is about to be released.")
                .setReportTitle("Test report for Eyes")
                .setMailSubject("Test report for Eyes")
                .setChangeLog(requestJson.getChangeLog())
                .setVersion(requestJson.getVersion())
                .setHighLevelReportTable(highLevelReportTable)
                .setDetailedPassedTestsTable(getDetailedPassedTestsTable())
                .setHtmlReportS3BucketName(Enums.EnvVariables.AwsS3EyesReportsBucketName.value);
        reportData.setReportTextPart(reportData.getReportTextPart() +
                "<br>" + highLevelReportTable);
        reportData.setReportTextPart(reportData.getReportTextPart().replace("\n", "<br/>") +
                "<br><br>HTML Report:<br>" + HtmlReport.generate(reportData));
        setRecipientMail(reportData);
//        if (requestJson.getSpecificRecipient() == null || requestJson.getSpecificRecipient().isEmpty()){
//            new SlackReporter().report(slackReportData);
//        }
        new MailSender().send(reportData);
    }

    private synchronized boolean isAllTestsEnded() throws IOException {
        EndTasksCounterObject counterObject = getEndTasksCounter();
        if (counterObject.id.equals(requestJson.getId()) || NumOfTestTasks == 1) {
            int count = counterObject.counter + 1;
            if (count >= NumOfTestTasks) {
                return true;
            }
        }
        return false;
    }

    private synchronized void updateEndTasksCounterFile() throws IOException {
        EndTasksCounterObject counterObject = getEndTasksCounter();
        if (!counterObject.id.equals(requestJson.getId())) {
            wrtieNewEndTasksCounter(new EndTasksCounterObject(requestJson.getId(), 1));
        } else {
            int count = counterObject.counter + 1;
            if (count >= NumOfTestTasks) {
                wrtieNewEndTasksCounter(new EndTasksCounterObject("0", 0));
            } else {
                wrtieNewEndTasksCounter(new EndTasksCounterObject(requestJson.getId(), counterObject.counter + 1));
            }
        }
    }

    private synchronized EndTasksCounterObject getEndTasksCounter() throws IOException {
        String[] endedTestTasksCounterStringArray = AwsS3Provider.getStringFromFile(Enums.EnvVariables.AwsS3EyesReportsBucketName.value, EndedTestTasksCounterFile).split(SplitString);
        return new EndTasksCounterObject(endedTestTasksCounterStringArray[0], Integer.parseInt(endedTestTasksCounterStringArray[1]));
    }

    private synchronized void wrtieNewEndTasksCounter(EndTasksCounterObject counterObject) {
        AwsS3Provider.writeStringToFile(Enums.EnvVariables.AwsS3EyesReportsBucketName.value, EndedTestTasksCounterFile, counterObject.id + SplitString + counterObject.counter);
    }

    private class EndTasksCounterObject {
        public String id;
        public int counter;

        public EndTasksCounterObject(String id, int counter) {
            this.id = id;
            this.counter = counter;
        }
    }

    private HTMLTableBuilder getHighLevelReportTable() {
        HTMLTableBuilder tableBuilder = new HTMLTableBuilder(false, 2, 3);
        tableBuilder.addTableHeader("Success percentage", "Passed test count", "Previous release test count");
        String previousTestCountFileName = "EyesPreviousTestCount.txt";
        String previousTestCount = "";
        String passedTestCount = Integer.toString(getPassedTestCount());
        String successPercentage = Integer.toString(Math.round(((float)getPassedTestCount() / getTotalTestCount()) * 100));
        try {
            previousTestCount = AwsS3Provider.getStringFromFile(Enums.EnvVariables.AwsS3EyesReportsBucketName.value, previousTestCountFileName);
        } catch (Throwable t) { t.printStackTrace(); }
        AwsS3Provider.writeStringToFile(Enums.EnvVariables.AwsS3EyesReportsBucketName.value, previousTestCountFileName, passedTestCount);
        tableBuilder.addRowValues(true, successPercentage, passedTestCount, previousTestCount);
        return tableBuilder;
    }

    private HTMLTableBuilder getDetailedPassedTestsTable() {
        HTMLTableBuilder tableBuilder = new HTMLTableBuilder(false, 2, 4);
        tableBuilder.addTableHeader("<div align=\"left\">Test name</div>", "Feature", "Feature sub-category", "Result");
        for (Enums.EyesSheetTabsNames eyesReportTab: Enums.EyesSheetTabsNames.values()) {
            if (eyesReportTab != Enums.EyesSheetTabsNames.Sandbox) {
                JsonArray reportSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.Eyes.value, eyesReportTab.value)).getSheetData();
                for (JsonElement row: reportSheet) {
                    if (!row.getAsJsonObject().get(Enums.EyesSheetColumnNames.TestName.value).getAsString().equals(Enums.EyesSheetColumnNames.TimestampRow.value)
                            && !row.getAsJsonObject().get(Enums.EyesSheetColumnNames.TestName.value).getAsString().equals(Enums.EyesSheetColumnNames.IDRow.value)
                            && row.getAsJsonObject().get(Enums.EyesSheetColumnNames.Status.value).getAsString().equals(Enums.TestResults.Passed.value)) {
                        tableBuilder.addRowValues(false, row.getAsJsonObject().get(Enums.EyesSheetColumnNames.TestName.value).getAsString(),
                                row.getAsJsonObject().get(Enums.EyesSheetColumnNames.Feature.value).getAsString(),
                                row.getAsJsonObject().get(Enums.EyesSheetColumnNames.FeatureSubCategory.value).getAsString(),
                                "PASS");
                    }
                }
            }
        }
        return tableBuilder;
    }

    private int getTotalTestCount(){
        int totalAmount = 0;
        for (Enums.EyesSheetTabsNames eyesReportTab: Enums.EyesSheetTabsNames.values()) {
            if (eyesReportTab != Enums.EyesSheetTabsNames.Sandbox) {
                JsonArray reportSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.Eyes.value, eyesReportTab.value)).getSheetData();
                for (JsonElement sheetEntry: reportSheet){
                    if (!sheetEntry.getAsJsonObject().get(Enums.EyesSheetColumnNames.TestName.value).getAsString().equals(Enums.EyesSheetColumnNames.TimestampRow.value)
                    && !sheetEntry.getAsJsonObject().get(Enums.EyesSheetColumnNames.TestName.value).getAsString().equals(Enums.EyesSheetColumnNames.IDRow.value)
                    && (sheetEntry.getAsJsonObject().get(Enums.EyesSheetColumnNames.Status.value).getAsString().equals(Enums.TestResults.Passed.value) ||
                            sheetEntry.getAsJsonObject().get(Enums.EyesSheetColumnNames.Status.value).getAsString().equals(Enums.TestResults.Failed.value))){
                        totalAmount++;
                    }
                }
            }
        }
        return totalAmount;
    }

    private int getPassedTestCount(){
        int passedAmount = 0;
        for (Enums.EyesSheetTabsNames eyesReportTab: Enums.EyesSheetTabsNames.values()) {
            if (eyesReportTab != Enums.EyesSheetTabsNames.Sandbox) {
                JsonArray reportSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.Eyes.value, eyesReportTab.value)).getSheetData();
                for (JsonElement sheetEntry: reportSheet){
                    if (!sheetEntry.getAsJsonObject().get(Enums.EyesSheetColumnNames.TestName.value).getAsString().equals(Enums.EyesSheetColumnNames.TimestampRow.value)
                            && !sheetEntry.getAsJsonObject().get(Enums.EyesSheetColumnNames.TestName.value).getAsString().equals(Enums.EyesSheetColumnNames.IDRow.value)
                            && sheetEntry.getAsJsonObject().get(Enums.EyesSheetColumnNames.Status.value).getAsString().equals(Enums.TestResults.Passed.value)){
                        passedAmount++;
                    }
                }
            }
        }
        return passedAmount;
    }

    private void setRecipientMail(ReportData reportData) {
        String recipientMail = "";
        if (requestJson.getSpecificRecipient() != null && !requestJson.getSpecificRecipient().isEmpty()) {
            recipientMail = requestJson.getSpecificRecipient();
        } else {
            recipientMail = "yarden.ingber@applitools.com";
        }
        reportData.setRecipientsJsonArray(new JSONArray().put(new JSONObject().put("Email", recipientMail).put("Name", "Release_Report")));
    }
}
