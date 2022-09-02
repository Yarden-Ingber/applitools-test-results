package com.yarden.restServiceDemo.kpis;

import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.RestCalls;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrelloUpdateRequestQueue extends TimerTask {

    private static ConcurrentLinkedQueue<TicketUpdateRequest> stateUpdateRequestQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<TicketUpdateRequest> archiveTicketRequestQueue = new ConcurrentLinkedQueue<>();
    private static boolean isRunning = false;
    private static Timer timer;

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("TrelloUpdateRequestQueue");
            timer.scheduleAtFixedRate(new TrelloUpdateRequestQueue(), 30, 200);
            isRunning = true;
            Logger.info("TrelloUpdateRequestQueue started");
        }
    }

    @Override
    public void run() {
        try {dumpStateUpdateRequest();} catch (EmptyQueueException e) {}
        try {dumpArchiveTicketRequest();} catch (EmptyQueueException e) {}
    }

    public static void addStateUpdateRequestToQueue(TicketUpdateRequest ticketUpdateRequest) {
        stateUpdateRequestQueue.add(ticketUpdateRequest);
    }

    public static void addArchiveTicketRequestToQueue(TicketUpdateRequest ticketUpdateRequest) {
        archiveTicketRequestQueue.add(ticketUpdateRequest);
    }

    private void dumpStateUpdateRequest() throws EmptyQueueException {
        TicketUpdateRequest ticketUpdateRequest = getFirstRequestInQueue(stateUpdateRequestQueue);
        synchronized (RestCalls.lock) {
            try {
                new KpisMonitoringService(ticketUpdateRequest).updateStateChange();
            } catch (Throwable t) {
                t.printStackTrace();
                stateUpdateRequestQueue.add(ticketUpdateRequest);
            }
        }
    }

    private void dumpArchiveTicketRequest() throws EmptyQueueException {
        TicketUpdateRequest ticketUpdateRequest = getFirstRequestInQueue(archiveTicketRequestQueue);
        synchronized (RestCalls.lock) {
            try {
                new KpisMonitoringService(ticketUpdateRequest).archiveCard();
            } catch (Throwable t) {
                archiveTicketRequestQueue.add(ticketUpdateRequest);
            }
        }
    }

    private TicketUpdateRequest getFirstRequestInQueue(ConcurrentLinkedQueue<TicketUpdateRequest> requestQueue) throws EmptyQueueException {
        if (requestQueue.size() > 0) {
            TicketUpdateRequest ticketUpdateRequest = requestQueue.poll();
            Logger.info("TrelloUpdateRequestQueue: Dumping request. ticket id: " + ticketUpdateRequest.getTicketId() + " queue size=" + requestQueue.size());
            return ticketUpdateRequest;
        } else {
            throw new EmptyQueueException();
        }
    }

}
