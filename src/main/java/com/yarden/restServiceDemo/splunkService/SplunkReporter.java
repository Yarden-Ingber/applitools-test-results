package com.yarden.restServiceDemo.splunkService;

import com.splunk.*;
import com.yarden.restServiceDemo.Enums;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class SplunkReporter extends TimerTask {

    private static Receiver receiver = null;
    private static Service service = null;
    private static LinkedList<SplunkReportObject> reportQueue = new LinkedList<>();
    private static final String lock = "lock";
    private static boolean isRunning = false;
    private static Timer timer;

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("SplunkReportQueue");
            timer.scheduleAtFixedRate(new SplunkReporter(), 30, 50);
            isRunning = true;
            System.out.println("SplunkReportQueue started");
        }
    }

    public void report(Enums.SplunkSourceTypes sourcetype, String json){
        synchronized (lock) {
            try {
                reportQueue.addLast(new SplunkReportObject(sourcetype, json));
            } catch (NullPointerException e) {
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public String getDataFromSplunk(String query, Date startTimeDate, Date endTimeDate, JobExportArgs.OutputMode outputMode) {
        String endTime = new SimpleDateFormat("MM/dd/yyyy:HH:mm:ss").format(endTimeDate);
        String startTime = new SimpleDateFormat("MM/dd/yyyy:HH:mm:ss").format(startTimeDate);
        query = "search starttime=\"" + startTime + "\" endtime=\"" + endTime + "\" " + query;
        return search(query, outputMode, 1000);
    }

    private String search(String query, JobExportArgs.OutputMode outputMode, int resultsCount) {
        Job job = null;
        try {
            job = getService().getJobs().create(query);
        } catch (Exception e) {
            resetSplunkConnection();
            job = getService().getJobs().create(query);
        }
        while (!job.isDone()) {
            try {
                Thread.sleep(500);
            } catch (Throwable t) {}
        }
        CollectionArgs outputArgs = new CollectionArgs();
        outputArgs.setCount(resultsCount);
        outputArgs.put("output_mode", outputMode.toString());
        InputStream stream = job.getResults(outputArgs);
        try {
            return IOUtils.toString(stream, Charset.defaultCharset());
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public void run() {
        boolean shouldSendMessage = false;
        SplunkReportObject reportObject = new SplunkReportObject(Enums.SplunkSourceTypes.RawServerLog, "");
        synchronized (lock) {
            if (!reportQueue.isEmpty()) {
                reportObject = reportQueue.removeFirst();
                shouldSendMessage = true;
            }
        }
        if (shouldSendMessage) {
            logToSplunk(reportObject);
        }
    }

    private void logToSplunk(SplunkReportObject reportObject) {
        Args args = new Args();
        args.add("sourcetype", reportObject.sourcetype.value);
        try {
            getReceiver().log("qualityevents", args, reportObject.json);
        } catch (Throwable t) {
            System.out.println("SplunkReporter: Retrying splunk log");
            try {
                resetSplunkConnection();
                getReceiver().log("qualityevents", args, reportObject.json);
            } catch (Throwable t2) {
                System.out.println("SplunkReporter: Failed logging to splunk: " + reportObject.json);
            }
        }
    }

    private void resetSplunkConnection(){
        receiver = null;
        service = null;
        System.gc();
    }

    private Service getService(){
        if (service == null) {
            ServiceArgs serviceArgs = new ServiceArgs();
            serviceArgs.setHost("applitools.splunkcloud.com");
            serviceArgs.setUsername(Enums.EnvVariables.SplunkUsername.value);
            serviceArgs.setPassword(Enums.EnvVariables.SplunkPassword.value);
            serviceArgs.setPort(8089);
            serviceArgs.setSSLSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
            service = Service.connect(serviceArgs);
        }
        return service;
    }

    private Receiver getReceiver(){
        if (receiver == null) {
            return getService().getReceiver();
        }
        return receiver;
    }

    public class SplunkReportObject {
        Enums.SplunkSourceTypes sourcetype;
        String json;

        public SplunkReportObject(Enums.SplunkSourceTypes sourcetype, String json) {
            this.sourcetype = sourcetype;
            this.json = json;
        }
    }
}
