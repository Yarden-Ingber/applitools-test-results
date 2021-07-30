package com.yarden.restServiceDemo.kpis;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ui.ModelMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TrelloApi {

    private static final String trelloApiKey = Enums.EnvVariables.TrelloApiKey.value;
    private static final String trelloApiToken = Enums.EnvVariables.TrelloApiToken.value;

    public static JSONArray getTrelloAccountsArray() throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get("https://api.trello.com/1/organizations/applitools/members")
                .queryString("key", trelloApiKey)
                .queryString("token", trelloApiToken)
                .asJson();
        return response.getBody().getArray();
    }

    public static void uploadFilesToTicket(String ticketId, MultipartFile[] fileArray) throws UnirestException {
        for (MultipartFile multipartFile : fileArray) {
            if (StringUtils.isNotEmpty(multipartFile.getOriginalFilename())) {
                File file = convertMultipartFileToFile(multipartFile, multipartFile.getOriginalFilename());
                HttpResponse<String> response = Unirest.post("https://api.trello.com/1/cards/" + ticketId + "/attachments")
                        .queryString("key", trelloApiKey)
                        .queryString("token", trelloApiToken)
                        .field("file", file).asString();
                file.delete();
            }
        }
    }

    public static JSONObject createTicket(ModelMap ticketFormFields) throws UnirestException {
        HttpResponse<JsonNode> createTicketResponse = Unirest.post("https://api.trello.com/1/cards")
                .queryString("key", trelloApiKey)
                .queryString("token", trelloApiToken)
                .queryString("idList", ticketFormFields.get(TrelloTicketCreator.FormFields.listID.name()))
                .queryString("name", ticketFormFields.get(TrelloTicketCreator.FormFields.ticketTitle.name()))
                .queryString("desc", ticketFormFields.get(TrelloTicketCreator.FormFields.ticketDescription.name()))
                .asJson();
        return createTicketResponse.getBody().getObject();
    }

    public static void addMemberToTicket(String ticketId, ModelMap ticketFormFields) throws UnirestException {
        final String requestUrl = "https://api.trello.com/1/cards/" + ticketId + "/idMembers?key=" + trelloApiKey + "&token=" + trelloApiToken + "&value=";
        Unirest.post(requestUrl +  ticketFormFields.get(TrelloTicketCreator.FormFields.accountID.name()))
                .asString();
        String[] extraAccounts = ((String)ticketFormFields.get(TrelloTicketCreator.FormFields.extraAccounts.name())).split(",");
        for (String account : extraAccounts) {
            if (StringUtils.isNotEmpty(account)) {
                String accountID = account.split(TrelloTicketCreator.AccountsSeparator)[1];
                Unirest.post(requestUrl +  accountID)
                        .asString();
            }
        }
    }

    public static void updateStringCustomFieldValue(ModelMap ticketFormFields, String customFieldName, String ticketID, String fieldValue) {
        try {
            String requestBody = "{\"value\":{\"text\":\"" + fieldValue + "\"}}";
            updateGenericCustomFieldValue(ticketFormFields, customFieldName, ticketID, requestBody);
        } catch (Throwable t) {
            Logger.warn("Failed to set custom field value " + customFieldName + "=" + fieldValue);
        }
    }

    public static void updateCheckboxCustomFieldValue(ModelMap ticketFormFields, String customFieldName, String ticketID, boolean fieldValue) {
        try {
            String requestBody = "{\"value\":{\"checked\":\"" + fieldValue + "\"}}";
            updateGenericCustomFieldValue(ticketFormFields, customFieldName, ticketID, requestBody);
        } catch (Throwable t) {
            Logger.warn("Failed to set custom field value " + customFieldName + "=" + fieldValue);
        }
    }

    public static void updateDropdownCustomFieldValue(ModelMap ticketFormFields, String customFieldName, String ticketID, String fieldValue) {
        try {
            String optionID = getCustomFieldDropdownOptionId(ticketFormFields, customFieldName, fieldValue);
            String requestBody = "{\"idValue\":\"" + optionID + "\"}";
            updateGenericCustomFieldValue(ticketFormFields, customFieldName, ticketID, requestBody);
        } catch (Throwable t) {
            Logger.warn("Failed to set custom field value " + customFieldName);
        }
    }

    private static void updateGenericCustomFieldValue(ModelMap ticketFormFields, String customFieldName, String ticketID, String requestBody) throws UnirestException {
        String customFieldID = getCustomFieldId(ticketFormFields, customFieldName);
        HttpResponse<JsonNode> response = Unirest.put("https://api.trello.com/1/cards/" + ticketID + "/customField/" + customFieldID + "/item?key=" + trelloApiKey + "&token=" + trelloApiToken)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .asJson();
    }

    private static String getCustomFieldId(ModelMap ticketFormFields, String desiredField) throws UnirestException {
        JSONArray customFieldsArray = getCustomFieldsArray(ticketFormFields);
        for (Object customField : customFieldsArray) {
            String fieldName = (String)((JSONObject)customField).get("name");
            if (fieldName.equalsIgnoreCase(desiredField)) {
                return (String)((JSONObject)customField).get("id");
            }
        }
        return "not found";
    }

    private static String getCustomFieldDropdownOptionId(ModelMap ticketFormFields, String desiredField, String optionText) throws UnirestException {
        JSONArray customFieldsArray = getCustomFieldsArray(ticketFormFields);
        for (Object customField : customFieldsArray) {
            String fieldName = (String)((JSONObject)customField).get("name");
            if (fieldName.equalsIgnoreCase(desiredField)) {
                JSONArray options = (JSONArray)((JSONObject)customField).get("options");
                for (Object option : options) {
                    if (((String)((JSONObject)((JSONObject)option).get("value")).get("text")).equalsIgnoreCase(optionText)) {
                        return (String)((JSONObject)option).get("id");
                    }
                }
            }
        }
        return "not found";
    }

    private static JSONArray getCustomFieldsArray(ModelMap ticketFormFields) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get("https://api.trello.com/1/boards/" + ticketFormFields.get(TrelloTicketCreator.FormFields.board.name()) + "/customFields?key=" + trelloApiKey + "&token=" + trelloApiToken).asJson();
        return response.getBody().getArray();
    }

    private static File convertMultipartFileToFile(MultipartFile multipartFile, String fileName) {
        Logger.info("TrelloApi: Uploading file: " + fileName);
        String filePath = System.getProperty("java.io.tmpdir")+File.separator+fileName;
        File file = new File(filePath);
        Logger.info("TrelloApi: local file path: " + filePath);
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(multipartFile.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
    
    private static File convertMultipartFileToFile2(MultipartFile multipartFile, String fileName) {
        Logger.info("TrelloApi: Uploading file: " + fileName);
        String filePath = System.getProperty("java.io.tmpdir")+File.separator+fileName;
        File file = new File(filePath);
        Logger.info("TrelloApi: local file path: " + filePath);
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(multipartFile.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
