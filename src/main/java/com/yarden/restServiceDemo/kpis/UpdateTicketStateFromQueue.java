package com.yarden.restServiceDemo.kpis;

import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.RestCalls;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

@Configuration
public class UpdateTicketStateFromQueue extends TimerTask {

    private static LinkedList<TicketUpdateRequest> requestQueue = new LinkedList<>();
    private static boolean isRunning = false;
    private static Timer timer;

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("UpdateTicketStateFromQueue");
            timer.scheduleAtFixedRate(new UpdateTicketStateFromQueue(), 30, 1000 * 3);
            isRunning = true;
            Logger.info("UpdateTicketStateFromQueue started");
        }
    }

    @Override
    public void run() {
        synchronized (RestCalls.lock) {
            if (requestQueue.size() > 0) {
                TicketUpdateRequest ticketUpdateRequest = requestQueue.removeFirst();
                Logger.info("UpdateTicketStateFromQueue: Dumping ticket state update request. ticket id: " + ticketUpdateRequest.getTicketId());
                new KpisMonitoringService(ticketUpdateRequest).updateStateChange();
            }
        }
    }

    public synchronized static void addUpdateTicketStateRequest(TicketUpdateRequest ticketUpdateRequest) {
        requestQueue.addLast(ticketUpdateRequest);
    }

}
