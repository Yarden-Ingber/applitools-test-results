package com.yarden.restServiceDemo.kpis;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.reportService.SdkReportService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ui.ModelMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class TrelloTicketCreator {

    private static final String sdks = "java,java appium,python,ruby,dotnet,espresso,xcui,earlgrey,php,images,DOM capture,UFT,XCTest,DOM snapshot,Integrations,Storybook,Cypress,Testcafe,JS Selenium 4,JS Selenium 3,WDIO 4,WDIO 5,Protractor,Playwright,Nightwatch,Puppeteer,Selenium IDE,JS images,Integrations,Not relevant";
    private static AtomicReference<Map> ticketUrls = new AtomicReference<>();
    public static final String AccountsSeparator = "@";

    public static String getTicketCreationFormHtml() throws IOException, UnirestException {
        InputStream inputStream = SdkReportService.class.getResourceAsStream("/create-ticket-page.html");
        String page = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        page = page.replaceAll("<<<ACCOUNTS>>>", getTrelloAccountsHtmlOptions());
        page = page.replace("<<<SDKS>>>", getSdksHtmlOptions());
        inputStream.close();
        return page;
    }

    public static String getTrelloTicketUrl(String requestID) {
        return (String)ticketUrls.get().get(requestID);
    }

    private static String getSdksHtmlOptions(){
        StringBuilder stringBuilder = new StringBuilder("\n");
        String[] sdksList = sdks.split(",");
        for (String sdk : sdksList) {
            String option = "<option value=\"" + sdk + "\">" + sdk + "</option>\n";
            stringBuilder.append(option);
        }
        return stringBuilder.toString();
    }

    private static String getTrelloAccountsHtmlOptions() throws UnirestException {
        JSONArray accountsArray = TrelloApi.getTrelloAccountsArray();
        return convertAccountsArrayToHtmlOptions(accountsArray);
    }

    private static String convertAccountsArrayToHtmlOptions(JSONArray accountsArray) {
        StringBuilder stringBuilder = new StringBuilder("\n");
        int arraySize = accountsArray.length();
        for (int i = 0; i < arraySize ; i++) {
            String memberName = (String)((JSONObject)accountsArray.get(i)).get("fullName");
            String memberId = (String)((JSONObject)accountsArray.get(i)).get("id");
            String option = "<option value=\"" + memberName + AccountsSeparator + memberId + "\">" + memberName + "</option>\n";
            stringBuilder.append(option);
        }
        return stringBuilder.toString();
    }

    public static void createTicket(ModelMap ticketFormFields) throws UnirestException {
        addTicketDetailsToDescription(ticketFormFields);
        JSONObject createResponse = TrelloApi.createTicket(ticketFormFields);
        if (ticketUrls.get() == null) {
            ticketUrls.set(new HashMap());
        }
        ticketUrls.get().put(ticketFormFields.get(FormFields.requestID.name()), createResponse.getString("shortUrl"));
        String ticketId = createResponse.getString("id");
        updateCustomFields(ticketFormFields, ticketId);
        TrelloApi.addMemberToTicket(ticketId, ticketFormFields);
        MultipartFile[] logFiles = ((MultipartFile[])ticketFormFields.get(FormFields.logFiles.name()));
        TrelloApi.uploadFilesToTicket(ticketId, logFiles);
        MultipartFile[] reproducibleFiles = ((MultipartFile[])ticketFormFields.get(FormFields.reproducibleFiles.name()));
        TrelloApi.uploadFilesToTicket(ticketId, reproducibleFiles);
        uploadExtraFiles(ticketId, ticketFormFields);
        Logger.info("TrelloTicketCreator: Ticket " + ticketId + " created");
    }

    private static void uploadExtraFiles(String ticketId, ModelMap ticketFormFields) throws UnirestException {
        for (int i = 1; i < 6; i++) {
            MultipartFile[] extraFiles = ((MultipartFile[])ticketFormFields.get(FormFields.valueOf("extraFiles" + i).name()));
            TrelloApi.uploadFilesToTicket(ticketId, extraFiles);
        }
    }

    private static void addTicketDetailsToDescription(ModelMap ticketFormFields) {
        Logger.info("TrelloTicketCreator: Before adding, the description is: " + ticketFormFields.get(FormFields.ticketDescription.name()));
        String ticketDescription = (String)ticketFormFields.get(FormFields.ticketDescription.name());
        ticketDescription = ticketDescription == null ? "" : ticketDescription;
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.customerAppUrl.name()))) {
            ticketDescription = ticketDescription + "\n\nCustomer app url: " + ticketFormFields.get(FormFields.customerAppUrl.name());
        }
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.sdk.name()))) {
            ticketDescription = ticketDescription + "\n\nSDK: " + ticketFormFields.get(FormFields.sdk.name());
        }
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.sdkVersion.name()))) {
            ticketDescription = ticketDescription + "\n\nSDK version: " + ticketFormFields.get(FormFields.sdkVersion.name());
        }
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.linkToTestResults.name()))) {
            ticketDescription = ticketDescription + "\n\nEyes dashboard test results: " + ticketFormFields.get(FormFields.linkToTestResults.name());
        }
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.isAppAccessible.name()))) {
            ticketDescription = ticketDescription + "\n\nIs customer app accessible: " + ticketFormFields.get(FormFields.isAppAccessible.name());
        }
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.renderID.name()))) {
            ticketDescription = ticketDescription + "\n\nRender ID: " + ticketFormFields.get(FormFields.renderID.name());
        }
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.zendeskCompanyName.name()))) {
            ticketDescription = ticketDescription + "\n\nZendesk company name: " + ticketFormFields.get(FormFields.zendeskCompanyName.name());
        }
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.renderID.name()))) {
            ticketDescription = ticketDescription + "\n\nRender ID: " + ticketFormFields.get(FormFields.renderID.name());
        }
        Logger.info("TrelloTicketCreator: Adding to description accountName: " + ticketFormFields.get(FormFields.accountName.name()));
        if (StringUtils.isNotEmpty((String)ticketFormFields.get(FormFields.accountName.name()))) {
            ticketDescription = ticketDescription + "\n\nCreated by: " + ticketFormFields.get(FormFields.accountName.name());
        }
        ticketFormFields.addAttribute(FormFields.ticketDescription.name(), ticketDescription);
        Logger.info("TrelloTicketCreator: After adding 1, the description is: " + ticketFormFields.get(FormFields.ticketDescription.name()));
        Logger.info("TrelloTicketCreator: After adding 2, the description is: " + ticketDescription);
    }

    private static void updateCustomFields(ModelMap ticketFormFields, String ticketId) {
        String fieldName;String fieldValue;
        fieldName = "KPI SUB PROJECT";
        fieldValue = (String)ticketFormFields.get(FormFields.sdk.name());
        TrelloApi.updateDropdownCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "Created by";
        fieldValue = (String)ticketFormFields.get(FormFields.accountName.name());
        TrelloApi.updateStringCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "Affected Versions";
        fieldValue = (String)ticketFormFields.get(FormFields.sdkVersion.name());
        TrelloApi.updateStringCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "SDK";
        fieldValue = (String)ticketFormFields.get(FormFields.sdk.name());
        TrelloApi.updateDropdownCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "Render ID";
        fieldValue = (String)ticketFormFields.get(FormFields.renderID.name());
        TrelloApi.updateStringCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "ZENDESK URL";
        fieldValue = (String)ticketFormFields.get(FormFields.zendeskUrl.name());
        TrelloApi.updateStringCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "CUSTOMER'S NAME";
        fieldValue = (String)ticketFormFields.get(FormFields.zendeskCustomerName.name());
        TrelloApi.updateStringCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "COMPANY'S NAME";
        fieldValue = (String)ticketFormFields.get(FormFields.zendeskCompanyName.name());
        TrelloApi.updateStringCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "Z. customer type";
        fieldValue = (String)ticketFormFields.get(FormFields.zendeskCustomerType.name());
        TrelloApi.updateStringCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "CUSTOMER TYPE";
        fieldValue = mapZendeskCustomerTypeToDropdownOptionInTrello((String)ticketFormFields.get(FormFields.zendeskCustomerType.name()));
        TrelloApi.updateDropdownCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "TIER";
        fieldValue = (String)ticketFormFields.get(FormFields.zendeskTier.name());
        TrelloApi.updateDropdownCustomFieldValue(ticketFormFields, fieldName, ticketId, fieldValue);
        fieldName = "BLOCKER?";
        TrelloApi.updateCheckboxCustomFieldValue(ticketFormFields, fieldName, ticketId, (boolean)ticketFormFields.get(FormFields.blocker.name()));
        fieldName = "WORKAROUND?";
        TrelloApi.updateCheckboxCustomFieldValue(ticketFormFields, fieldName, ticketId, (boolean)ticketFormFields.get(FormFields.workaround.name()));
    }

    private static String mapZendeskCustomerTypeToDropdownOptionInTrello(String zendeskCustomerType) {
        try {
            if (StringUtils.isEmpty(zendeskCustomerType)) {
                return "Lead";
            } else if (zendeskCustomerType.equalsIgnoreCase("Prospect")) {
                return "POC";
            } else if (zendeskCustomerType.equalsIgnoreCase("Customer")) {
                return "Paying";
            } else {
                return "";
            }
        } catch (Throwable t) {
            return "";
        }
    }

    public enum FormFields {
        accountName, accountID, board, listID, ticketTitle, ticketDescription, customerAppUrl, sdk, sdkVersion, linkToTestResults, logFiles,
        reproducibleFiles, isAppAccessible, renderID, requestID, zendeskCustomerName, zendeskCompanyName, zendeskUrl, zendeskTier, zendeskCustomerType,
        workaround, blocker, extraFiles1, extraFiles2, extraFiles3, extraFiles4, extraFiles5, extraAccounts
    }

}
