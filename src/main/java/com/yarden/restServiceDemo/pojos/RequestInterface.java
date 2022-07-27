package com.yarden.restServiceDemo.pojos;

import com.google.gson.JsonArray;

public interface RequestInterface {

    Boolean getSandbox();

    String getId();

    JsonArray getResults();

    String getGroup();

    String getSdk();

    void setResults(JsonArray results);

    void setTimestamp(String timestamp);


}
