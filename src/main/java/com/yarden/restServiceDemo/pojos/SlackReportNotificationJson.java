package com.yarden.restServiceDemo.pojos;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SlackReportNotificationJson {

    @SerializedName("sdk")
    @Expose
    private String sdk;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("version")
    @Expose
    private String version;
    @SerializedName("changeLog")
    @Expose
    private String changeLog;

    @SerializedName("testCoverageGap")
    @Expose
    private String testCoverageGap;
    @SerializedName("reportTitle")
    @Expose
    private String reportTitle;
    @SerializedName("mailTextPart")
    @Expose
    private String mailTextPart;
    @SerializedName("specificRecipient")
    @Expose
    private String specificRecipient;
    @SerializedName("frameworkVersions")
    @Expose
    private String frameworkVersions;

    public String getSdk() {
        return sdk;
    }

    public void setSdk(String sdk) {
        this.sdk = sdk;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLogUrl(String changeLog) {
        this.changeLog = changeLog;
    }

    public String getTestCoverageGap() {
//        return testCoverageGap;
        return "Browser stack (mobile web)\n" +
                "Browser stack (mobile native)\n" +
                "Perfecto mobile (mobile web)\n" +
                "Perfecto mobile (mobile native)\n" +
                "All supported browser versions\n" +
                "Public method names validation\n" +
                "Default configuration validation";
    }

    public void setTestCoverageGap(String testCoverageGap) {
        this.testCoverageGap = testCoverageGap;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getMailTextPart() {
        return mailTextPart;
    }

    public void setMailTextPart(String mailTextPart) {
        this.mailTextPart = mailTextPart;
    }

    public String getSpecificRecipient() {
        return specificRecipient;
    }

    public void setSpecificRecipient(String specificRecipient) {
        this.specificRecipient = specificRecipient;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrameworkVersions() {
        return frameworkVersions;
    }

    public void setFrameworkVersions(String frameworkVersions) {
        this.frameworkVersions = frameworkVersions;
    }

    public SdkResultRequestJson convert() {
        SdkResultRequestJson request = new SdkResultRequestJson();
        request.setId(this.id);
        request.setSdk(this.sdk);
        return request;
    }
}