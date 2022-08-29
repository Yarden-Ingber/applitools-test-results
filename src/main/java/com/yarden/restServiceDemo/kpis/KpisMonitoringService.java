package com.yarden.restServiceDemo.kpis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import org.apache.commons.lang3.StringUtils;

public class KpisMonitoringService {

    SheetData rawDataSheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.KPIS.value, Enums.KPIsSheetTabsNames.RawData.value));
    TicketUpdateRequest ticketUpdateRequest;
    TicketStates newState;

    public KpisMonitoringService(TicketUpdateRequest ticketUpdateRequest) {
        this.ticketUpdateRequest = ticketUpdateRequest;
        this.newState = new TicketsNewStateResolver(ticketUpdateRequest).resolve();
    }

    public void updateStateChange() {
        TicketSearchResult ticketSearchResult = findSheetEntry();
        if (ticketSearchResult.isFound) {
            ignoreTeamChangeForEyesOperationsBoard(ticketSearchResult.ticket);
            updateTicketFields(ticketSearchResult.ticket);
            if (!newState.equals(TicketStates.NoState)) {
                new TicketsStateChanger().updateExistingTicketState(ticketSearchResult.ticket, newState);
            }
        } else {
            if (!newState.equals(TicketStates.NoState) && TicketsNewStateResolver.isTicketInATeamBoard(ticketUpdateRequest.getTeam())) {
                JsonElement ticket = addNewTicketEntry();
                ticketSearchResult = new TicketSearchResult(ticket);
            } else {
                Logger.info("KPIs: Ticket" + ticketUpdateRequest.getTicketId() + " sent an update but doesn't correspond to a valid state");
            }
        }
        updateStateIfParentExists(ticketSearchResult);
        if (!newState.equals(TicketStates.NoState)) {
            updateAllChildTicketsStates();
        }
        new KpiSplunkReporter(rawDataSheetData, ticketUpdateRequest).reportStandAloneEvent(newState);
    }

    public void archiveCard() {
        ticketUpdateRequest.setTeam(Enums.Strings.Archived.value);
        TicketSearchResult ticketSearchResult = findSheetEntry();
        if (ticketSearchResult.isFound) {
            updateTicketFields(ticketSearchResult.ticket);
        } else {
            Logger.info("KPIs: Ticket " + ticketUpdateRequest.getTicketId() + " wasn't found in the sheet");
        }
        new KpiSplunkReporter(rawDataSheetData, ticketUpdateRequest).reportStandAloneEvent(newState);
    }

    private void updateAllChildTicketsStates() {
        for (JsonElement sheetEntry: rawDataSheetData.getSheetData()){
            if (sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.ParentTicket.value).getAsString().equals(ticketUpdateRequest.getTicketId())){
                Logger.info("KPIs: Updating state of child ticket: " + sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.TicketID.value).getAsString() + " as parent ticket: " + ticketUpdateRequest.getTicketId());
                new TicketsStateChanger().updateExistingTicketState(sheetEntry, newState);
            }
        }
    }

    private void updateStateIfParentExists(TicketSearchResult ticketSearchResult) {
        String parentId = "";
        JsonElement childTicket = ticketSearchResult.ticket;
        if (childTicket.getAsJsonObject().get(Enums.KPIsSheetColumnNames.ParentTicket.value) == null ||
                childTicket.getAsJsonObject().get(Enums.KPIsSheetColumnNames.ParentTicket.value).isJsonNull() ||
                StringUtils.isEmpty(childTicket.getAsJsonObject().get(Enums.KPIsSheetColumnNames.ParentTicket.value).getAsString())) {
            return;
        } else {
            parentId = childTicket.getAsJsonObject().get(Enums.KPIsSheetColumnNames.ParentTicket.value).getAsString();
        }
        Logger.info("KPIs: Updating state of child ticket: " + ticketUpdateRequest.getTicketId() + " as parent ticket: " + parentId);
        for (JsonElement sheetEntry: rawDataSheetData.getSheetData()){
            if (sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.TicketID.value).getAsString().equals(parentId)){
                for (TicketStates state : TicketStates.values()) {
                    if (state.name().equals(sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CurrentState.value).getAsString())) {
                        new TicketsStateChanger().updateExistingTicketState(childTicket, state);
                        return;
                    }
                }
            }
        }
    }

    private void ignoreTeamChangeForEyesOperationsBoard(JsonElement ticket) {
        if (ticketUpdateRequest.getTeam().equals(TicketsNewStateResolver.Boards.EyesOperations.value)) {
            ticketUpdateRequest.setTeam(ticket.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString());
        }
    }

    private void archiveTicketIfMovedToEyesAppIssuesBoard() {
        if (ticketUpdateRequest.getTeam().equals(TicketsNewStateResolver.Boards.EyesAppIssues.value)) {
            ticketUpdateRequest.setTeam(Enums.Strings.Archived.value);
            newState = TicketStates.NoState;
            archiveCard();
        }
    }

    private void updateTicketFields(JsonElement ticket) {
        addTypeToTicket(ticket);
        addIsCrossBoards(ticket);
        addTicketSubProject(ticket);
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.Workaround.value, ticketUpdateRequest.getWorkaround());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.Team.value, ticketUpdateRequest.getTeam());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketID.value, ticketUpdateRequest.getTicketId());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.ParentTicket.value, ticketUpdateRequest.getParentTicket());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketTitle.value, ticketUpdateRequest.getTicketTitle());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketUrl.value, ticketUpdateRequest.getTicketUrl());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.CreatedBy.value, ticketUpdateRequest.getCreatedBy());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.CurrentTrelloList.value, ticketUpdateRequest.getCurrent_trello_list());
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.Labels.value, ticketUpdateRequest.getLabels());
    }

    private void addTicketSubProject(JsonElement ticket) {
        if (StringUtils.isEmpty(ticketUpdateRequest.getSubProject())) {
            ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.SubProject.value, ticketUpdateRequest.getSdk());
        } else {
            ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.SubProject.value, ticketUpdateRequest.getSubProject());
        }
    }

    private void addIsCrossBoards(JsonElement ticket) {
        String team = ticket.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString();
        if (!(team.equals(ticketUpdateRequest.getTeam()) || ticketUpdateRequest.getTeam().equals(Enums.Strings.Archived.value))) {
            ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.IsCrossBoards.value, Enums.Strings.True.value);
        }
    }

    private void addTypeToTicket(JsonElement ticket) {
        Logger.info("KPIs: Updating ticket type for ticket " + ticketUpdateRequest.getTicketId() + ": " + ticketUpdateRequest.getTicketType());
        String type = ticketUpdateRequest.getTicketType() == null ? "" : ticketUpdateRequest.getTicketType();
        ticket.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketType.value, type);
    }

    private TicketSearchResult findSheetEntry() {
        for (JsonElement sheetEntry: rawDataSheetData.getSheetData()){
            if (sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.TicketID.value).getAsString().equals(ticketUpdateRequest.getTicketId())){
                return new TicketSearchResult(sheetEntry);
            }
        }
        return new TicketSearchResult(false);
    }

    private JsonElement addNewTicketEntry(){
        JsonElement newEntry = new JsonObject();
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.Team.value, ticketUpdateRequest.getTeam());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.SubProject.value, ticketUpdateRequest.getSubProject());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketID.value, ticketUpdateRequest.getTicketId());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.ParentTicket.value, ticketUpdateRequest.getParentTicket());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketTitle.value, ticketUpdateRequest.getTicketTitle());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketUrl.value, ticketUpdateRequest.getTicketUrl());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.CreationDate.value, Logger.getTimaStamp());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.TicketType.value, ticketUpdateRequest.getTicketType());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.CreatedBy.value, ticketUpdateRequest.getCreatedBy());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.Workaround.value, ticketUpdateRequest.getWorkaround());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.CurrentTrelloList.value, ticketUpdateRequest.getCurrent_trello_list());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.Labels.value, ticketUpdateRequest.getLabels());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.EnterForTimeCalculationState.value + newState.name(), Logger.getTimaStamp());
        newEntry.getAsJsonObject().addProperty(Enums.KPIsSheetColumnNames.CurrentState.value, newState.name());
        Logger.info("KPIs: Adding a new ticket to the sheet: " + newEntry.toString());
        rawDataSheetData.getSheetData().add(newEntry);
        return newEntry;
    }

    private static class TicketSearchResult {

        final JsonElement ticket;
        final boolean isFound;

        public TicketSearchResult(JsonElement ticket) {
            this.ticket = ticket;
            this.isFound = true;
        }

        public TicketSearchResult(boolean isFound) {
            this.ticket = null;
            this.isFound = isFound;
        }

    }

}
