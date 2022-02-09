package com.yarden.restServiceDemo.pojos;

import org.apache.commons.lang3.StringUtils;
import com.yarden.restServiceDemo.slackService.HTMLTableBuilder;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.json.JSONArray;

public class SlackReportData {

    private String sdk;
    private String reportTextPart;
    private HTMLTableBuilder detailedMissingTestsTable = null;
    private HTMLTableBuilder highLevelReportTable = null;
    private HTMLTableBuilder detailedPassedTestsTable = null;
    private HTMLTableBuilder detailedFailedTestsTable = null;
    private String reportTitle = "";
    private String mailSubject = "";
    private String changeLog = "";
    private String version = "";
    private String frameworkVersions = "";
    private String coverageGap = "";
    private JSONArray recipientsJsonArray = null;
    private String htmlReportS3BucketName = "";
    private String htmlReportUrl = "";
    private int passedTestsCount = 0;
    private int failedTestsCount = 0;
    private int missingTestsCount = 0;
    private MailingGroups mailingGroupId = null;

    public String getReportTitle() {
        return reportTitle;
    }

    public SlackReportData setReportTitle(String reportTitle) {
        this.reportTitle = fixNewLineForHtml(reportTitle);
        return this;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public SlackReportData setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
        return this;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public SlackReportData setChangeLog(String changeLog) {
        if (changeLog == null) {
            throw new NullPointerException("Change log value is null");
        }
        Parser parser = Parser.builder().build();
        Node document = parser.parse(changeLog);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        this.changeLog = renderer.render(document);
        return this;
    }

    public String getVersion() {
        return version;
    }

    public SlackReportData setVersion(String version) {
        this.version = version.replaceAll("RELEASE_CANDIDATE-", "").replaceAll("RELEASE_CANDIDATE", "");
        return this;
    }

    public String getCoverageGap() {
        return coverageGap;
    }

    public SlackReportData setCoverageGap(String coverageGap) {
        this.coverageGap = fixNewLineForHtml(multilineCapitalize(coverageGap));
        return this;
    }

    public String getReportTextPart() {
        return reportTextPart;
    }

    public SlackReportData setReportTextPart(String reportTextPart) {
        this.reportTextPart = reportTextPart;
        return this;
    }

    public HTMLTableBuilder getDetailedMissingTestsTable() {
        return detailedMissingTestsTable;
    }

    public SlackReportData setDetailedMissingTestsTable(HTMLTableBuilder detailedMissingTestsTable) {
        this.detailedMissingTestsTable = detailedMissingTestsTable;
        return this;
    }

    public HTMLTableBuilder getHighLevelReportTable() {
        return highLevelReportTable;
    }

    public SlackReportData setHighLevelReportTable(HTMLTableBuilder highLevelReportTable) {
        this.highLevelReportTable = highLevelReportTable;
        return this;
    }

    public HTMLTableBuilder getDetailedPassedTestsTable() {
        return detailedPassedTestsTable;
    }

    public SlackReportData setDetailedPassedTestsTable(HTMLTableBuilder detailedPassedTestsTable) {
        this.detailedPassedTestsTable = detailedPassedTestsTable;
        return this;
    }

    public JSONArray getRecipientsJsonArray() {
        return recipientsJsonArray;
    }

    public SlackReportData setRecipientsJsonArray(JSONArray recipientsJsonArray) {
        this.recipientsJsonArray = recipientsJsonArray;
        return this;
    }

    private String fixNewLineForHtml(String string){
        return string.replace("\n", "<br/>").replace(" ", "&nbsp;");
    }

    private String multilineCapitalize(String text){
        String result = "";
        String[] lines = text.split("\n");
        for (String line: lines) {
            result = result + StringUtils.capitalize(line) + "\n";
        }
        return result;
    }

    public String getHtmlReportS3BucketName() {
        return htmlReportS3BucketName;
    }

    public SlackReportData setHtmlReportS3BucketName(String htmlReportS3BucketName) {
        this.htmlReportS3BucketName = htmlReportS3BucketName;
        return this;
    }

    public String getHtmlReportUrl() {
        return htmlReportUrl;
    }

    public SlackReportData setHtmlReportUrl(String htmlReportUrl) {
        this.htmlReportUrl = htmlReportUrl;
        return this;
    }

    public HTMLTableBuilder getDetailedFailedTestsTable() {
        return detailedFailedTestsTable;
    }

    public SlackReportData setDetailedFailedTestsTable(HTMLTableBuilder detailedFailedTestsTable) {
        this.detailedFailedTestsTable = detailedFailedTestsTable;
        return this;
    }

    public String getSdk() {
        return sdk;
    }

    public SlackReportData setSdk(String sdk) {
        this.sdk = sdk;
        return this;
    }

    public int getPassedTestsCount() {
        return passedTestsCount;
    }

    public SlackReportData setPassedTestsCount(int passedTestsCount) {
        this.passedTestsCount = passedTestsCount;
        return this;
    }

    public int getFailedTestsCount() {
        return failedTestsCount;
    }

    public SlackReportData setFailedTestsCount(int failedTestsCount) {
        this.failedTestsCount = failedTestsCount;
        return this;
    }

    public int getMissingTestsCount() {
        return missingTestsCount;
    }

    public SlackReportData setMissingTestsCount(int missingTestsCount) {
        this.missingTestsCount = missingTestsCount;
        return this;
    }

    public String getFrameworkVersions() {
        return frameworkVersions;
    }

    public SlackReportData setFrameworkVersions(String frameworkVersions) {
        this.frameworkVersions = frameworkVersions;
        return this;
    }

    public SlackReportData setMailingGroupId (MailingGroups mailingGroup) {
        this.mailingGroupId = mailingGroup;
        return this;
    }

    public MailingGroups getMailingGroupId () {
        return this.mailingGroupId;
    }

    public enum MailingGroups {
        ReleaseReports("61a32dfb-b8f4-4104-9be5-495e447d57f4"), Ops("7a4e31b8-e12e-4619-9e75-906ef8474517");

        public String id;

        MailingGroups(String id) {
            this.id = id;
        }
    }
}
