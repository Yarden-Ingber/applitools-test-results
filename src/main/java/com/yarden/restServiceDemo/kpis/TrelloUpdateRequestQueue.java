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
public class TrelloUpdateRequestQueue extends TimerTask {

    private static LinkedList<TicketUpdateRequest> stateUpdateRequestQueue = new LinkedList<>();
    private static LinkedList<TicketUpdateRequest> updateTicketFieldsRequestQueue = new LinkedList<>();
    private static LinkedList<TicketUpdateRequest> archiveTicketRequestQueue = new LinkedList<>();
    private static boolean isRunning = false;
    private static Timer timer;
    private static final String privateLock = "LOCK";

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("TrelloUpdateRequestQueue");
            timer.scheduleAtFixedRate(new TrelloUpdateRequestQueue(), 30, 1000 * 3);
            isRunning = true;
            Logger.info("TrelloUpdateRequestQueue started");
        }
    }

    @Override
    public void run() {
        try {dumpStateUpdateRequest();} catch (EmptyQueueException e) {}
        try {dumpUpdateTicketFieldsRequest();} catch (EmptyQueueException e) {}
        try {dumpArchiveTicketRequest();} catch (EmptyQueueException e) {}
    }

    public synchronized static void addStateUpdateRequestToQueue(TicketUpdateRequest ticketUpdateRequest) {
        synchronized (privateLock) {
            stateUpdateRequestQueue.addLast(ticketUpdateRequest);
        }
    }

    public synchronized static void addUpdateTicketFieldsRequestToQueue(TicketUpdateRequest ticketUpdateRequest) {
        synchronized (privateLock) {
            updateTicketFieldsRequestQueue.addLast(ticketUpdateRequest);
        }
    }

    public synchronized static void addArchiveTicketRequestToQueue(TicketUpdateRequest ticketUpdateRequest) {
        synchronized (privateLock) {
            archiveTicketRequestQueue.addLast(ticketUpdateRequest);
        }
    }

    private void dumpStateUpdateRequest() throws EmptyQueueException {
        TicketUpdateRequest ticketUpdateRequest = getFirstRequestInQueue(stateUpdateRequestQueue);
        synchronized (RestCalls.lock) {
            new KpisMonitoringService(ticketUpdateRequest).updateStateChange();
        }
    }

    private void dumpUpdateTicketFieldsRequest() throws EmptyQueueException {
        TicketUpdateRequest ticketUpdateRequest = getFirstRequestInQueue(updateTicketFieldsRequestQueue);
        synchronized (RestCalls.lock) {
            new KpisMonitoringService(ticketUpdateRequest).updateTicketFields();
        }
    }

    private void dumpArchiveTicketRequest() throws EmptyQueueException {
        TicketUpdateRequest ticketUpdateRequest = getFirstRequestInQueue(archiveTicketRequestQueue);
        synchronized (RestCalls.lock) {
            new KpisMonitoringService(ticketUpdateRequest).archiveCard();
        }
    }

    private TicketUpdateRequest getFirstRequestInQueue(LinkedList<TicketUpdateRequest> requestQueue) throws EmptyQueueException {
        TicketUpdateRequest ticketUpdateRequest = new TicketUpdateRequest();
        boolean isRequestExists = false;
        synchronized (privateLock) {
            if (requestQueue.size() > 0) {
                ticketUpdateRequest = requestQueue.removeFirst();
                isRequestExists = true;
            }
        }
        if (isRequestExists) {
            Logger.info("TrelloUpdateRequestQueue: Dumping request. ticket id: " + ticketUpdateRequest.getTicketId() + " queue size=" + requestQueue.size());
            return ticketUpdateRequest;
        } else {
            throw new EmptyQueueException();
        }
    }

}
