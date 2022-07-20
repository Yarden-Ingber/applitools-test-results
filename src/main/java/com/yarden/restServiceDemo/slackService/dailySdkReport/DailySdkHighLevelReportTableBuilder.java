package com.yarden.restServiceDemo.slackService.dailySdkReport;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import com.yarden.restServiceDemo.slackService.HTMLTableBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DailySdkHighLevelReportTableBuilder {

    final List<String> sdks;
    final SheetData reportSheet;
    final Table<String, String, String> highLevelReportTable;
    public String suiteResultString = "";
    static final String PASSED = "Passed";
    static final String FAILED = "Failed";
    static final String MISSING = "Missing";
    static final String STATUS = "Status";

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
        tableHeader.add(STATUS);
        tableHeader.addAll(highLevelReportTable.columnKeySet());
        tableBuilder.addTableHeader(tableHeader.toArray(new String[0]));
        addPassedResultsToHtmlTable(tableBuilder);
        addFailedResultsToHtmlTable(tableBuilder);
        return tableBuilder;
    }

    public String getFailedSdksHtml() {
        StringBuilder failedSdksHtml = new StringBuilder("");
        for (String sdk : highLevelReportTable.columnKeySet()) {
            if (!sdk.equals(STATUS)) {
                if (Integer.parseInt(highLevelReportTable.get(FAILED, sdk)) > 0) {
                    failedSdksHtml.append(sdk);
                    failedSdksHtml.append("<br>");
                }
            }
        }
        String title = "Failed SDKs:<br>";
        if (!failedSdksHtml.toString().isEmpty()) {
            return title + failedSdksHtml.toString() + "<br>";
        } else {
            return title + "None<br><br>";
        }
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
                        setSuiteResult(FAILED);
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
        setSuiteResult(PASSED);
    }

    private void setSuiteResult(String result) {
        if (StringUtils.isEmpty(suiteResultString)) {
            suiteResultString = result;
        }
    }

}
