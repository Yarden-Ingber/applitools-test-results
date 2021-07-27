package com.yarden.restServiceDemo.kpis;

import com.google.gson.JsonElement;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.RestCalls;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@Configuration
public class WriteKpisToSplunkPeriodically extends TimerTask{
    private static boolean isRunning = false;
    private static Timer timer;
    private static boolean isKpisDumped = false;
    private static final int SixMonthsAgo = 6;

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("WriteKpisToSplunkPeriodically");
            timer.scheduleAtFixedRate(new WriteKpisToSplunkPeriodically(), 30, 1000 * 60 * 10);
            isRunning = true;
            Logger.info("WriteKpisToSplunkPeriodically started");
        }
    }

    public static synchronized void stop(){
        isRunning = false;
        timer.cancel();
    }

    @Override
    public void run() {
        try {
            synchronized (RestCalls.lock) {
                try {
                    if (shouldDumpTickets()) {
                        Logger.info("WriteKpisToSplunkPeriodically: Starting KPI dump");
                        periodicDumpTickets();
                        Logger.info("WriteKpisToSplunkPeriodically: KPI dump ended");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean shouldDumpTickets() throws ParseException {
        Date date = Logger.timestampToDate(Logger.getTimaStamp());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        if (hour == 5) {
            Logger.info("WriteKpisToSplunkPeriodically: Dump window is open. Don't restart server!!!!!!!!!!!!!!");
            if (!isKpisDumped) {
                isKpisDumped = true;
                return true;
            }
        } else {
            isKpisDumped = false;
        }
        return false;
    }

    private boolean isDateWithinTimeSpan (Date ticketCreationDate, int numOfMonthsAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -numOfMonthsAgo);
        Date dateMonthsAgo = calendar.getTime();
        return dateMonthsAgo.before(ticketCreationDate);
    }

    private void periodicDumpTickets() throws ParseException {
        SheetData rawDataSheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.KPIS.value, Enums.KPIsSheetTabsNames.RawData.value));
        String timeStamp = Logger.getTimaStamp();
        updateCurrentTimestampToAllTickets(rawDataSheetData, timeStamp);
        dumpAllTicketsToSplunk(rawDataSheetData);
    }

    private void updateCurrentTimestampToAllTickets(SheetData rawDataSheetData, String timeStamp) {
        TicketsStateChanger ticketsStateChanger = new TicketsStateChanger();
        for (JsonElement sheetEntry: rawDataSheetData.getSheetData()){
            try {
                TicketStates currentState = TicketStates.valueOf(sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CurrentState.value).getAsString());
                if (!(currentState.equals(TicketStates.Done) || currentState.equals(TicketStates.NoState))) {
                    ticketsStateChanger.executeUpdateState(sheetEntry, currentState, currentState, timeStamp);
                }
            } catch (Throwable t) {}
        }
    }

    private void dumpAllTicketsToSplunk(SheetData rawDataSheetData) throws ParseException {
        for (JsonElement sheetEntry: rawDataSheetData.getSheetData()){
            Date ticketCreationDate = Logger.timestampToDate(sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.CreationDate.value).getAsString());
            if (isDateWithinTimeSpan(ticketCreationDate, SixMonthsAgo)){
                TicketUpdateRequest ticketUpdateRequest = new TicketUpdateRequest();
                ticketUpdateRequest.setTeam(sheetEntry.getAsJsonObject().get(Enums.KPIsSheetColumnNames.Team.value).getAsString());
                new KpiSplunkReporter(rawDataSheetData, ticketUpdateRequest).reportLatestState(sheetEntry);
            }
        }
    }

}