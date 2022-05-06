package com.yarden.restServiceDemo.kpis;

import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.RestCalls;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class UpdateTicketStateFromQueue extends TimerTask {

    private static AtomicReference<LinkedList<TicketUpdateRequest>> requestQueue = new AtomicReference<>();
    private static boolean isRunning = false;
    private static Timer timer;

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            requestQueue.set(new LinkedList<>());
            timer = new Timer("UpdateTicketStateFromQueue");
            timer.scheduleAtFixedRate(new UpdateTicketStateFromQueue(), 30, 1000 * 3);
            isRunning = true;
            Logger.info("UpdateTicketStateFromQueue started");
        }
    }

    @Override
    public void run() {
        synchronized (RestCalls.lock) {
            if (requestQueue.get().size() > 0) {
                TicketUpdateRequest ticketUpdateRequest = requestQueue.get().removeFirst();
                Logger.info("UpdateTicketStateFromQueue: Dumping ticket state update request. ticket id: " + ticketUpdateRequest.getTicketId() + " queue size=" + requestQueue.get().size());
                new KpisMonitoringService(ticketUpdateRequest).updateStateChange();
            }
        }
    }

    public synchronized static void addUpdateTicketStateRequest(TicketUpdateRequest ticketUpdateRequest) {
        requestQueue.get().addLast(ticketUpdateRequest);
    }

}
