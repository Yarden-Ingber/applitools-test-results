package com.yarden.restServiceDemo.slackService.dailySdkReport;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import com.yarden.restServiceDemo.slackService.HTMLTableBuilder;

import java.util.ArrayList;
import java.util.List;

public class DailySdkHighLevelReportTableBuilder {

    final List<String> sdks;
    final SheetData reportSheet;
    final Table<String, String, String> highLevelReportTable;
    final String PASSED = "Passed";
    final String FAILED = "Failed";
    final String MISSING = "Missing";

    public DailySdkHighLevelReportTableBuilder() {
        this.reportSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, Enums.SdkGroupsSheetTabNames.Selenium.value));
        this.sdks = reportSheet.getColumnNames();
        sdks.remove(Enums.SdkSheetColumnNames.TestName.value);
        this.highLevelReportTable = HashBasedTable.create();
        calculateTableForAllSdks();
    }

    public HTMLTableBuilder getHighLevelReportTable() {
        HTMLTableBuilder tableBuilder = new HTMLTableBuilder(false, 3, sdks.size() + 1);
        List<String> tableHeader = new ArrayList<>();
        tableHeader.add("Status");
        tableHeader.addAll(highLevelReportTable.columnKeySet());
        tableBuilder.addTableHeader(tableHeader.toArray(new String[0]));
        addPassedResultsToHtmlTable(tableBuilder);
        addFailedResultsToHtmlTable(tableBuilder);
        return tableBuilder;
    }

    private void addPassedResultsToHtmlTable(HTMLTableBuilder tableBuilder) {
        List<String> passedRow = new ArrayList<>();
        passedRow.add(PASSED);
        passedRow.addAll(highLevelReportTable.row(PASSED).values());
        tableBuilder.addRowValues(true, passedRow.toArray(new String[0]));
    }

    private void addFailedResultsToHtmlTable(HTMLTableBuilder tableBuilder) {
        List<String> failedRow = new ArrayList<>();
        failedRow.add(FAILED);
        failedRow.addAll(highLevelReportTable.row(FAILED).values());
        tableBuilder.addRowValues(true, failedRow.toArray(new String[0]));
    }

    private void calculateTableForAllSdks(){
        int countPassed = 0; int countFailed = 0; int countMissing = 0;
        JsonArray sheetArray = reportSheet.getSheetData();
        for (String sdk : sdks) {
            for (JsonElement sheetEntry : sheetArray) {
                if (!sheetEntry.getAsJsonObject().get(Enums.SdkSheetColumnNames.TestName.value).getAsString().equals(Enums.SdkSheetColumnNames.IDRow.value)) {
                    if (sheetEntry.getAsJsonObject().get(sdk).getAsString().equals(Enums.TestResults.Passed.value)) {
                        countPassed++;
                    } else if (sheetEntry.getAsJsonObject().get(sdk).getAsString().equals(Enums.TestResults.Failed.value)) {
                        countFailed++;
                    } else if (sheetEntry.getAsJsonObject().get(sdk).getAsString().equals(Enums.TestResults.Missing.value)) {
                        countMissing++;
                    }
                }
            }
            highLevelReportTable.put(PASSED, sdk, String.valueOf(countPassed));
            highLevelReportTable.put(FAILED, sdk, String.valueOf(countFailed));
            highLevelReportTable.put(MISSING, sdk, String.valueOf(countMissing));
            countPassed = 0; countFailed = 0; countMissing = 0;
        }
    }

}
