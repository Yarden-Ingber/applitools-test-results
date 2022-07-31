package com.yarden.restServiceDemo.reportService;

import com.google.gson.*;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.pojos.ExtraDataPojo;
import com.yarden.restServiceDemo.pojos.ExtraDataRequestJson;
import com.yarden.restServiceDemo.pojos.SdkResultRequestJson;
import com.yarden.restServiceDemo.pojos.TestResultData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SdkReportService {

    private SdkResultRequestJson sdkResultRequestJson;
    private ExtraDataRequestJson extraDataRequestJson;
    private String googleSheetTabName;
    private SheetData sheetData = null;

    public void postResults(SdkResultRequestJson _sdkResultRequestJson) throws JsonSyntaxException, InternalError{
        SheetData.incrementResultsCounter();
        this.sdkResultRequestJson = _sdkResultRequestJson;
        if (sdkResultRequestJson.getGroup() == null || sdkResultRequestJson.getGroup().isEmpty()){
            throw new JsonSyntaxException("Missing group parameter in json");
        }
        sdkResultRequestJson.setGroup(capitalize(sdkResultRequestJson.getGroup()));
        setGoogleSheetTabName();
        sheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, googleSheetTabName));
        new SdkRequestJsonValidator(sdkResultRequestJson).validate(sheetData);
        try {
            deleteColumnForNewTestId();
            updateSheetWithNewResults();
            validateThereIsIdRowOnSheet(sheetData);
            writeEntireSheetData(sheetData);
        } catch (Throwable t) {
            Logger.error("Something went wrong: " + t.getMessage());
            t.printStackTrace();
            throw new InternalError();
        }
        Logger.info("Test result count is: " + SheetData.resultsCount.get());
        SheetData.resetResultsCounterIfBiggerThankResultsBufferSize();
    }

    public void postExtraTestData(String json) throws JsonSyntaxException, InternalError{
        googleSheetTabName = Enums.SdkGeneralSheetTabsNames.Sandbox.value;
        extraDataRequestJson = new Gson().fromJson(json, ExtraDataRequestJson.class);
        sheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, googleSheetTabName));
        try {
            updateSheetWithExtraTestData();
            writeEntireSheetData(sheetData);
        } catch (Throwable t) {
            throw new InternalError();
        }
    }

    public void deleteAllColumnsForSdkInAllTabs(String json){
        sdkResultRequestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
        Logger.info("Deleting all result columns in all tabs for sdk: " + sdkResultRequestJson.getSdk());
        for (Enums.SdkGroupsSheetTabNames tabName : Enums.SdkGroupsSheetTabNames.values()) {
            sheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, tabName.value));
            deleteTestResultsForSdk(sdkResultRequestJson.getSdk());
        }
    }

    public static void deleteEntireDailyReportResults(){
        Logger.info("Deleting all result columns in Daily tab report");
        SheetData sheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, Enums.SdkGroupsSheetTabNames.Daily.value));
        List<String> sheetColumns = sheetData.getColumnNames();
        for (JsonElement sheetEntry : sheetData.getSheetData()) {
            for (String column : sheetColumns) {
                if (!Enums.SdkSheetColumnNames.TestName.value.equals(column)) {
                    sheetEntry.getAsJsonObject().addProperty(column, "");
                }
            }
        }
    }

    private void updateSheetWithExtraTestData(){
        JsonArray extraDataArray = extraDataRequestJson.getExtraData();
        for (JsonElement testData: extraDataArray){
            ExtraDataPojo extraDataPojo = new Gson().fromJson(testData, ExtraDataPojo.class);
            String testName = capitalize(extraDataPojo.getTestName());
            addExtraDataToSingleTestInSandbox(extraDataRequestJson.getSdk(), testName, extraDataPojo.getData());
        }
    }

    private void updateSheetWithNewResults(){
        if (!isSandbox(sdkResultRequestJson)) {
            Logger.info("Updating results in local cached sheet");
        }
        JsonArray resultsArray = sdkResultRequestJson.getResults();
        for (JsonElement result: resultsArray) {
            if (result != null && !result.isJsonNull()) {
                TestResultData testResult = new Gson().fromJson(result, TestResultData.class);
                if (!(testResult.isGeneric() && testResult.isSkipped())) {
                    try {
                        String testName = addGenericTestFlag(testResult, capitalize(testResult.getTestName()));
                        String paramsString = getTestParamsAsString(testResult);
                        testName = testName + paramsString;
                        updateSingleTestResult(sdkResultRequestJson.getSdk(), testName, testResult.getPassed());
                    } catch (Exception e) {
                        Logger.error("Failed to add a test result: TestName=" + testResult.getTestName() + "; " + " Parameters=" + testResult.getParameters().toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String addGenericTestFlag(TestResultData testResult, String testName){
        if (testResult.isGeneric()) {
            return testName + Enums.Strings.Generic.value;
        }
        return testName;
    }

    private String getTestParamsAsString(TestResultData testResult){
        if (testResult.getParameters() == null)
            return "";
        List<Map.Entry<String, JsonElement>> paramsList = new ArrayList<>(testResult.getParameters().entrySet());
        paramsList.sort(new ParamsComperator());
        StringBuilder paramsString = new StringBuilder();
        for (Map.Entry<String, JsonElement> param: paramsList) {
            String key = param.getKey();
            String value = "";
            if (param.getValue() == null || param.getValue().isJsonNull()) {
                value = "null";
            } else {
                try {
                    value = param.getValue().getAsString();
                } catch (Exception e) {
                    try {
                        value = param.getValue().toString();
                    } catch (Exception ex) {
                        Logger.error("Failed to get a test parameter: (" + key + ":" + param.getValue() + ") for test " + testResult.getTestName());
                        ex.printStackTrace();
                    }
                }
            }
            paramsString.append("(").append(capitalize(key)).append(":").append(capitalize(value)).append(") ");
        }
        return paramsString.toString().trim();
    }

    private void deleteColumnForNewTestId(){
        String currentColumnId = getCurrentColumnId(sdkResultRequestJson.getSdk());
        if (!isSandbox(sdkResultRequestJson)) {
            Logger.info("Current id in sheet for sdk " + sdkResultRequestJson.getSdk() + " is: " + currentColumnId);
            Logger.info("New requested id for sdk " + sdkResultRequestJson.getSdk() + " is: " + sdkResultRequestJson.getId());
        }
        if (!sdkResultRequestJson.getId().equals(currentColumnId)){
            if (!isSandbox(sdkResultRequestJson)) {
                Logger.info("Updating id for sdk: " + sdkResultRequestJson.getSdk());
            }
            deleteEntireSdkColumn(sdkResultRequestJson.getSdk());
            updateTestResultId(sdkResultRequestJson.getSdk(), sdkResultRequestJson.getId());
        }
    }

    private String getCurrentColumnId(String sdk){
        try {
            for (JsonElement sheetEntry: sheetData.getSheetData()){
                if (sheetEntry.getAsJsonObject().get(Enums.SdkSheetColumnNames.TestName.value).getAsString().equals(Enums.SdkSheetColumnNames.IDRow.value)){
                    return sheetEntry.getAsJsonObject().get(sdk).getAsString();
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private void updateTestResultId(String sdk, String id){
        for (JsonElement sheetEntry: sheetData.getSheetData()){
            if (sheetEntry.getAsJsonObject().get(Enums.SdkSheetColumnNames.TestName.value).getAsString().equals(Enums.SdkSheetColumnNames.IDRow.value)){
                sheetEntry.getAsJsonObject().addProperty(sdk, id);
                return;
            }
        }
    }

    private void addExtraDataToSingleTestInSandbox(String sdk, String testName, String extraData){
        Logger.info("Adding extra data to test " + testName + " on sdk " + sdk + ": " + extraData);
        for (JsonElement sheetEntry: sheetData.getSheetData()){
            if (sheetEntry.getAsJsonObject().get(Enums.SdkSheetColumnNames.TestName.value).getAsString().equals(testName)){
                sheetEntry.getAsJsonObject().addProperty(sdk + Enums.SdkSheetColumnNames.ExtraData.value, extraData);
                return;
            }
        }
        JsonElement newEntry = new JsonParser().parse("{\"" + Enums.SdkSheetColumnNames.TestName.value + "\":\"" + testName + "\",\"" + sdk + Enums.SdkSheetColumnNames.ExtraData.value + "\":\"" + extraData + "\"}");
        Logger.info("Adding new result entry: " + newEntry.toString() + " to sheet");
        sheetData.getSheetData().add(newEntry);
    }

    private void updateSingleTestResult(String sdk, String testName, boolean passed){
        String testResult = passed ? Enums.TestResults.Passed.value : Enums.TestResults.Failed.value;
        for (JsonElement sheetEntry: sheetData.getSheetData()){
            if (sheetEntry.getAsJsonObject().get(Enums.SdkSheetColumnNames.TestName.value).getAsString().equals(testName)){
                if (!isSandbox(sdkResultRequestJson)) {
//                    Logger.info("Adding test result for sdk: " + sdk + ", " + testName + "=" + testResult);
                }
                sheetEntry.getAsJsonObject().addProperty(sdk, testResult);
                return;
            }
        }
        JsonElement newEntry = new JsonParser().parse("{\"" + Enums.SdkSheetColumnNames.TestName.value + "\":\"" + testName + "\",\"" + sdk + "\":\"" + testResult + "\"}");
//        Logger.info("Adding new result entry: " + newEntry.toString() + " to sheet");
        sheetData.getSheetData().add(newEntry);
    }

    private void deleteEntireSdkColumn(String sdk){
        Logger.info("Deleting entire column for sdk: " + sdk);
        deleteTestResultsForSdk(sdk);
        deleteEntireSdkExtraDataColumn(sdk);
    }

    private void deleteTestResultsForSdk(String sdk) {
        if (sheetData.getSheetData().get(0).getAsJsonObject().keySet().contains(sdk)) {
            for (JsonElement sheetEntry : sheetData.getSheetData()) {
                sheetEntry.getAsJsonObject().addProperty(sdk, "");
            }
        }
    }

    private void deleteEntireSdkExtraDataColumn(String sdk) {
        if (isSandbox(sdkResultRequestJson)) {
            for (JsonElement sheetEntry: sheetData.getSheetData()){
                sheetEntry.getAsJsonObject().addProperty(sdk + Enums.SdkSheetColumnNames.ExtraData.value, "");
            }
        }
    }

    public void validateThereIsIdRowOnSheet(SheetData sheetData){
        for (JsonElement sheetEntry : sheetData.getSheetData()) {
            if (sheetEntry.getAsJsonObject().get(Enums.SdkSheetColumnNames.TestName.value).getAsString().equals(Enums.SdkSheetColumnNames.IDRow.value)) {
                return;
            }
        }
        Logger.warn("There was no ID row");
        JsonElement newEntry = new JsonParser().parse("{\"" + Enums.SdkSheetColumnNames.TestName.value + "\":\"" + Enums.SdkSheetColumnNames.IDRow.value + "\",\"" + sdkResultRequestJson.getSdk() + "\":\"" + sdkResultRequestJson.getId() + "\"}");
        sheetData.addElementToBeginningOfReportSheet(newEntry);
        Logger.info("Now the cached sheet looks like this: " + sheetData.getSheetData().toString());
    }

    private void writeEntireSheetData(SheetData sheetData){
        try {
            int retryCount = 0;
            int maxRetry = 5;
            while (retryCount < maxRetry) {
                try {
                    sheetData.writeSheet();
                    return;
                } catch (Throwable t1) {
                    Logger.warn("Failed writing sheet. retrying...");
                    Thread.sleep(1000 );
                    retryCount++;
                }
            }
        } catch (Throwable t) {
            Logger.error("Failed to update sheet: " + t.getMessage());
        }
    }

    private static String capitalize(String s) {
        final String ACTIONABLE_DELIMITERS = " '-/_"; // these cause the character following to be capitalized
        StringBuilder sb = new StringBuilder();
        boolean capNext = true;
        for (char c : s.toCharArray()) {
            c = capNext ? Character.toUpperCase(c) : c;
            sb.append(c);
            capNext = (ACTIONABLE_DELIMITERS.indexOf((int) c) >= 0); // explicit cast not needed
        }
        return sb.toString().replaceAll("[ |\\-|_]", "");
    }

    public class ParamsComperator implements Comparator<Map.Entry<String, JsonElement>> {
        @Override
        public int compare(Map.Entry<String, JsonElement> lhs, Map.Entry<String, JsonElement> rhs) {
            if (lhs == rhs)
                return 0;
            if (lhs == null)
                return -1;
            if (rhs == null)
                return 1;
            return capitalize(lhs.getKey()).compareTo(capitalize(rhs.getKey()));
        }
    }

    private void setGoogleSheetTabName(){
        googleSheetTabName = sdkResultRequestJson.getGroup();
        if (isSandbox(sdkResultRequestJson)) {
            googleSheetTabName = Enums.SdkGeneralSheetTabsNames.Sandbox.value;
        } else {
            Logger.info("Posting result to sheet: " + googleSheetTabName);
        }
    }

    public static boolean isSandbox(SdkResultRequestJson sdkResultRequestJson){
        return (((sdkResultRequestJson.getSandbox() != null) && sdkResultRequestJson.getSandbox())
                || isTestedLocally(sdkResultRequestJson));
    }

    private static boolean isTestedLocally(SdkResultRequestJson sdkResultRequestJson){
        return sdkResultRequestJson.getId().equals("0000-0000") || sdkResultRequestJson.getId().equals("aaaa-aaaa");
    }

}
