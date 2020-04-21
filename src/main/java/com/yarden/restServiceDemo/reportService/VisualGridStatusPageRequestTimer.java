package com.yarden.restServiceDemo.reportService;

import com.yarden.restServiceDemo.Logger;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class VisualGridStatusPageRequestTimer extends TimerTask {

    public static boolean isRequestReceived;
    private static boolean isRunning = false;
    private static Timer timer;

    public static synchronized void start() {
        if (!isRunning) {
            isRequestReceived = true;
            timer = new Timer("VisualGridStatusPageRequestTimer");
            timer.scheduleAtFixedRate(new VisualGridStatusPageRequestTimer(), 30, 12 * 1000 * 60);
            isRunning = true;
            Logger.info("VisualGridStatusPageRequestTimer started");
        }
    }

    @Override
    public void run() {
        Logger.info("VisualGridStatusPageRequestTimer saying: \"tick...\"");
        if (!isRequestReceived) {
            Logger.info("VisualGridStatusPageRequestTimer timeout. adding empty row to Visual Grid status page.");
            String json = "{\"status\": []}";
            try {
                new VisualGridStatusPageService().postResults(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        isRequestReceived = false;
    }

}