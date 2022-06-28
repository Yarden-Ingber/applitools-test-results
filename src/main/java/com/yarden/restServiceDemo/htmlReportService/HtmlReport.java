package com.yarden.restServiceDemo.htmlReportService;

import com.yarden.restServiceDemo.pojos.ReportData;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class HtmlReport {

    private static final String htmlReportFileName = "test_report.html";

    public static String generate(ReportData reportData) throws FileNotFoundException, UnsupportedEncodingException {
        generateLocalHtmlReportFile(reportData);
        return uploadReportToS3(reportData);
    }

    private static void generateLocalHtmlReportFile(ReportData reportData) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(htmlReportFileName, "UTF-8");
        try {
            writer.println(new HtmlReportGenerator(reportData).getHtmlReportAsPlainSting());
        } finally {
            writer.close();
        }
    }

    private static String uploadReportToS3(ReportData reportData) {
        return new HtmlReportUploader(reportData).upload(reportData.getHtmlReportS3BucketName(), htmlReportFileName);
    }

}
