package com.yarden.restServiceDemo.kpis;

import com.google.gson.Gson;
import com.yarden.restServiceDemo.Logger;

public class TicketsNewStateResolver {

    private TicketUpdateRequest request;

    public TicketsNewStateResolver(TicketUpdateRequest request) {
        this.request = request;
    }

    public enum Boards {
        UltrafastGrid("Ultrafast Grid"), JSSDKs("JS SDKs"), AlgoBugs("Algo Bugs"), SDKs("SDKs"), EyesBackend("Eyes Backend"),
        EyesFrontend("Eyes frontend"), EyesAppIssues("Eyes App - Issues"), EyesOperations("Eyes operations");

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
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForUFG(){
        if (request.getCurrent_trello_list().equalsIgnoreCase("New")) {
            return TicketStates.New;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for R&D")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Missing quality")) {
            return TicketStates.MissingQuality;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Trying to reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for field approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for Product")) {
            return TicketStates.WaitingForProduct;
        } else  {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForAlgoBugs() {
        if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("New/Pending")) {
            return TicketStates.New;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Layout")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("RCA")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Contrast")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for Adam")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Automated maintenance")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Strict - Broken")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Strict - Fixed (not deployed)")) {
            return TicketStates.WaitingForRD;
        }else if (request.getCurrent_trello_list().equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Solved/Waiting for publish")) {
            return TicketStates.Doing;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Deployed to cloud")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Closed")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("No Algo Change Required")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Known limitations (RFE)")) {
            return TicketStates.RFE;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for other ticket")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Missing information")) {
            return TicketStates.WaitingForFieldInput;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for product")) {
            return TicketStates.WaitingForProduct;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForJSSdks() {
        if (request.getCurrent_trello_list().equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("New")) {
            return TicketStates.New;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for R&D")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Missing quality")) {
            return TicketStates.MissingQuality;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Trying to reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for field approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for Product")) {
            return TicketStates.WaitingForProduct;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Next")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Bugs")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Fixed - need to release other SDK's")) {
            return TicketStates.Done;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForGeneralSdks() {
        if (request.getCurrent_trello_list().equalsIgnoreCase("New | Pending")) {
            return TicketStates.New;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Trying to Reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting For Release")) {
            return TicketStates.Doing;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting For Field's Input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Missing information")) {
            return TicketStates.WaitingForFieldInput;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for Field Approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Done")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Refactor Tests to Generic")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("All SDKs: Implementation updates")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("On Hold")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Known Limitations / Waiting for 3rd Party")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for Product")) {
            return TicketStates.WaitingForProduct;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("waiting for r&d investigation")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("bugs")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Next")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("For Daniel's Review")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Integrations")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("UFT")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Missing Quality Info")) {
            return TicketStates.MissingQuality;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForEyesIssues() {
        if (request.getCurrent_trello_list().equalsIgnoreCase("New")) {
            return TicketStates.New;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for R&D")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Not an issue")) {
            return TicketStates.Done;
        }else if (request.getCurrent_trello_list().equalsIgnoreCase("Trying to reproduce")) {
            return TicketStates.TryingToReproduce;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Doing")) {
            return TicketStates.Doing;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("On Hold / low priority")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for field input")) {
            return TicketStates.WaitingForFieldInput;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for customer response")) {
            return TicketStates.WaitingForCustomerResponse;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for product")) {
            return TicketStates.WaitingForProduct;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("For Amit to Review")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("To be Deployed next Hotfix")) {
            return TicketStates.WaitingForRD;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("rfe")) {
            return TicketStates.RFE;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("done")) {
            return TicketStates.Done;
        } else if (request.getCurrent_trello_list().equalsIgnoreCase("Waiting for field approval")) {
            return TicketStates.WaitingForFieldApproval;
        } else {
            return noStateFound();
        }
    }

    private TicketStates resolveStateForEyesOperations() {
        if (request.getCurrent_trello_list().toLowerCase().contains("on hold")){
            return TicketStates.OnHold;
        } else if (request.getCurrent_trello_list().toLowerCase().contains("done")){
            return TicketStates.Done;
        } else {
            return noStateFound();
        }
    }

    private TicketStates noStateFound(){
        Gson gson = new Gson();
        Logger.warn("KPIs: no state found for request " + gson.toJson(request));
        return TicketStates.NoState;
    }
}
