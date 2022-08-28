package com.yarden.restServiceDemo.kpis;

import com.google.gson.Gson;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.reportService.WriteEntireSheetsPeriodically;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class KpisRestCalls {

    public void test() throws IOException {
        state_update("{\"team\":\"JS SDKs\",\"sub_project\":\"\",\"ticket_id\":\"ike58Acv\",\"ticket_title\":\"Storybook RFE | Add option to not fail the test suite when diffs are found\",\"created_by\":\"Rivka Beck\",\"ticket_url\":\"https://trello.com/c/ike58Acv\",\"state\":\"New\",\"current_trello_list\":\"New\",\"ticket_type\":\"\",\"workaround\":\"\",\"labels\":\"\"}");
    }

    @RequestMapping(method = RequestMethod.POST, path = "/state_update")
    public ResponseEntity state_update(@RequestBody String json) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        WriteEntireSheetsPeriodically.shouldStopSheetWritingTimer = false;
        WriteEntireSheetsPeriodically.start();
        newRequestPrint(json, "/state_update");
        TicketUpdateRequest ticketUpdateRequest = new Gson().fromJson(json, TicketUpdateRequest.class);
        TrelloUpdateRequestQueue.addStateUpdateRequestToQueue(ticketUpdateRequest);
        stopWatch.stop();
        Logger.info("/state_update took: " + stopWatch.getTime(TimeUnit.MILLISECONDS));
        return new ResponseEntity(ticketUpdateRequest.toString(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/archive_card")
    public ResponseEntity archive_card(@RequestBody String json) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        WriteEntireSheetsPeriodically.shouldStopSheetWritingTimer = false;
        WriteEntireSheetsPeriodically.start();
        newRequestPrint(json, "/archive_card");
        TicketUpdateRequest ticketUpdateRequest = new Gson().fromJson(json, TicketUpdateRequest.class);
        TrelloUpdateRequestQueue.addArchiveTicketRequestToQueue(ticketUpdateRequest);
        stopWatch.stop();
        Logger.info("/archive_card took: " + stopWatch.getTime(TimeUnit.MILLISECONDS));
        return new ResponseEntity(ticketUpdateRequest.toString(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/dump_kpi_tickets_to_splunk_manually")
    public ResponseEntity archive_card() throws ParseException {
        newRequestPrint("", "/dump_kpi_tickets_to_splunk_manually");
        new WriteKpisToSplunkPeriodically().periodicDumpTickets();
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping(value = "/get_create_ticket_page", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String get_create_ticket_page() throws IOException, UnirestException {
        return TrelloTicketCreator.getTicketCreationFormHtml();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/get_trello_ticket_url")
    public String get_trello_ticket_url(@RequestParam String requestID) {
        String ticketUrl = TrelloTicketCreator.getTrelloTicketUrl(requestID);
        Logger.info("KPIs: request for a created ticket url: " + ticketUrl);
        return ticketUrl;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/create_trello_ticket")
    public void create_trello_ticket(@RequestParam(name="account") String account,
                                     @RequestParam(name="boards") String board,
                                     @RequestParam(name="ticketTitle") String ticketTitle,
                                     @RequestParam(name="ticketDescription") String ticketDescription,
                                     @RequestParam(name="requestID") String requestID,
                                     @RequestParam(required=false,name="customerAppUrl") String customerAppUrl,
                                     @RequestParam(required=false,name="sdk") String sdk,
                                     @RequestParam(required=false,name="sdkVersion") String sdkVersion,
                                     @RequestParam(required=false,name="linkToTestResults") String linkToTestResults,
                                     @RequestParam(required=false,name="isAccessible") String isAppAccessible,
                                     @RequestParam(required=false,name="renderID") String renderID,
                                     @RequestParam(required=false,name="zendeskCustomerName") String zendeskCustomerName,
                                     @RequestParam(required=false,name="zendeskCompanyName") String zendeskCompanyName,
                                     @RequestParam(required=false,name="zendeskUrl") String zendeskUrl,
                                     @RequestParam(required=false,name="zendeskTier") String zendeskTier,
                                     @RequestParam(required=false,name="zendeskCustomerType") String zendeskCustomerType,
                                     @RequestParam(required=false,name="logFiles") MultipartFile[] logFiles,
                                     @RequestParam(required=false,name="reproducible") MultipartFile[] reproducibleFiles,
                                     @RequestParam(required=false,name="extraAccounts") String extraAccounts,
                                     @RequestParam(required=false,name="workaround") String workaround,
                                     @RequestParam(required=false,name="blocker") String blocker,
                                     @RequestParam(required=false,name="extraFiles1") MultipartFile[] extraFiles1,
                                     @RequestParam(required=false,name="extraFiles2") MultipartFile[] extraFiles2,
                                     @RequestParam(required=false,name="extraFiles3") MultipartFile[] extraFiles3,
                                     @RequestParam(required=false,name="extraFiles4") MultipartFile[] extraFiles4,
                                     @RequestParam(required=false,name="extraFiles5") MultipartFile[] extraFiles5,
                                     ModelMap ticketFormFields) {
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.requestID.name(), requestID);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.accountName.name(), account.split(TrelloTicketCreator.AccountsSeparator)[0]);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.accountID.name(), account.split(TrelloTicketCreator.AccountsSeparator)[1]);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.board.name(), board.split(",")[0]);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.listID.name(), board.split(",")[1]);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.ticketTitle.name(), ticketTitle);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.ticketDescription.name(), ticketDescription);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.customerAppUrl.name(), customerAppUrl == null ? "" : customerAppUrl);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.sdk.name(), sdk == null ? "" : sdk);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.sdkVersion.name(), sdkVersion == null ? "" : sdkVersion);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.extraAccounts.name(), extraAccounts == null ? "" : extraAccounts);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.linkToTestResults.name(), linkToTestResults == null ? "" : linkToTestResults);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.logFiles.name(), logFiles);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.reproducibleFiles.name(), reproducibleFiles);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.isAppAccessible.name(), isAppAccessible == null ? "" : isAppAccessible);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.renderID.name(), renderID == null ? "" : renderID);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.zendeskCustomerName.name(), zendeskCustomerName == null ? "" : zendeskCustomerName);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.zendeskCompanyName.name(), zendeskCompanyName == null ? "" : zendeskCompanyName);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.zendeskUrl.name(), zendeskUrl == null ? "" : zendeskUrl);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.zendeskTier.name(), zendeskTier == null ? "" : zendeskTier);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.zendeskCustomerType.name(), zendeskCustomerType == null ? "" : zendeskCustomerType);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.workaround.name(), workaround == null ? false : true);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.blocker.name(), blocker == null ? false : true);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.extraFiles1.name(), extraFiles1);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.extraFiles2.name(), extraFiles2);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.extraFiles3.name(), extraFiles3);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.extraFiles4.name(), extraFiles4);
        ticketFormFields.addAttribute(TrelloTicketCreator.FormFields.extraFiles5.name(), extraFiles5);
        Logger.info("KPIs: Trello ticket creation request: " + ticketFormFields.toString());
        try {
            TrelloTicketCreator.createTicket(ticketFormFields);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    private void newRequestPrint(String json, String request){
        Logger.info("KPIs: **********************************************************************************************");
        Logger.info("KPIs: **********************************************************************************************");
        Logger.info("KPIs: New KPI request detected: " + request + " === payload: " + json);
    }
}
