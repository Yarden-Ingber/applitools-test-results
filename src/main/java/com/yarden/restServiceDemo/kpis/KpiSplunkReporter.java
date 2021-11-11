package com.yarden.restServiceDemo.kpis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.splunkService.SplunkReporter;
import org.json.JSONObject;

public class KpiSplunkReporter {

    private SheetData rawDataSheetData;
    private TicketUpdateRequest ticketUpdateRequest;

    public KpiSplunkReporter(SheetData rawDataSheetData, TicketUpdateRequest ticketUpdateRequest) {
        this.rawDataSheetData = rawDataSheetData;
        this.ticketUpdateRequest = ticketUpdateRequest;
    }

    public void reportLatestState(JsonElement ticket) {
        JSONObject splunkEventJson = new JSONObject();
        splunkEventJson.put("Started_at_state_new", 1);
        addColumnsToEvent(splunkEventJson, ticket);
        Logger.info("KPIs: reporting the latest state of ticket to Splunk: " + splunkEventJson.toString());
        new SplunkReporter().report(Enums.SplunkSourceTypes.RawKPILog, splunkEventJson.toString());
    }

    public void reportStandAloneEvent(TicketStates newState) {
        JSONObject splunkEventJson = new JSONObject();
        splunkEventJson.put(Enums.KPIsSheetColumnNames.Team.value.replace(" ", "_"), ticketUpdateRequest.getTeam());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.SubProject.value.replace(" ", "_"), ticketUpdateRequest.getSubProject());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.TicketID.value.replace(" ", "_"), ticketUpdateRequest.getTicketId());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.TicketType.value.replace(" ", "_"), ticketUpdateRequest.getTicketType());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.CreatedBy.value.replace(" ", "_"), ticketUpdateRequest.getCreatedBy());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.Workaround.value.replace(" ", "_"), ticketUpdateRequest.getWorkaround());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.TicketTitle.value.replace(" ", "_"), ticketUpdateRequest.getTicketTitle());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.TicketUrl.value.replace(" ", "_"), ticketUpdateRequest.getTicketUrl());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.Timestamp.value.replace(" ", "_"), Logger.getTimaStamp());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.CurrentTrelloList.value.replace(" ", "_"), ticketUpdateRequest.getCurrent_trello_list());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.Labels.value.replace(" ", "_"), ticketUpdateRequest.getLabels());
        splunkEventJson.put(Enums.KPIsSheetColumnNames.CurrentState.value.replace(" ", "_"), newState.name());
        new SplunkReporter().report(Enums.SplunkSourceTypes.RawKPILog, splunkEventJson.toString());
    }

    private void addColumnsToEvent(JSONObject splunkEventJson, JsonElement ticket) {
        Logger.info("Adding columns to splunk event for ticket: " + ticket.toString());
        for (String column : rawDataSheetData.getColumnNames()) {
            JsonObject ticketAsJsonObject = ticket.getAsJsonObject();
            JsonElement singleTicketFieldData = ticketAsJsonObject.get(column);
            if (column.equals(Enums.KPIsSheetColumnNames.IsCrossBoards.value)) {
                addIsCrossBoardsValue(splunkEventJson, singleTicketFieldData, column);
            } else if (column.equals(Enums.KPIsSheetColumnNames.Team.value)) {
                splunkEventJson.put("Board", ticketUpdateRequest.getTeam());
            } else if (column.equals(Enums.KPIsSheetColumnNames.Workaround.value)) {
                addWorkaroundValue(splunkEventJson, singleTicketFieldData, column);
            } else if (column.contains(Enums.KPIsSheetColumnNames.CalculatedTimeInState.value)) {
                addCalculatedTimeInState(splunkEventJson, singleTicketFieldData, column);
            } else if (!column.contains(Enums.KPIsSheetColumnNames.EnterForTimeCalculationState.value)){
                addGeneralColumnValue(splunkEventJson, singleTicketFieldData, column);
            }
        }
    }

    private void addCalculatedTimeInState(JSONObject splunkEventJson, JsonElement singleTicketFieldData, String column) {
        if (singleTicketFieldData == null || singleTicketFieldData.isJsonNull()) {
            splunkEventJson.put(column.replace(" ", "_"), 0);
        } else {
            String stringValue = singleTicketFieldData.getAsString();
            if (stringValue.isEmpty()) {
                splunkEventJson.put(column.replace(" ", "_"), 0);
            } else {
                try {
                    int intValue = Integer.parseInt(stringValue);
                    splunkEventJson.put(column.replace(" ", "_"), intValue);
                } catch (NumberFormatException e) {
                    splunkEventJson.put(column.replace(" ", "_"), stringValue);
                }
            }
        }
    }

    private void addIsCrossBoardsValue(JSONObject splunkEventJson, JsonElement singleTicketFieldData, String column){
        if (singleTicketFieldData == null || singleTicketFieldData.isJsonNull()) {
            splunkEventJson.put(column.replace(" ", "_"), 0);
        } else {
            int value = 0;
            value = singleTicketFieldData.getAsString().equals(Enums.Strings.True.value) ? 1 : 0;
            splunkEventJson.put(column.replace(" ", "_"), value);
        }
    }

    private void addWorkaroundValue(JSONObject splunkEventJson, JsonElement singleTicketFieldData, String column){
        if (singleTicketFieldData == null || singleTicketFieldData.isJsonNull()) {
            splunkEventJson.put(column.replace(" ", "_"), 0);
        } else {
            int value = singleTicketFieldData.getAsString().equals(Enums.Strings.Checked.value) ? 1 : 0;
            splunkEventJson.put(column.replace(" ", "_"), value);
        }
    }

    private void addGeneralColumnValue(JSONObject splunkEventJson, JsonElement singleTicketFieldData, String column){
        if (singleTicketFieldData == null || singleTicketFieldData.isJsonNull()) {
            splunkEventJson.put(column.replace(" ", "_"), "");
        } else {
            String stringValue = singleTicketFieldData.getAsString();
            try {
                int intValue = Integer.parseInt(stringValue);
                splunkEventJson.put(column.replace(" ", "_"), intValue);
            } catch (NumberFormatException e) {
                splunkEventJson.put(column.replace(" ", "_"), stringValue);
            }
        }
    }

}
