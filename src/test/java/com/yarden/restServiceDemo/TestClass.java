package com.yarden.restServiceDemo;

import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.yarden.restServiceDemo.slackService.dailySdkReport.SdkDailyRegressionReport;
import org.junit.Test;

import java.io.IOException;

public class TestClass {

    @Test
    public void test() throws IOException, MailjetSocketTimeoutException, MailjetException {
        new SdkDailyRegressionReport().send();
    }

}
