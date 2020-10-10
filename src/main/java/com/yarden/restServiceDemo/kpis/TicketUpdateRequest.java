package com.yarden.restServiceDemo.kpis;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TicketUpdateRequest {

    @SerializedName("team")
    @Expose
    private String team;
    @SerializedName("sub_project")
    @Expose
    private String subProject;
    @SerializedName("ticket_id")
    @Expose
    private String ticketId;
    @SerializedName("ticket_title")
    @Expose
    private String ticketTitle;
    @SerializedName("ticket_type")
    @Expose
    private String ticketType;
    @SerializedName("created_by")
    @Expose
    private String createdBy;
    @SerializedName("ticket_url")
    @Expose
    private String ticketUrl;
    @SerializedName("state")
    @Expose
    private String state;

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getSubProject() {
        return subProject;
    }

    public void setSubProject(String subProject) {
        this.subProject = subProject;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketTitle() {
        return ticketTitle;
    }

    public void setTicketTitle(String ticketTitle) {
        this.ticketTitle = ticketTitle;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTicketUrl() {
        return ticketUrl;
    }

    public void setTicketUrl(String ticketUrl) {
        this.ticketUrl = ticketUrl;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}