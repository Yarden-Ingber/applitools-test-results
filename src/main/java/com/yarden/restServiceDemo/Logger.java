package com.yarden.restServiceDemo;

import com.yarden.restServiceDemo.splunkService.SplunkReporter;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class Logger {

    public static void info(String msg){
        String timestamp = getTimaStamp();
        System.out.println(timestamp + " == INFO: " + msg);
        JSONObject log = new JSONObject().put("level", "info").put("text", timestamp + " == " + msg);
        new SplunkReporter().report(Enums.SplunkSourceTypes.RawServerLog, log.toString());
    }

    public static void warn(String msg){
        String timestamp = getTimaStamp();
        System.out.println(timestamp + " == WARNING: " + msg);
        JSONObject log = new JSONObject().put("level", "warning").put("text", timestamp + " == " + msg);
        new SplunkReporter().report(Enums.SplunkSourceTypes.RawServerLog, log.toString());
    }

    public static void error(String msg){
        String timestamp = getTimaStamp();
        System.out.println(timestamp + " == ERROR: " + msg);
        JSONObject log = new JSONObject().put("level", "error").put("text", timestamp + " == " + msg);
        new SplunkReporter().report(Enums.SplunkSourceTypes.RawServerLog, log.toString());
    }

    public static String getTimaStamp(){
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("GMT+3"));
        return Timestamp.valueOf(zonedDateTime.toLocalDateTime()).toString();
    }

    public static Date timestampToDate(String timestamp) throws ParseException {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        timestamp = timestamp.substring(0,pattern.length());
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        return dateFormat.parse(timestamp);
    }
}
