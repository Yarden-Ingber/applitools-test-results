package com.yarden.restServiceDemo.kpis;

import com.google.gson.Gson;
import com.yarden.restServiceDemo.Logger;

public class TicketsNewStateResolver {

    private TicketUpdateRequest request;
    private final String currentTrelloList;

    public TicketsNewStateResolver(TicketUpdateRequest request) {
        this.request = request;
        this.currentTrelloList = request.getCurrent_trello_list();
    }

    public enum Boards {
        UltrafastGrid("Ultrafast Grid"), JSSDKs("JS SDKs"), AlgoBugs("Algo Bugs"), SDKs("SDKs"), EyesBackend("Eyes Backend"),
        EyesFrontend("Eyes frontend"), EyesAppIssues("Eyes App - Issues"), EyesOperations("Eyes operations"), IosNmg("iOS NMG"),
        AndroidNmg("Android NMG");

        public final String value;

        Boards(String value) {
            this.value = value;
        }
    }

    public TicketStates resolve() {
        if (request.getTeam().equals(Boards.UltrafastGrid.value)) {
            return resolveStateForUFG();
        } else if (request.getTeam().equals(Boards.JSSDKs.value)) {
            return resolveStateForJSSdks();
        } else if (request.getTeam().equals(Boards.AlgoBugs.value)) {
            return resolveStateForAlgoBugs();
        } else if(request.getTeam().equals(Boards.SDKs.value)) {
            return resolveStateForGeneralSdks();
        } else if(request.getTeam().equals(Boards.EyesBackend.value) || request.getTeam().equals(Boards.EyesFrontend.value)) {
            return resolveStateForEyesIssues();
        } else if (request.getTeam().equals(Boards.EyesOperations.value)) {
            return resolveStateForEyesOperations();
        } else if (request.getTeam().equals(Boards.IosNmg.value) || request.getTeam().equals(Boards.AndroidNmg.value)) {
            return resolveStateForNmg();
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForUFG(){
        if (currentTrelloList.equalsIgnoreCase("New")) {
            return TicketStates.New;
        } else if (currentTrelloList.toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (currentTrelloList.equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (currentTrelloList.equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for R&D")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Missing quality")) {
            return TicketStates.MissingQuality;
        } else if (currentTrelloList.equalsIgnoreCase("Trying to reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for field approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (currentTrelloList.toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (currentTrelloList.equalsIgnoreCase("Known Limitations / Not yet supported")) {
            return TicketStates.RFE;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for Product")) {
            return TicketStates.WaitingForProduct;
        } else if (currentTrelloList.equalsIgnoreCase("Couldn't Reproduce")) {
            return TicketStates.WaitingForFieldInput;
        }else  {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForAlgoBugs() {
        if (currentTrelloList.equalsIgnoreCase("New/Pending")) {
            return TicketStates.New;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for R&D")){
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Doing")){
            return TicketStates.Doing;
        } else if (currentTrelloList.equalsIgnoreCase("on hold")){
            return TicketStates.OnHold;
        } else if (currentTrelloList.equalsIgnoreCase("No Algo Change Required")){
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("RFE")){
            return TicketStates.RFE;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for product")){
            return TicketStates.WaitingForProduct;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for field input")){
            return TicketStates.WaitingForFieldInput;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for customer response")){
            return TicketStates.WaitingForCustomerResponse;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForJSSdks() {
        if (currentTrelloList.equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (currentTrelloList.toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (currentTrelloList.equalsIgnoreCase("New")) {
            return TicketStates.New;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (currentTrelloList.equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for R&D")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Missing quality")) {
            return TicketStates.MissingQuality;
        } else if (currentTrelloList.equalsIgnoreCase("Trying to reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for field approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (currentTrelloList.toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for Product")) {
            return TicketStates.WaitingForProduct;
        } else if (currentTrelloList.equalsIgnoreCase("Next")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Bugs")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Fixed - need to release other SDK's")) {
            return TicketStates.Done;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForGeneralSdks() {
        if (currentTrelloList.equalsIgnoreCase("New | Pending")) {
            return TicketStates.New;
        } else if (currentTrelloList.toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (currentTrelloList.equalsIgnoreCase("Trying to Reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (currentTrelloList.equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting For Release")) {
            return TicketStates.Doing;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting For Field's Input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (currentTrelloList.equalsIgnoreCase("Missing information")) {
            return TicketStates.WaitingForFieldInput;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for Field Approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (currentTrelloList.equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("Refactor Tests to Generic")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("All SDKs: Implementation updates")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("All SDKs")) {
            return TicketStates.Doing;
        } else if (currentTrelloList.equalsIgnoreCase("On Hold")) {
            return TicketStates.OnHold;
        } else if (currentTrelloList.equalsIgnoreCase("Known Limitations / Waiting for 3rd Party")) {
            return TicketStates.Done;
        } else if (currentTrelloList.toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for Product")) {
            return TicketStates.WaitingForProduct;
        } else if (currentTrelloList.toLowerCase().contains("waiting for r&d investigation")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.toLowerCase().contains("bugs")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Next")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("For Daniel's Review")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Integrations")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("UFT")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Missing quality")) {
            return TicketStates.MissingQuality;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForEyesIssues() {
        if (currentTrelloList.equalsIgnoreCase("New")) {
            return TicketStates.New;
        } else if (currentTrelloList.toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for R&D")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Not an issue")) {
            return TicketStates.Done;
        }else if (currentTrelloList.equalsIgnoreCase("Trying to reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (currentTrelloList.equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (currentTrelloList.equalsIgnoreCase("On Hold / low priority")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for product")) {
            return TicketStates.WaitingForProduct;
        } else if (currentTrelloList.equalsIgnoreCase("For Amit to Review")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("To be Deployed next Hotfix")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (currentTrelloList.equalsIgnoreCase("R&D done - needs testing")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("done")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for field approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForNmg() {
        if (currentTrelloList.equalsIgnoreCase("New Field Issues")) {
            return TicketStates.New;
        } else if (currentTrelloList.equalsIgnoreCase("Backlog")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Trying to Reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (currentTrelloList.equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for Field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for Customer Response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (currentTrelloList.equalsIgnoreCase("In Code Review")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting For Release")) {
            return TicketStates.WaitingForRD;
        } else if (currentTrelloList.equalsIgnoreCase("Waiting for Field Approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (currentTrelloList.equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (currentTrelloList.equalsIgnoreCase("Known Limitations")) {
            return TicketStates.RFE;
        } else if (currentTrelloList.equalsIgnoreCase("Low Priority")) {
            return TicketStates.OnHold;
        } else if (currentTrelloList.equalsIgnoreCase("On Hold")) {
            return TicketStates.OnHold;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForEyesOperations() {
        if (currentTrelloList.toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (currentTrelloList.toLowerCase().contains("done")){
            return TicketStates.Done;
        } else {
            return TicketStates.WaitingForRD;
        }
    }

    private TicketStates noStateFound(){
        Gson gson = new Gson();
        Logger.warn("KPIs: no state found for request " + gson.toJson(request));
        return TicketStates.NoState;
    }
}
