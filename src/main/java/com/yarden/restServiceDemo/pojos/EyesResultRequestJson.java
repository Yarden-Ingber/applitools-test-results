package com.yarden.restServiceDemo.pojos;

import com.google.gson.JsonArray;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class EyesResultRequestJson implements RequestInterface{

    @SerializedName("group")
    @Expose
    private String group;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("sandbox")
    @Expose
    private Boolean sandbox;
    @SerializedName("results")
    @Expose
    private JsonArray results;
    @SerializedName("timestamp")
    @Expose
    private String timestamp;

    public String getId() {
        if (StringUtils.isEmpty(this.id)) {
            setId(UUID.randomUUID().toString().substring(0, 6));
        }
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getSandbox() {
        return sandbox;
    }

    public void setSandbox(Boolean sandbox) {
        this.sandbox = sandbox;
    }

    public JsonArray getResults() {
        return results;
    }

    public void setResults(JsonArray results) {
        this.results = results;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSdk() { return ""; }

    @Override
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}