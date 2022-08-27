package com.yarden.restServiceDemo.kpis;

import com.google.gson.JsonElement;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.RestCalls;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Configuration
public class KpisSummaryReporter extends TimerTask {

    private static boolean isRunning = false;
    private static Timer timer;
    private static boolean isReportsSent = false;

    private StringBuilder jsTicketsInNew;
    private StringBuilder jsTicketsWithoutType;
    private StringBuilder jsMissingQuality;
    private StringBuilder jsUnlabelled;

    private StringBuilder sdksTicketsInNew;
    private StringBuilder sdksTicketsWithoutType;
    private StringBuilder sdksMissingQuality;
    private StringBuilder sdksUnlabelled;

    private StringBuilder eyesBackendTicketsInNew;
    private StringBuilder eyesBackendTicketsWithoutType;
    private StringBuilder eyesBackendUnlabelled;

    private StringBuilder eyesFrontendTicketsInNew;
    private StringBuilder eyesFrontendTicketsWithoutType;
    private StringBuilder eyesFrontendUnlabelled;

    private StringBuilder ufgTicketsInNew;
    private StringBuilder ufgTicketsWithoutType;
    private StringBuilder ufgUnlabelled;

    private StringBuilder algoUnlabelled;

    private StringBuilder androidNmgUnlabelled;

    private StringBuilder iosNmgUnlabelled;

    private StringBuilder fieldTickets;

    private static final int SixMonthsAgo = 6;

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("KpisSummaryReporter");
            timer.scheduleAtFixedRate(new KpisSummaryReporter(), 30, 1000 * 60 * 10);
            isRunning = true;
            Logger.info("KpisSummaryReporter started");
        }
    }

    @Override
    public void run() {
        try {
            synchronized (RestCalls.lock) {
                try {
                    if (shouldSendReport()) {
                        sendReports();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean shouldSendReport() throws ParseException {
        Date date = Logger.timestampToDate(Logger.getTimaStamp());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        if (day == Calendar.SUNDAY && hour == 6 && minute < 30) {
            Logger.info("KpisSummaryReporter: Report window is open. Don't restart server!!!!!!!!!!!!!!");
            if (!isReportsSent) {
                isReportsSent = true;
                return true;
            }
        } else {
            isReportsSent = false;
        }
        return false;
    }

    private void sendReports() throws MailjetSocketTimeoutException, MailjetException {
        resetTeamsStrings();
        SheetData rawDataSheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.KPIS.value, Enums.KPIsSheetTabsNames.RawData.value));
        buildStringReports(rawDataSheetData);
        sendJsMailReport();
        sendSDKsMailReport();
        sendUFGMailReport();
        sendEyesMailReport();
        sendEyesFrontendMailReport();
        sendFieldMailReport();
        sendAlgoMailReport();
        sendiOSNmgMailReport();
        sendAndroidNmgMailReport();
    }

    private void resetTeamsStrings() {
        String ticketsWithoutTypeTitle = "Tickets without type field (not classified yet):\n";
        String ticketsInStateNewTitle = "Tickets under NEW column:\n";
        String ticketsInStateMissingQualityTitle = "Tickets in missing quality:\n";
        String unlabelledTickets = "Unlabelled tickets (no Field or Internal labels):\n";

        jsTicketsInNew = new StringBuilder(ticketsInStateNewTitle);
        jsTicketsWithoutType = new StringBuilder(ticketsWithoutTypeTitle);
        jsMissingQuality = new StringBuilder(ticketsInStateMissingQualityTitle);
        jsUnlabelled = new StringBuilder(unlabelledTickets);

        sdksTicketsInNew = new StringBuilder(ticketsInStateNewTitle);
        sdksTicketsWithoutType = new StringBuilder(ticketsWithoutTypeTitle);
        sdksMissingQuality = new StringBuilder(ticketsInStateMissingQualityTitle);
        sdksUnlabelled = new StringBuilder(unlabelledTickets);

        eyesBackendTicketsInNew = new StringBuilder(ticketsInStateNewTitle);
        eyesBackendTicketsWithoutType = new StringBuilder(ticketsWithoutTypeTitle);
        eyesBackendUnlabelled = new StringBuilder(unlabelledTickets);

        eyesFrontendTicketsInNew = new StringBuilder(ticketsInStateNewTitle);
        eyesFrontendTicketsWithoutType = new StringBuilder(ticketsWithoutTypeTitle);
        eyesFrontendUnlabelled = new StringBuilder(unlabelledTickets);

        ufgTicketsInNew = new StringBuilder(ticketsInStateNewTitle);
        ufgTicketsWithoutType = new StringBuilder(ticketsWithoutTypeTitle);
        ufgUnlabelled = new StringBuilder(unlabelledTickets);

        algoUnlabelled = new StringBuilder(unlabelledTickets);

        androidNmgUnlabelled = new StringBuilder(unlabelledTickets);

        iosNmgUnlabelled = new StringBuilder(unlabelledTickets);

        fieldTickets = new StringBuilder("Tickets on field:\n");
    }

    private void buildStringReports(SheetData rawDataSheetData) {
        for (JsonElement sheetEntry: rawDataSheetData.getSheetData()){
            try {
                String team = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString();
                if (!team.equals(Enums.Strings.Archived.value)) {
                    TicketStates currentState = TicketStates.valueOf(sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CurrentState.value).getAsString());
                    String ticketType = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.TicketType.value).getAsString();
                    if (isTicketOnField(currentState, team)) {
                        String createdBy = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CreatedBy.value).getAsString();
                        String createdByString = createdBy.isEmpty() ? "" : "(Created by: " + createdBy + ")";
                        fieldTickets.append(getSingleTicketLineString(sheetEntry, true) + " " + createdByString);
                    }
                    if (isTicketRelevantForUnlabelled(sheetEntry, currentState)) {
                        addUnlabelledTicketToStringReport(sheetEntry);
                    }

                    if (currentState.equals(TicketStates.New)) {
                        addStateNewTicketToStringReport(sheetEntry);
                    } else if (StringUtils.isEmpty(ticketType) && isTicketRelevantForMissingTypeField(sheetEntry)) {
                        addTicketsWithoutTypeToStringReport(sheetEntry);
                    } else if (currentState.equals(TicketStates.MissingQuality)) {
                        addTicketInMissingQualityToStringReport(sheetEntry);
                    }
                }
            } catch (Throwable t) {
                Logger.error("KpisSummaryReporter: Failed to add ticket to KPI summary report");
                t.printStackTrace();
            }
        }
    }

    private boolean isTicketOnField(TicketStates currentState, String team) {
        return !team.equals(TicketsNewStateResolver.Boards.AlgoBugs.value) &&
                (currentState.equals(TicketStates.WaitingForFieldApproval) ||
                currentState.equals(TicketStates.WaitingForFieldInput) ||
                currentState.equals(TicketStates.WaitingForCustomerResponse));
    }

    private boolean isTicketRelevantForUnlabelled(JsonElement sheetEntry, TicketStates currentState) {
        String labels = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Labels.value).getAsString();
        boolean isLabelExist = labels.contains(Enums.Strings.Field.value) || labels.contains(Enums.Strings.Internal.value);
        return ! (isLabelExist || currentState.equals(TicketStates.Done));
    }

    private boolean isTicketRelevantForMissingTypeField(JsonElement sheetEntry) throws ParseException {
        String movedToDoneString = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.MovedToStateDone.value).getAsString();
        TicketStates currentState = TicketStates.valueOf(sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CurrentState.value).getAsString());
        return
                (currentState.equals(TicketStates.Done) && isDateWithinTimeSpan(Logger.timestampToDate(movedToDoneString), SixMonthsAgo)) ||
                currentState.equals(TicketStates.WaitingForFieldApproval);
    }

    private void sendMailReports(String mailSubject, String mailContent, JSONArray recipients) throws MailjetSocketTimeoutException, MailjetException {
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
                                .put(Emailv31.Message.TO, recipients)
                                .put(Emailv31.Message.SUBJECT, mailSubject)
                                .put(Emailv31.Message.TEXTPART, mailContent)
                                .put(Emailv31.Message.CUSTOMID, "KpiReport")));
        Logger.info("KpisSummaryReporter: Sending report with subject: " + mailSubject);
        response = client.post(request);
    }

    private void addStateNewTicketToStringReport(JsonElement sheetEntry) throws ParseException {
        String team = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString();
        String newTicketString = getSingleTicketLineString(sheetEntry, true);
        if (team.equals(TicketsNewStateResolver.Boards.UltrafastGrid.value)) {
            ufgTicketsInNew.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.JSSDKs.value)) {
            jsTicketsInNew.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.SDKs.value)) {
            sdksTicketsInNew.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.EyesBackend.value) || team.equals(TicketsNewStateResolver.Boards.EyesAppIssues.value)) {
            eyesBackendTicketsInNew.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.EyesFrontend.value)) {
            eyesFrontendTicketsInNew.append(newTicketString);
        }
    }

    private void addTicketInMissingQualityToStringReport(JsonElement sheetEntry) throws ParseException {
        String team = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString();
        String newTicketString = getSingleTicketLineString(sheetEntry, false);
        if (team.equals(TicketsNewStateResolver.Boards.JSSDKs.value)) {
            jsMissingQuality.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.SDKs.value)) {
            sdksMissingQuality.append(newTicketString);
        }
    }

    private void addTicketsWithoutTypeToStringReport(JsonElement sheetEntry) throws ParseException {
        String team = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString();
        String newTicketString = getSingleTicketLineString(sheetEntry, false);
        if (team.equals(TicketsNewStateResolver.Boards.UltrafastGrid.value)) {
            ufgTicketsWithoutType.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.JSSDKs.value)) {
            jsTicketsWithoutType.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.SDKs.value)) {
            sdksTicketsWithoutType.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.EyesBackend.value) || team.equals(TicketsNewStateResolver.Boards.EyesAppIssues.value)) {
            eyesBackendTicketsWithoutType.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.EyesFrontend.value)) {
            eyesFrontendTicketsWithoutType.append(newTicketString);
        }
    }

    private void addUnlabelledTicketToStringReport(JsonElement sheetEntry) throws ParseException {
        String team = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString();
        String newTicketString = getSingleTicketLineString(sheetEntry, false);
        if (team.equals(TicketsNewStateResolver.Boards.UltrafastGrid.value)) {
            ufgUnlabelled.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.JSSDKs.value)) {
            jsUnlabelled.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.SDKs.value)) {
            sdksUnlabelled.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.EyesBackend.value) || team.equals(TicketsNewStateResolver.Boards.EyesAppIssues.value)) {
            eyesBackendUnlabelled.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.EyesFrontend.value)) {
            eyesFrontendUnlabelled.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.AlgoBugs.value)) {
            algoUnlabelled.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.IosNmg.value)) {
            iosNmgUnlabelled.append(newTicketString);
        } else if (team.equals(TicketsNewStateResolver.Boards.AndroidNmg.value)) {
            androidNmgUnlabelled.append(newTicketString);
        }
    }

    private String getSingleTicketLineString(JsonElement sheetEntry, boolean showTicketLifetime) throws ParseException {
        String ticketUrl = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.TicketUrl.value).getAsString();
        String hot = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Labels.value).getAsString().toLowerCase().contains("hot") ? " (HOT)" : "";
        String closed = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CurrentState.value).getAsString().equals(TicketStates.Done.name()) ? " (Closed)" : "";
        String newTicketString = "\n" + ticketUrl;
        if (showTicketLifetime) {
            newTicketString = newTicketString + " (Ticket lifetime: " + getTicketDurationUntilToday(sheetEntry) +
                    " days ===== Time in state NEW: " + getTicketDurationInStateNew(sheetEntry) + " days) " + hot + closed;
        }
        return newTicketString;
    }

    private long getTicketDurationUntilToday(JsonElement sheetEntry) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        Date creationDate = Logger.timestampToDate(sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CreationDate.value).getAsString());
        return TimeUnit.DAYS.convert(today.getTime() - creationDate.getTime(), TimeUnit.MILLISECONDS);
    }

    private long getTicketDurationInStateNew(JsonElement sheetEntry) {
        String timeInNew = sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CalculatedTimeInState.value + TicketStates.New.name()).getAsString();
        if (StringUtils.isEmpty(timeInNew)) {
            return 0;
        } else {
            return Long.parseLong(timeInNew) / 24;
        }
    }

    private void sendJsMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "ben.babayoff@applitools.com").put("Name", "Ben Babayoff"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "amit.zur@applitools.com").put("Name", "Amit Zur"));
        sendMailReports("JS SDKs Trello board report", jsTicketsInNew + "\n\n" + jsTicketsWithoutType + "\n\n" + jsMissingQuality + "\n\n" + jsUnlabelled, recipients);
    }

    private void sendSDKsMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "ben.babayoff@applitools.com").put("Name", "Ben Babayoff"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "daniel.puterman@applitools.com").put("Name", "Daniel Puterman"));
        sendMailReports("SDKs Trello board report", sdksTicketsInNew + "\n\n" + sdksTicketsWithoutType + "\n\n" + sdksMissingQuality + "\n\n" + sdksUnlabelled, recipients);
    }

    private void sendUFGMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "ben.babayoff@applitools.com").put("Name", "Ben Babayoff"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "or.david@applitools.com").put("Name", "Or David"));
        sendMailReports("UFG Trello board report", ufgTicketsInNew + "\n\n" + ufgTicketsWithoutType + "\n\n" + ufgUnlabelled, recipients);
    }

    private void sendEyesMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "yotam.madem@applitools.com").put("Name", "Yotam Madem"));
        sendMailReports("Eyes Backend Trello board report", eyesBackendTicketsInNew + "\n\n" + eyesBackendTicketsWithoutType + "\n\n" + eyesBackendUnlabelled, recipients);
    }

    private void sendEyesFrontendMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "amit.zur@applitools.com").put("Name", "Amit Zur"));
        sendMailReports("Eyes Frontend Trello board report", eyesFrontendTicketsInNew + "\n\n" + eyesFrontendTicketsWithoutType + "\n\n" + eyesFrontendUnlabelled, recipients);
    }

    private void sendFieldMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "yarden.naveh@applitools.com").put("Name", "Yarden Naveh"));
        recipients.put(new JSONObject().put("Email", "liran.barokas@applitools.com").put("Name", "Liran Barokas"));
        recipients.put(new JSONObject().put("Email", "matt.jasaitis@applitools.com").put("Name", "Matt Jasaitis"));
        recipients.put(new JSONObject().put("Email", "patrick.mccartney@applitools.com").put("Name", "Patrick McCartney"));
        recipients.put(new JSONObject().put("Email", "satish.mallela@applitools.com").put("Name", "Satish Mallela"));
        sendMailReports("Field Trello tickets report", fieldTickets.toString(), recipients);
    }

    private void sendAlgoMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "ram.nathaniel@applitools.com").put("Name", "Ram Nathaniel"));
        sendMailReports("Algo Trello board report", algoUnlabelled.toString(), recipients);
    }

    private void sendAndroidNmgMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "daniel.puterman@applitools.com").put("Name", "Daniel Puterman"));
        sendMailReports("Android NMG Trello board report", androidNmgUnlabelled.toString(), recipients);
    }

    private void sendiOSNmgMailReport() throws MailjetSocketTimeoutException, MailjetException {
        JSONArray recipients = new JSONArray();
        recipients.put(new JSONObject().put("Email", "yarden.ingber@applitools.com").put("Name", "Yarden Ingber"));
        recipients.put(new JSONObject().put("Email", "adam.carmi@applitools.com").put("Name", "Adam Carmi"));
        recipients.put(new JSONObject().put("Email", "daniel.puterman@applitools.com").put("Name", "Daniel Puterman"));
        sendMailReports("iOS NMG Trello board report", iosNmgUnlabelled.toString(), recipients);
    }

    private boolean isDateWithinTimeSpan (Date date, int numOfMonthsAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -numOfMonthsAgo);
        Date dateMonthsAgo = calendar.getTime();
        return dateMonthsAgo.before(date);
    }

}
