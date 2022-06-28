package com.yarden.restServiceDemo;

import com.lowagie.text.DocumentException;
import com.mailjet.client.Base64;
import com.yarden.restServiceDemo.awsS3Service.AwsS3Provider;
import com.yarden.restServiceDemo.pojos.ReportData;
import org.apache.commons.io.IOUtils;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.time.LocalDate;

public class HtmlReportGenerator {

    StringBuilder htmlReportStringBuilder = new StringBuilder();
    ReportData reportData;
    private final String htmlReportFileName = "test_report.html";
    private final String pdfReportFileName = "test_report.pdf";

    public HtmlReportGenerator(ReportData reportData){
        this.reportData = reportData;
    }

    public String getHtmlReportUrlInAwsS3(String bucketName) throws FileNotFoundException, UnsupportedEncodingException {
        generateHtmlReportFile();
        String fileNameInBucket = getFileNameInBucket();
        AwsS3Provider.uploadFileToS3(bucketName, fileNameInBucket, htmlReportFileName);
        String fileUrl = AwsS3Provider.getUrlToFileInS3(bucketName, fileNameInBucket);
        try {
            new File(htmlReportFileName).delete();
        } catch (Throwable t) {t.printStackTrace();}
        return fileUrl;
    }

    public String getPdfReportAsBase64() throws IOException, DocumentException {
        generateHtmlReportFile();
        convertHtmlToPdfFile();
        String result = Base64.encode(IOUtils.toByteArray(new FileInputStream(pdfReportFileName)));
        try {
            new File(htmlReportFileName).delete();
            new File(pdfReportFileName).delete();
        } catch (Throwable t) {t.printStackTrace();}
        return result;
    }

    private void generateHtmlReportFile() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(htmlReportFileName, "UTF-8");
        try {
            writer.println(getHtmlReportAsPlainSting());
        } finally {
            writer.close();
        }
    }

    private void convertHtmlToPdfFile() throws IOException, DocumentException{
        String inputFile = htmlReportFileName;
        String url = new File(inputFile).toURI().toURL().toString();
        String outputFile = pdfReportFileName;
        OutputStream os = new FileOutputStream(outputFile);
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(url);
            renderer.layout();
            renderer.createPDF(os);
        } finally {
            os.close();
        }
    }

    private String getHtmlReportAsPlainSting(){
        htmlReportStringBuilder.append("<!DOCTYPE html><html><head>");
        htmlReportStringBuilder.append(getCSS());
        htmlReportStringBuilder.append("</head><body><div class=\"wrapper\">\n" +
                "    <div class=\"content\">\n" +
                "        <div class=\"header\">applitools</div>");
        htmlReportStringBuilder.append("<h1>" + reportData.getReportTitle() + "</h1>");
        htmlReportStringBuilder.append("<h2>" + LocalDate.now().toString() + "</h2>");
        if (reportData.getVersion() != null && !reportData.getVersion().isEmpty()) {
            htmlReportStringBuilder.append("<h2>Version</h2>");
            htmlReportStringBuilder.append(versionToList(reportData.getVersion()) + "<br/><br/>");
        }
        if (reportData.getChangeLog() != null && !reportData.getChangeLog().isEmpty()) {
            htmlReportStringBuilder.append("<details open><summary><b>Change log</b></summary>");
            htmlReportStringBuilder.append(reportData.getChangeLog() + "<br/>");
            htmlReportStringBuilder.append("</details><br/>");
        }
        if (reportData.getFrameworkVersions() != null && !reportData.getFrameworkVersions().isEmpty()) {
            htmlReportStringBuilder.append("<h2>Tested framework versions:</h2>");
            htmlReportStringBuilder.append(versionToList(reportData.getFrameworkVersions()) + "<br/><br/>");
        }
        if (reportData.getHighLevelReportTable() != null) {
            htmlReportStringBuilder.append("<h2>Test summary</h2><br/>");
            htmlReportStringBuilder.append(reportData.getHighLevelReportTable());
        }
//        if (slackReportData.getCoverageGap() != null && !slackReportData.getCoverageGap().isEmpty()) {
//            htmlReportStringBuilder.append("<br/><h2>Test coverage gap</h2>");
//            htmlReportStringBuilder.append(slackReportData.getCoverageGap() + "<br/><br/>");
//        }
        if (reportData.getDetailedPassedTestsTable() != null) {
            if (reportData.getPassedTestsCount() > 0) {
                htmlReportStringBuilder.append("<br/><details><summary><b>Executed tests (Total: " + (reportData.getPassedTestsCount()+ reportData.getFailedTestsCount())
                        + " Passed: " + reportData.getPassedTestsCount() + " Failed: " + reportData.getFailedTestsCount() + ")</b></summary>");
            } else {
                htmlReportStringBuilder.append("<br/><details><summary><b>Passed tests</b></summary>");
            }
            htmlReportStringBuilder.append(reportData.getDetailedPassedTestsTable());
            htmlReportStringBuilder.append("</details>");
        }
//        if (slackReportData.getDetailedMissingTestsTable() != null){
//            htmlReportStringBuilder.append("<br/><details><summary><b>Unexecuted tests</b></summary>");
//            htmlReportStringBuilder.append(slackReportData.getDetailedMissingTestsTable());
//            htmlReportStringBuilder.append("</details><br/>");
//        }
        if (reportData.getDetailedMissingGenericTestsTable() != null){
            htmlReportStringBuilder.append("<br/><details><summary><b>Unexecuted generic tests</b></summary>");
            htmlReportStringBuilder.append(reportData.getDetailedMissingGenericTestsTable());
            htmlReportStringBuilder.append("</details><br/>");
        }
        if (reportData.getDetailedFailedTestsTable() != null) {
            htmlReportStringBuilder.append("<br/><details><summary><b>Failed tests</b></summary>");
            htmlReportStringBuilder.append(reportData.getDetailedFailedTestsTable());
            htmlReportStringBuilder.append("</details><br/>");
        }
        htmlReportStringBuilder.append("</div></div></body></html>");
        return htmlReportStringBuilder.toString();
    }

    private String getCSS(){
        return "<style type=\"text/css\">\n" +
                "    h1 {\n" +
                "        font-size: 1.5em;\n" +
                "    }\n" +
                "    h2 {\n" +
                "        font-size: 1.25em;\n" +
                "    }\n" +
                "    h3 {\n" +
                "        font-size: 1em;\n" +
                "    }\n" +
                "    summary {\n" +
                "        font-size: 1.25em;\n" +
                "    }\n" +
                "    .content {\n" +
                "        background:#ffffff;\n" +
                "        margin: 40px auto;\n" +
                "        width: 65%;\n" +
                "        padding: 30px;\n" +
                "        box-shadow: 0 10px 10px #c7ced0;\n" +
                "        border:1px solid #c7ced0;\n" +
                "    }\n" +
                "    .wrapper {\n" +
                "        background: #e4f0f4;\n" +
                "        padding: 1px;\n" +
                "        font-family: sans-serif;\n" +
                "    }\n" +
                "    .header {\n" +
                "        margin: -30px -30px 0;\n" +
                "        padding: 17px 30px;\n" +
                "        color: white;\n" +
                "        background: #3ab8ac;\n" +
                "        font-size: 24px;\n" +
                "        font-weight: bold;\n" +
                "    }\n" +
                "    body {\n" +
                "        margin: 0;\n" +
                "    }\n" +
                "    table {\n" +
                "        width: 70%;\n" +
                "    }\n" +
                "    tr {\n" +
                "        background: #f8f8f8;\n" +
                "    }\n" +
                "    td, th {\n" +
                "        padding: 3px 12px;\n" +
                "    }\n" +
                "    .fail {\n" +
                "       background: #b74938;\n" +
                "       color: white;\n" +
                "       font-family: sans-serif;\n" +
                "    }\n" +
                "    .pass {\n" +
                "       background: #34a87d;\n" +
                "       color: white;\n" +
                "       font-family: sans-serif;\n" +
                "    }\n" +
                "</style>";
    }

    private String getFileNameInBucket(){
        if (reportData.getSdk() != null && !reportData.getSdk().isEmpty()
            && reportData.getVersion() != null && !reportData.getVersion().isEmpty()) {
            return "report" + "_" + reportData.getSdk() + "_" + reportData.getVersion();
        } else {
            return "report" + "_" + Logger.getTimaStamp().replaceAll(" ", "_").replace('.', ':');
        }
    }

    private String versionToList(String version){
        String result = "<ul>";
        String[] versionParts = version.split(";");
        for (String part: versionParts) {
            result = result + "<li>" + part + "</li>";
        }
        result = result + "</ul>";
        return result;
    }

}
