package com.yarden.restServiceDemo;

import com.yarden.restServiceDemo.pojos.ReportData;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

public class HtmlReportGenerator {

    StringBuilder htmlReportStringBuilder = new StringBuilder();
    ReportData reportData;

    HtmlReportGenerator(ReportData reportData){
        this.reportData = reportData;
    }

    String getHtmlReportAsPlainSting(){
        addHeadElement();
        htmlReportStringBuilder.append("<body><div class=\"wrapper\">\n" +
                "    <div class=\"content\">\n" +
                "        <div class=\"header\">applitools</div>");
        addPageTitle();
        if (StringUtils.isNotEmpty(reportData.getVersion())) { addVersionSection(); }
        if (StringUtils.isNotEmpty(reportData.getChangeLog())) { addChangeLogSection(); }
        if (StringUtils.isNotEmpty(reportData.getFrameworkVersions())) { addFrameworkVersionsSection(); }
        if (reportData.getHighLevelReportTable() != null) { addHighLevelReportSection(); }
//        if (StringUtils.isNotEmpty(reportData.getCoverageGap())) { addTestCoverageGapSection(); }
        if (reportData.getDetailedPassedTestsTable() != null) { addDetailedPassedTestsSection(); }
//        if (reportData.getDetailedMissingTestsTable() != null){ addDetailedMissingTestsSection(); }
        if (reportData.getDetailedMissingGenericTestsTable() != null){ addDetailedMissingGenericTestsSection(); }
        if (reportData.getDetailedFailedTestsTable() != null) { addDetailedFailedTestsSection(); }
        htmlReportStringBuilder.append("</div></div></body></html>");
        return htmlReportStringBuilder.toString();
    }

    private void addHeadElement() {
        htmlReportStringBuilder.append("<!DOCTYPE html><html><head>");
        htmlReportStringBuilder.append(getCSS());
        htmlReportStringBuilder.append("</head>");
    }

    private void addPageTitle() {
        htmlReportStringBuilder.append("<h1>" + reportData.getReportTitle() + "</h1>");
        htmlReportStringBuilder.append("<h2>" + LocalDate.now().toString() + "</h2>");
    }

    private void addVersionSection() {
        htmlReportStringBuilder.append("<h2>Version</h2>");
        htmlReportStringBuilder.append(versionToList(reportData.getVersion()) + "<br/><br/>");
    }

    private void addChangeLogSection() {
        htmlReportStringBuilder.append("<details open><summary><b>Change log</b></summary>");
        htmlReportStringBuilder.append(reportData.getChangeLog() + "<br/>");
        htmlReportStringBuilder.append("</details><br/>");
    }

    private void addFrameworkVersionsSection() {
        htmlReportStringBuilder.append("<h2>Tested framework versions:</h2>");
        htmlReportStringBuilder.append(versionToList(reportData.getFrameworkVersions()) + "<br/><br/>");
    }

    private void addHighLevelReportSection() {
        htmlReportStringBuilder.append("<h2>Test summary</h2><br/>");
        htmlReportStringBuilder.append(reportData.getHighLevelReportTable());
    }

    private void addDetailedPassedTestsSection() {
        if (reportData.getPassedTestsCount() > 0) {
            htmlReportStringBuilder.append("<br/><details><summary><b>Executed tests (Total: " + (reportData.getPassedTestsCount()+ reportData.getFailedTestsCount())
                    + " Passed: " + reportData.getPassedTestsCount() + " Failed: " + reportData.getFailedTestsCount() + ")</b></summary>");
        } else {
            htmlReportStringBuilder.append("<br/><details><summary><b>Passed tests</b></summary>");
        }
        htmlReportStringBuilder.append(reportData.getDetailedPassedTestsTable());
        htmlReportStringBuilder.append("</details>");
    }

    private void addDetailedMissingGenericTestsSection() {
        htmlReportStringBuilder.append("<br/><details><summary><b>Unexecuted generic tests</b></summary>");
        htmlReportStringBuilder.append(reportData.getDetailedMissingGenericTestsTable());
        htmlReportStringBuilder.append("</details><br/>");
    }

    private void addDetailedFailedTestsSection() {
        htmlReportStringBuilder.append("<br/><details><summary><b>Failed tests</b></summary>");
        htmlReportStringBuilder.append(reportData.getDetailedFailedTestsTable());
        htmlReportStringBuilder.append("</details><br/>");
    }

    private void addTestCoverageGapSection() {
        htmlReportStringBuilder.append("<br/><h2>Test coverage gap</h2>");
        htmlReportStringBuilder.append(reportData.getCoverageGap() + "<br/><br/>");
    }

    private void addDetailedMissingTestsSection() {
        htmlReportStringBuilder.append("<br/><details><summary><b>Unexecuted tests</b></summary>");
        htmlReportStringBuilder.append(reportData.getDetailedMissingTestsTable());
        htmlReportStringBuilder.append("</details><br/>");
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
