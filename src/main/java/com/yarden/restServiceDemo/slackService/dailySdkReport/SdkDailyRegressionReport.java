package com.yarden.restServiceDemo.slackService.dailySdkReport;

import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.awsS3Service.AwsS3Provider;
import com.yarden.restServiceDemo.mailService.MailSender;
import com.yarden.restServiceDemo.pojos.ReportData;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

public class SdkDailyRegressionReport {

    static final String sdkResultsCsvFilePath = "sdk_test_results.csv";
    final String sdkDailyRegressionResultFileName = "sdk_test_results_" + Logger.getTimaStamp().replaceAll(" ", "_").replace('.', ':') + ".csv";

    public void send() throws IOException, MailjetSocketTimeoutException, MailjetException {
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

}
