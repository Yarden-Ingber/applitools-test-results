package com.yarden.restServiceDemo.slackService.dailySdkReport;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.awsS3Service.AwsS3Provider;
import com.yarden.restServiceDemo.firebaseService.FirebaseResultsJsonsService;
import com.yarden.restServiceDemo.mailService.MailSender;
import com.yarden.restServiceDemo.pojos.ReportData;
import com.yarden.restServiceDemo.pojos.SdkResultRequestJson;
import com.yarden.restServiceDemo.reportService.SdkReportService;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import javassist.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.List;

public class SdkDailyRegressionReport {

    static final String sdkResultsCsvFilePath = "sdk_test_results.csv";
    final String sdkDailyRegressionResultFileName = "sdk_test_results_" + Logger.getTimaStamp().replaceAll(" ", "_").replace('.', ':') + ".csv";

    public void send(String json) throws IOException, MailjetSocketTimeoutException, MailjetException {
        try {
            SdkResultRequestJson requestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
            dumpResultsFromFirebaseToSheet(requestJson);
        } catch (JsonSyntaxException e) {
            Logger.warn("SdkDailyRegressionReport: Failed to parse json for daily sdk regression report request");
        }
        DailySdkHighLevelReportTableBuilder dailySdkHighLevelReportTableBuilder = new DailySdkHighLevelReportTableBuilder();
        ReportData reportData = new ReportData()
                .setReportTextPart("The daily SDK regression has finished.\n\n")
                .setReportTitle("Daily SDK regression report - " + dailySdkHighLevelReportTableBuilder.suiteResultString)
                .setMailSubject("Daily SDK regression report - " + dailySdkHighLevelReportTableBuilder.suiteResultString)
                .setRecipientsJsonArray(new JSONArray()
                        .put(new JSONObject()
                                .put("Email", "yarden.ingber@applitools.com")
                                .put("Name", "Release_Report"))
//                        .put(new JSONObject()
//                                .put("Email", "adam.carmi@applitools.com")
//                                .put("Name", "Release_Report"))
//                        .put(new JSONObject()
//                                .put("Email", "amit.zur@applitools.com")
//                                .put("Name", "Release_Report"))
//                        .put(new JSONObject()
//                                .put("Email", "daniel.puterman@applitools.com")
//                                .put("Name", "Release_Report"))
//                        .put(new JSONObject()
//                                .put("Email", "kyrylo.onufriiev@applitools.com")
//                                .put("Name", "Release_Report"))
                );
        reportData.setReportTextPart(reportData.getReportTextPart() + dailySdkHighLevelReportTableBuilder.getFailedSdksHtml() +
                "<br>" + dailySdkHighLevelReportTableBuilder.getHighLevelReportTable());
        reportData.setReportTextPart(reportData.getReportTextPart().replace("\n", "<br/>") +
                "<br><br>Results CSV file:<br>" + uplodaReportCsvToS3());
        new MailSender().send(reportData);
    }

    private String uplodaReportCsvToS3() throws FileNotFoundException, UnsupportedEncodingException {
        SheetData reportSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, Enums.SdkGroupsSheetTabNames.Selenium.value));
        PrintWriter writer = new PrintWriter(sdkResultsCsvFilePath, "UTF-8");
        try {
            writer.println(reportSheet.getSheetDataAsCsvString());
        } finally {
            writer.close();
        }
        AwsS3Provider.uploadFileToS3(Enums.EnvVariables.AwsS3SdkReportsBucketName.value, sdkDailyRegressionResultFileName, sdkResultsCsvFilePath);
        String fileUrl = AwsS3Provider.getUrlToFileInS3(Enums.EnvVariables.AwsS3SdkReportsBucketName.value, sdkDailyRegressionResultFileName);
        try {
            new File(sdkResultsCsvFilePath).delete();
        } catch (Throwable t) {t.printStackTrace();}
        return fileUrl;
    }

    private void dumpResultsFromFirebaseToSheet(SdkResultRequestJson request) throws IOException {
        if (StringUtils.isNotEmpty(request.getId()) && !request.getId().equals("null")) {
            SheetData reportSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, Enums.SdkGroupsSheetTabNames.Selenium.value));
            List<String> sdks = reportSheet.getColumnNames();
            sdks.remove(Enums.SdkSheetColumnNames.TestName.value);
            for (String sdk : sdks) {
                try {
                    FirebaseResultsJsonsService.dumpMappedRequestsToFirebase();
                    request.setSdk(sdk);
                    request.setGroup(Enums.SdkGroupsSheetTabNames.Selenium.value);
                    String json = FirebaseResultsJsonsService.getCurrentSdkRequestFromFirebase(request);
                    SdkResultRequestJson sdkResultRequestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
                    Logger.info("SdkDailyRegressionReport: Dumping request from firebase for: " + request.getSdk() + "-" + request.getId());
                    new SdkReportService().postResults(sdkResultRequestJson);
                } catch (NotFoundException e) {
                    Logger.warn("SdkDailyRegressionReport: Failed to dump request from firebase to sheet for sdk: " + request.getSdk() + " group: " + request.getGroup() + " id: " + request.getId());
                }
            }
            SheetData.writeAllTabsToSheet();
        }
    }

}
