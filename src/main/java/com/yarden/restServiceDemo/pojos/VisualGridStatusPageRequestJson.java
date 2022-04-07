package com.yarden.restServiceDemo.pojos;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.yarden.restServiceDemo.Logger;

public class VisualGridStatusPageRequestJson {

    @SerializedName("status")
    @Expose
    private List<VisualGridStatus> status = null;

    public List<VisualGridStatus> getStatus() {
        return status;
    }

    public void setStatus(List<VisualGridStatus> status) {
        this.status = status;
    }

    public void printSystemList() {
        for (VisualGridStatus visualGridStatus : status) {
            Logger.info(visualGridStatus.getSystem());
        }
    }

}