package com.yarden.restServiceDemo.reportService;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SheetData {

    private List<String> columnNames = null;
    private SheetTabIdentifier sheetTabIdentifier;
    private static Map<SheetTabIdentifier, List<String>> columnsNamesMap = new HashMap<>();
    private static Map<SheetTabIdentifier, JsonArray> sheetDataPerTabMap = new HashMap<>();
    private static final String lock = "lock";

    public SheetData(SheetTabIdentifier sheetTabIdentifier){
        this.sheetTabIdentifier = sheetTabIdentifier;
    }

    public JsonArray getSheetData(){
        synchronized (lock){
            if (!sheetDataPerTabMap.containsKey(sheetTabIdentifier)) {
                try {
                    try {
                        List<List<Object>> sheet = SheetDBApiService.getAllSheet(sheetTabIdentifier);
                        columnsNamesMap.put(sheetTabIdentifier, SheetDBApiService.getKeyList(sheet));
                        sheetDataPerTabMap.put(sheetTabIdentifier, SheetDBApiService.listToJsonArray(sheet));
                    } catch (Throwable t1) {
                        Logger.warn("SheetDBApiService.getAllSheet failed");
                        Thread.sleep(3000);
                        Logger.warn("Retrying getSheetData");
                        List<List<Object>> sheet = SheetDBApiService.getAllSheet(sheetTabIdentifier);
                        sheetDataPerTabMap.put(sheetTabIdentifier, SheetDBApiService.listToJsonArray(sheet));
                    }
                } catch (Throwable t) {
                    Logger.error("failed getting sheet:" + t.getMessage());
                    t.printStackTrace();
                }
            }
            return sheetDataPerTabMap.get(sheetTabIdentifier);
        }
    }

    public String getSheetDataAsCsvString() {
        StringBuilder resultCsv = new StringBuilder();
        final List<String> columns = this.getColumnNames();
        for (String column : columns) {
            resultCsv.append(column);
            resultCsv.append(",");
        }
        resultCsv.append("\n");
        JsonArray sheetData = this.getSheetData();
        for (JsonElement sheetEntry : sheetData) {
            for (String column : columns) {
                String cellValue = sheetEntry.getAsJsonObject().get(column).getAsString();
                resultCsv.append(StringUtils.isEmpty(cellValue) ? "" : cellValue);
                resultCsv.append(",");
            }
            resultCsv.append("\n");
        }
        return resultCsv.toString();
    }

    public void writeSheet() throws IOException {
        if (resultsCount.get() >= PostResultsBufferSize) {
            writeAllTabsToSheet();
        }
    }

    public static void writeAllTabsToSheet() throws IOException {
        Logger.info("Writing all sheets to google");
        for (Enums.SdkGeneralSheetTabsNames tab: Enums.SdkGeneralSheetTabsNames.values()) {
            writeSpecificSheetTab(Enums.SpreadsheetIDs.SDK.value, tab.value);
        }
        for (Enums.SdkGroupsSheetTabNames tab: Enums.SdkGroupsSheetTabNames.values()) {
            writeSpecificSheetTab(Enums.SpreadsheetIDs.SDK.value, tab.value);
        }
        for (Enums.EyesSheetTabsNames tab: Enums.EyesSheetTabsNames.values()) {
            writeSpecificSheetTab(Enums.SpreadsheetIDs.Eyes.value, tab.value);
        }
        for (Enums.VisualGridSheetTabsNames tab: Enums.VisualGridSheetTabsNames.values()) {
            writeSpecificSheetTab(Enums.SpreadsheetIDs.VisualGrid.value, tab.value);
        }
        for (Enums.SdkVersionsSheetTabsNames tab: Enums.SdkVersionsSheetTabsNames.values()) {
            writeSpecificSheetTab(Enums.SpreadsheetIDs.SdkVersions.value, tab.value);
        }
        for (Enums.KPIsSheetTabsNames tab: Enums.KPIsSheetTabsNames.values()) {
            writeSpecificSheetTab(Enums.SpreadsheetIDs.KPIS.value, tab.value);
        }
        clearCachedSheetData();
    }

    private static void writeSpecificSheetTab(String spreadsheetID, String sheetTabName) throws IOException {
        synchronized (lock){
            SheetTabIdentifier sheetTabIdentifier = new SheetTabIdentifier(spreadsheetID, sheetTabName);
            if (sheetDataPerTabMap.containsKey(sheetTabIdentifier)) {
                SheetData sheetData = new SheetData(sheetTabIdentifier);
                sheetData.columnNames = columnsNamesMap.get(sheetTabIdentifier);
                sheetData.sheetTabIdentifier = sheetTabIdentifier;
                try {
                    SheetDBApiService.updateSheet(sheetData);
                } catch (Throwable t){
                    Logger.warn("SheetDBApiService.updateSheet failed");
                    t.printStackTrace();
                    try { Thread.currentThread().sleep(3000); } catch (Throwable e) { }
                    Logger.warn("Retrying writeSpecificSheetTab");
                    SheetDBApiService.updateSheet(sheetData);
                }
            }
        }
    }

    public void addElementToBeginningOfReportSheet(JsonElement jsonElement){
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonElement);
        for (JsonElement sheetEntry: getSheetData()){
            jsonArray.add(sheetEntry);
        }
        sheetDataPerTabMap.put(sheetTabIdentifier, jsonArray);
    }

    public void deleteLastRowInSheet() {
        getSheetData().remove(getSheetData().size() - 1);
    }

    public List<String> getColumnNames(){
        List<String> result = columnsNamesMap.get(sheetTabIdentifier);
        if (result == null) {
            Logger.error("No columns for tab identifier: " + sheetTabIdentifier);
            List<List<Object>> sheet = SheetDBApiService.getAllSheet(sheetTabIdentifier);
            columnsNamesMap.put(sheetTabIdentifier, SheetDBApiService.getKeyList(sheet));
            result = columnsNamesMap.get(sheetTabIdentifier);
            if (result == null) {
                Logger.error("Still no columns for tab identifier: " + sheetTabIdentifier);
            }
        }
        return result;
    }

    public SheetTabIdentifier getSheetTabIdentifier(){
        return this.sheetTabIdentifier;
    }

    public static void clearCachedSheetData(){
        Logger.info("Clearing cached sheet maps");
        sheetDataPerTabMap.clear();
        columnsNamesMap.clear();
        resultsCount.set(0);
        System.gc();
    }

    public static void incrementResultsCounter(){
        try {
            resultsCount.set(resultsCount.get() + 1);
        } catch (Throwable t) {
            resultsCount.set(1);
        }
    }

    public static void resetResultsCounterIfBiggerThankResultsBufferSize(){
        try {
            if (resultsCount.get() >= PostResultsBufferSize) {
                SheetData.clearCachedSheetData();
                resultsCount.set(0);
            }
        } catch (Throwable t) {
            resultsCount.set(0);
        }
    }

    public static final int PostResultsBufferSize = 200;
    public static AtomicReference<Integer> resultsCount = new AtomicReference<>();

}
