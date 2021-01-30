package com.yarden.restServiceDemo.slackService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.awsS3Service.AwsS3Provider;
import com.yarden.restServiceDemo.firebaseService.FirebaseResultsJsonsService;
import com.yarden.restServiceDemo.pojos.SdkResultRequestJson;
import com.yarden.restServiceDemo.pojos.SlackReportNotificationJson;
import com.yarden.restServiceDemo.pojos.TestResultData;
import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import javassist.NotFoundException;
import org.apache.http.impl.execchain.RequestAbortedException;

import java.util.HashSet;

public class SdkReleaseEventHighLevelReportTableBuilder extends SdkHighLevelTableBuilderBaseClass{

    public final String currentTotalTestCount;
    public final String currentSpecificTestCount;
    public final String currentGenericTestCount;
    public final String currentUnexecutedGenericTestCount;
    public final String previousSpecificTestCount;
    public final String previousGenericTestCount;
    public final String previousTotalTestCount;
    public final String previousUnexecutedGenericTestCount;

    public SdkReleaseEventHighLevelReportTableBuilder(SlackReportNotificationJson requestJson) throws RequestAbortedException {
        super(requestJson);
        if (getFailedTestCountForSdk((String testName) -> true) > 0) {
            throw new RequestAbortedException("There are failed tests in the excel sheet");
        }
        currentSpecificTestCount = Integer.toString(getPassedTestCountForSdk((String testName) -> !testName.contains(Enums.Strings.Generic.value)));
        currentGenericTestCount = Integer.toString(getPassedTestCountForSdk((String testName) -> testName.contains(Enums.Strings.Generic.value)));
        currentTotalTestCount = String.valueOf(Integer.parseInt(currentGenericTestCount) + Integer.parseInt(currentSpecificTestCount));
        currentUnexecutedGenericTestCount = Integer.toString(getMissingGenericTestsCount());
        if (currentTotalTestCount == "0"){
            throw new RequestAbortedException("No test results in sheet for sdk: " + requestJson.getSdk());
        }
        String previousSpecificTestCountFileName = requestJson.getSdk() + "PreviousSpecificTestCount.txt";
        String previousGenericTestCountFileName = requestJson.getSdk() + "PreviousGenericTestCount.txt";
        String previousTotalTestCountFileName = requestJson.getSdk() + "PreviousTotalTestCount.txt";
        String previousUnexecutedGenericTestCountFileName = requestJson.getSdk() + "PreviousUnexecutedGenericTestCount.txt";
        previousSpecificTestCount = getTestCountFromFileNameInS3(previousSpecificTestCountFileName);
        previousGenericTestCount = getTestCountFromFileNameInS3(previousGenericTestCountFileName);
        previousTotalTestCount = getTestCountFromFileNameInS3(previousTotalTestCountFileName);
        previousUnexecutedGenericTestCount = getTestCountFromFileNameInS3(previousUnexecutedGenericTestCountFileName);
        AwsS3Provider.writeStringToFile(Enums.EnvVariables.AwsS3SdkReportsBucketName.value, previousSpecificTestCountFileName, currentSpecificTestCount);
        AwsS3Provider.writeStringToFile(Enums.EnvVariables.AwsS3SdkReportsBucketName.value, previousGenericTestCountFileName, currentGenericTestCount);
        AwsS3Provider.writeStringToFile(Enums.EnvVariables.AwsS3SdkReportsBucketName.value, previousTotalTestCountFileName, currentTotalTestCount);
        AwsS3Provider.writeStringToFile(Enums.EnvVariables.AwsS3SdkReportsBucketName.value, previousUnexecutedGenericTestCountFileName, currentUnexecutedGenericTestCount);
    }

    public HTMLTableBuilder getHighLevelReportTable() throws RequestAbortedException {
        HTMLTableBuilder tableBuilder = new HTMLTableBuilder(false, 3, 5);
        tableBuilder.addTableHeader("Test run", "Total test count", "Specific test count", "Generic test count", "Unexecuted generic tests");
        tableBuilder.addRowValues(true, "Current", currentTotalTestCount, currentSpecificTestCount, currentGenericTestCount, currentUnexecutedGenericTestCount);
        tableBuilder.addRowValues(true, "Previous", previousTotalTestCount, previousSpecificTestCount, previousGenericTestCount, previousUnexecutedGenericTestCount);
        return tableBuilder;
    }

    private String getTestCountFromFileNameInS3(String fileName){
        try {
            return AwsS3Provider.getStringFromFile(Enums.EnvVariables.AwsS3SdkReportsBucketName.value, fileName);
        } catch (Throwable t) {
            Logger.warn("No file named: " + fileName + " in S3 bucket: " + Enums.EnvVariables.AwsS3SdkReportsBucketName.value);
            t.printStackTrace();
        }
        return "";
    }

    private int getMissingGenericTestsCount(){
        int count = 0;
        FirebaseResultsJsonsService.dumpMappedRequestsToFirebase();
        for (Enums.SdkGroupsSheetTabNames group : Enums.SdkGroupsSheetTabNames.values()) {
            String id = getIdForSdkByGroup(group);
            try {
                SdkResultRequestJson sdkResultRequestJson = new Gson().fromJson(FirebaseResultsJsonsService.getCurrentSdkRequestFromFirebase(id, group.value), SdkResultRequestJson.class);
                JsonArray resultsArray = sdkResultRequestJson.getResults();
                for (JsonElement result: resultsArray) {
                    TestResultData testResult = new Gson().fromJson(result, TestResultData.class);
                    if (testResult.isGeneric() && testResult.isSkipped()) {
                        count++;
                    }
                }
            } catch (NotFoundException e) {
                Logger.warn("SdkReleaseEventHighLevelReportTableBuilder: Failed to count missing generic tests for sdk: " + requestJson.getSdk() + " group: " + group + " id: " + requestJson.getId());
            }
        }
        return count;
    }

    private String getIdForSdkByGroup(Enums.SdkGroupsSheetTabNames group){
        JsonArray reportSheet = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, group.value)).getSheetData();
        for (JsonElement sheetEntry: reportSheet){
            if (sheetEntry.getAsJsonObject().get(Enums.SdkSheetColumnNames.TestName.value).getAsString().equals(Enums.SdkSheetColumnNames.IDRow.value)){
                return sheetEntry.getAsJsonObject().get(requestJson.getSdk()).getAsString();
            }
        }
        return "";
    }

}
