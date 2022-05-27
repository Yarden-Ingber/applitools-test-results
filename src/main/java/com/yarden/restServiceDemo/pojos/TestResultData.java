package com.yarden.restServiceDemo.pojos;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TestResultData {

    @SerializedName("test_name")
    @Expose
    private String testName;
    @SerializedName("feature")
    @Expose
    private String feature;
    @SerializedName("feature_sub_category")
    @Expose
    private String feature_sub_category;
    @SerializedName("parameters")
    @Expose
    private JsonObject parameters;
    @SerializedName("passed")
    @Expose
    private Boolean passed;
    @SerializedName("result_url")
    @Expose
    private String resultUrl;
    @SerializedName("isGeneric")
    @Expose
    private Boolean isGeneric;
    @SerializedName("isSkipped")
    @Expose
    private Boolean isSkipped;
    @SerializedName("isTestRequiredForSdk")
    @Expose
    private Boolean isTestRequiredForSdk;

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public JsonObject getParameters() {
        return parameters;
    }

    public void setParameters(JsonObject parameters) {
        this.parameters = parameters;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getResultUrl() {
        if (resultUrl == null) {
            return "";
        } else {
            return this.resultUrl;
        }
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getFeature_sub_category() {
        return feature_sub_category;
    }

    public void setFeature_sub_category(String feature_sub_category) {
        this.feature_sub_category = feature_sub_category;
    }

    public boolean isGeneric() {
        return (isGeneric != null && isGeneric);
    }

    public void setGeneric(Boolean generic) {
        isGeneric = generic;
    }

    public boolean isTestRequiredForSdk() {
        return (isTestRequiredForSdk == null || isTestRequiredForSdk);
    }

    public void setIsTestRequiredForSdk(Boolean required) {
        isTestRequiredForSdk = required;
    }

    public boolean isSkipped() {
        return (isSkipped != null && isSkipped);
    }

    public void setSkipped(Boolean skipped) {
        isSkipped = skipped;
    }

    @Override
    public String toString() {
        return "TestResultData{" +
                "testName='" + testName + '\'' +
                ", feature='" + feature + '\'' +
                ", feature_sub_category='" + feature_sub_category + '\'' +
                ", parameters=" + parameters +
                ", passed=" + passed +
                ", resultUrl='" + resultUrl + '\'' +
                ", isGeneric=" + isGeneric +
                ", isSkipped=" + isSkipped +
                ", isRequiredForSdk=" + isTestRequiredForSdk +
                '}';
    }
}