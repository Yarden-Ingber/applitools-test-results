package com.yarden.restServiceDemo.productionMonitor;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;
import com.splunk.JobExportArgs;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import com.yarden.restServiceDemo.splunkService.SplunkReporter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Configuration
public class ProductionMonitor extends TimerTask {

    private static boolean isRunning = false;
    private static Timer timer;
    private static final String VERSION = "4";

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("ProductionMonitor");
            timer.scheduleAtFixedRate(new ProductionMonitor(), 30, 1000 * 60 * 10);
            isRunning = true;
            Logger.info("ProductionMonitor started");
        }
    }

    @Override
    public void run() {
        try {
            Logger.info("ProductionMonitor: Starting monitor");
            monitor();
            Logger.info("ProductionMonitor: Monitor ended");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void monitor() throws IOException, MailjetSocketTimeoutException, MailjetException {
        try {
            Logger.info("ProductionMonitor: Reporting Eyes endpoints");
            sendEyesEndpointsEvents();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            Logger.info("ProductionMonitor: Reporting VG status");
            sendVGEvent();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void sendEyesEndpointsEvents() throws IOException, MailjetSocketTimeoutException, MailjetException {
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_MONTH, -4);
        String query = "data.Info.RequestType=GetUserInfo OR data.Info.RequestType=StartSession OR data.Info.RequestType=MatchExpectedOutputAsSession | rex field=data.Context.RequestUrl \"(?<domain>https://.*\\.applitools.com)\" | stats count by domain data.Site | where NOT LIKE(domain, \"https://test%\") | rename data.Site as site | table domain site";
        String theString = new SplunkReporter().getDataFromSplunk(query, calendar.getTime(), today, JobExportArgs.OutputMode.CSV);
        new SplunkReporter().report(Enums.SplunkSourceTypes.ProductionMonitor, new JSONObject().put("eventType", "log").put("value", theString).put("domainsCount", StringUtils.countMatches(theString, "://")).toString());
        theString = theString.replace("\"", "").replace("domain,site\n", "");
        String[] domainsSitesList = theString.split("\n");
        StringBuilder failedEndpoints = new StringBuilder("");
        for (String domainSite : domainsSitesList) {
            String domain = domainSite.split(",")[0];
            String site = domainSite.split(",")[1];
            if (!(site.equalsIgnoreCase("Sqli") || site.equalsIgnoreCase("Unitymedia"))) {
                domain = domain + "/api/admin/userinfo";
                URL endpoint = new URL(domain);
                JSONObject productionMonitorEventJson = new JSONObject();
                productionMonitorEventJson.put("version", VERSION);
                productionMonitorEventJson.put("site", site);
                productionMonitorEventJson.put("domain", domain);
                productionMonitorEventJson.put("eventType", "Endpoint");
                try {
                    int responseStatusCode = 0;
                    try {
                        responseStatusCode = getEndpointRequestSession(endpoint).getResponseCode();
                    } catch (Throwable t) {
                        try {
                            responseStatusCode = getEndpointRequestSession(endpoint).getResponseCode();
                        } catch (Throwable t2) {
                            responseStatusCode = getEndpointRequestSession(endpoint).getResponseCode();
                        }
                    }
                    if (responseStatusCode == 200 || responseStatusCode == 403) {
                        productionMonitorEventJson.put("isUp", 1);
                    } else {
                        Logger.error("ProductionMonitor: Status code for site " + site + " is: " + responseStatusCode);
                        productionMonitorEventJson.put("isUp", 0);
                        failedEndpoints.append(site).append(";");
                        productionMonitorEventJson.put("statusCode", responseStatusCode);
                    }
                } catch (Throwable t) {
                    Logger.error("ProductionMonitor: failed to get response from endpoint " + domain);
                    t.printStackTrace();
                    productionMonitorEventJson.put("isUp", 0);
                    failedEndpoints.append(site).append(";");
                }
                productionMonitorEventJson.put("uuid", UUID.randomUUID().toString().substring(0, 8));
                new SplunkReporter().report(Enums.SplunkSourceTypes.ProductionMonitor, productionMonitorEventJson.toString());
            } else {
                Logger.info("ProductionMonitor: site=" + site + ", domain=" + domain + " was skipped in monitor");
            }
        }
        if (!failedEndpoints.toString().isEmpty()) {
            sendEndpointMailNotification(failedEndpoints.toString());
        }
    }

    private HttpURLConnection getEndpointRequestSession(URL endpoint) throws IOException {
        HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();
        con.setConnectTimeout(1000 * 60 * 5);
        con.setRequestMethod("GET");
        return con;
    }

    private void sendVGEvent() throws MailjetSocketTimeoutException, MailjetException {
        int failedBrowsers = 0;
        int totalBrowsers = 0;
        SheetData vgStatusSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.VisualGrid.value, Enums.VisualGridSheetTabsNames.Status.value));
        vgStatusSheet.getSheetData();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        for (String browser : vgStatusSheet.getColumnNames()) {
            if (!browser.equals(Enums.VisualGridSheetColumnNames.Timestamp.value)) {
                totalBrowsers++;
                JSONObject productionMonitorEventJson = new JSONObject();
                productionMonitorEventJson.put("version", VERSION);
                productionMonitorEventJson.put("eventType", "VG");
                productionMonitorEventJson.put("Browser", browser);
                if (vgStatusSheet.getSheetData().get(0).getAsJsonObject().get(browser).getAsString().equals(Enums.TestResults.Passed.value)) {
                    productionMonitorEventJson.put("isUp", 1);
                } else {
                    failedBrowsers++;
                    productionMonitorEventJson.put("isUp", 0);
                }
                productionMonitorEventJson.put("uuid", uuid);
                new SplunkReporter().report(Enums.SplunkSourceTypes.ProductionMonitor, productionMonitorEventJson.toString());
            }
        }
        if ((totalBrowsers/2) < failedBrowsers) {
            sendVGMailNotification(uuid);
        }
    }

    private void sendVGMailNotification(String uuid) throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipient = new JSONArray().put(new JSONObject().put("Email", "eyesops@applitools.com").put("Name", "Production_monitor"));
        sendMailNotification(recipient, "Production monitor alert", "Alert that more than 50% of the browsers in the VG failed \n\n uuid: " + uuid);
    }

    private void sendEndpointMailNotification(String endpoint) throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipient = new JSONArray().put(new JSONObject().put("Email", "eyesops@applitools.com").put("Name", "Production_monitor"));
        sendMailNotification(recipient, "Production monitor alert", "The GET request for endpoints: " + endpoint + " failed");
    }

    private void sendMailNotification(JSONArray recipient, String subject, String content) throws MailjetSocketTimeoutException, MailjetException {
        MailjetClient client;
        MailjetRequest request;
        MailjetResponse response;
        client = new MailjetClient(Enums.EnvVariables.MailjetApiKeyPublic.value, Enums.EnvVariables.MailjetApiKeyPrivate.value, new ClientOptions("v3.1"));
        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", "yarden.ingber@applitools.com")
                                        .put("Name", "Yarden Ingber"))
                                .put(Emailv31.Message.TO, recipient)
                                .put(Emailv31.Message.SUBJECT, subject)
                                .put(Emailv31.Message.TEXTPART, content)
                                .put(Emailv31.Message.CUSTOMID, "ProductionMonitor")));
        response = client.post(request);
        Logger.info(Integer.toString(response.getStatus()));
        Logger.info(response.getData().toString());
    }

}
