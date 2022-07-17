package com.yarden.restServiceDemo.slackService;

import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.pojos.ReportData;
import com.yarden.restServiceDemo.pojos.SdkReportMessage;

import java.io.IOException;

public class SlackReporter {

    public void report(ReportData reportData, String htmlReportUrl) throws IOException {
        SdkReportMessage message = new SdkReportMessage(reportData.getReportTextPart() + "\n\nHTML Report:\n" + htmlReportUrl);
        SlackRetrofitClient.getAPIService().sendReport(Enums.EnvVariables.SlackSdkReleaseChannelEndpoint.value, message).execute();
    }

}
