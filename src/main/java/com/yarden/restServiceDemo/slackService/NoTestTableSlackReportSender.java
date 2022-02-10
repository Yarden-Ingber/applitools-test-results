package com.yarden.restServiceDemo.slackService;

import com.google.gson.Gson;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.HtmlReportGenerator;
import com.yarden.restServiceDemo.mailService.MailSender;
import com.yarden.restServiceDemo.pojos.SlackReportData;
import com.yarden.restServiceDemo.pojos.SlackReportNotificationJson;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class NoTestTableSlackReportSender {

    public void send(String json) throws MailjetSocketTimeoutException, MailjetException, FileNotFoundException, UnsupportedEncodingException {
        SlackReportNotificationJson requestJson = new Gson().fromJson(json, SlackReportNotificationJson.class);
        String recipient = StringUtils.isEmpty(requestJson.getSpecificRecipient()) ? "yarden.ingber@applitools.com" : requestJson.getSpecificRecipient();
        SlackReportData slackReportData = new SlackReportData()
                .setSdk(requestJson.getSdk())
                .setVersion(getVersion(requestJson))
                .setChangeLog(requestJson.getChangeLog())
                .setFrameworkVersions(requestJson.getFrameworkVersions())
                .setReportTextPart("A new SDK is about to be released.\n\nSDK: " + requestJson.getSdk() + "\nVersion:\n* " + getVersion(requestJson) + "\n")
                .setReportTitle("Release report for SDK: " + requestJson.getSdk())
                .setMailSubject("Release report for SDK: " + requestJson.getSdk())
                .setRecipientsJsonArray(new JSONArray()
                        .put(new JSONObject()
                                .put("Email", recipient)
                                .put("Name", "Release_Report")))
                .setHtmlReportS3BucketName(Enums.EnvVariables.AwsS3SdkReportsBucketName.value)
                .setMailingGroupId(SlackReportData.MailingGroups.ReleaseReports);
        slackReportData.setHtmlReportUrl(new HtmlReportGenerator(slackReportData).getHtmlReportUrlInAwsS3(slackReportData.getHtmlReportS3BucketName()));
        new MailSender().send(slackReportData);
    }

    private String getVersion(SlackReportNotificationJson requestJson){
        return requestJson.getVersion()
                .replace("RELEASE_CANDIDATE;", "")
                .replaceAll("RELEASE_CANDIDATE-", "")
                .replaceAll("@", " ");
    }

}