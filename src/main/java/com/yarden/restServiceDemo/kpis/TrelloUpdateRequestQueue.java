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
public class TrelloUpdateRequestQueue extends TimerTask {

    private static AtomicReference<LinkedList<TicketUpdateRequest>> stateUpdateRequestQueue = new AtomicReference<>();
    private static AtomicReference<LinkedList<TicketUpdateRequest>> updateTicketFieldsRequestQueue = new AtomicReference<>();
    private static AtomicReference<LinkedList<TicketUpdateRequest>> archiveTicketRequestQueue = new AtomicReference<>();
    private static boolean isRunning = false;
    private static Timer timer;

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            stateUpdateRequestQueue.set(new LinkedList<>());
            updateTicketFieldsRequestQueue.set(new LinkedList<>());
            archiveTicketRequestQueue.set(new LinkedList<>());
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
        stateUpdateRequestQueue.get().addLast(ticketUpdateRequest);
    }

    public synchronized static void addUpdateTicketFieldsRequestToQueue(TicketUpdateRequest ticketUpdateRequest) {
        updateTicketFieldsRequestQueue.get().addLast(ticketUpdateRequest);
    }

    public synchronized static void addArchiveTicketRequestToQueue(TicketUpdateRequest ticketUpdateRequest) {
        archiveTicketRequestQueue.get().addLast(ticketUpdateRequest);
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

    private TicketUpdateRequest getFirstRequestInQueue(AtomicReference<LinkedList<TicketUpdateRequest>> requestQueue) throws EmptyQueueException {
        if (requestQueue.get().size() > 0) {
            TicketUpdateRequest ticketUpdateRequest = requestQueue.get().removeFirst();
            Logger.info("TrelloUpdateRequestQueue: Dumping request. ticket id: " + ticketUpdateRequest.getTicketId() + " queue size=" + requestQueue.get().size());
            return ticketUpdateRequest;
        } else {
            throw new EmptyQueueException();
        }
    }

}
