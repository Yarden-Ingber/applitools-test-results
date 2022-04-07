package com.yarden.restServiceDemo;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.yarden.restServiceDemo.firebaseService.FirebaseResultsJsonsService;
import com.yarden.restServiceDemo.pojos.SdkResultRequestJson;
import com.yarden.restServiceDemo.reportService.*;
import com.yarden.restServiceDemo.slackService.EyesSlackReporterSender;
import com.yarden.restServiceDemo.slackService.NoTestTableSlackReportSender;
import com.yarden.restServiceDemo.slackService.SdkSlackReportSender;
import com.yarden.restServiceDemo.splunkService.SplunkReporter;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestCalls {

    public static final String lock = "LOCK";
    private static final boolean PrintPayload = true;
    private static final boolean DontPrintPayload = false;

    @RequestMapping(method = RequestMethod.POST, path = "/result")
    public ResponseEntity postResults(@RequestBody String json) {
        synchronized (lock) {
            WriteEntireSheetsPeriodically.shouldStopSheetWritingTimer = false;
            WriteEntireSheetsPeriodically.start();
            newRequestPrint(json, "/result", DontPrintPayload);
            try {
                SdkResultRequestJson sdkResultRequestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
                if (!SdkReportService.isSandbox(sdkResultRequestJson)) {
                    Logger.info("Non sandbox request: " + json.replace(" ", ""));
                } else {
                    Logger.info("sandbox request: sdk=" + sdkResultRequestJson.getSdk() + "; id=" + sdkResultRequestJson.getId() + "; isSandbox=" + sdkResultRequestJson.getSandbox() + "; group=" + sdkResultRequestJson.getGroup());
                }
                FirebaseResultsJsonsService.addSdkRequestToFirebase(sdkResultRequestJson);
                new SdkReportService().postResults(sdkResultRequestJson);
            } catch (InternalError e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (JsonSyntaxException e) {
                String errorMessage = "Failed parsing the json: \n\n" + json + "\n\n" + e.getMessage();
                Logger.error(errorMessage);
                return new ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity(json, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/delete_previous_results")
    public ResponseEntity deletePreviousResults(@RequestBody String json) {
        synchronized (lock) {
            WriteEntireSheetsPeriodically.shouldStopSheetWritingTimer = false;
            WriteEntireSheetsPeriodically.start();
            newRequestPrint(json, "/delete_previous_results", PrintPayload);
            try {
                new SdkReportService().deleteAllColumnsForSdkInAllTabs(json);
            } catch (InternalError e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (JsonSyntaxException e) {
                String errorMessage = "Failed parsing the json: \n\n" + json + "\n\n" + e.getMessage();
                Logger.error(errorMessage);
                return new ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity(json, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/eyes_result")
    public ResponseEntity postEyesResults(@RequestBody String json) {
        synchronized (lock) {
            WriteEntireSheetsPeriodically.shouldStopSheetWritingTimer = false;
            WriteEntireSheetsPeriodically.start();
            newRequestPrint(json, "/eyes_result", DontPrintPayload);
            try {
                FirebaseResultsJsonsService.addEyesRequestToFirebase(json);
                new EyesReportService().postResults(json);
            } catch (InternalError e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (JsonSyntaxException e) {
                String errorMessage = "Failed parsing the json: \n\n" + json + "\n\n" + e.getMessage();
                Logger.error(errorMessage);
                return new ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity(json, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/vg_status")
    public ResponseEntity postVisualGridStatus(@RequestBody String json) {
        synchronized (lock) {
            VisualGridStatusPageRequestTimer.isRequestReceived = true;
            WriteEntireSheetsPeriodically.shouldStopSheetWritingTimer = false;
            WriteEntireSheetsPeriodically.start();
            Logger.info("**********New VG status request detected**********");
            try {
                new VisualGridStatusPageService().postResults(json);
            } catch (Throwable e) {
                String errorMessage = "Request failed: \n\n" + json + "\n\n" + e.getMessage();
                Logger.error(errorMessage);
                return new ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity(json, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/health")
    public ResponseEntity getHealth(){
        return new ResponseEntity("Up and running!", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/extra_test_data")
    public ResponseEntity postExtraTestData(@RequestBody String json){
        synchronized (lock) {
            newRequestPrint(json, "/health", DontPrintPayload);
            try {
                new SdkReportService().postExtraTestData(json);
            } catch (JsonSyntaxException e) {
                return new ResponseEntity("Failed parsing the json: " + json, HttpStatus.BAD_REQUEST);
            } catch (InternalError internalError) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity(json, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/send_mail")
    public ResponseEntity sendSdkMailReport(@RequestBody String json){
        return sendSdkMailReportOverload(json);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/send_mail/sdks")
    public ResponseEntity sendSdkMailReportOverload(@RequestBody String json){
        synchronized (lock) {
            newRequestPrint(json, "/send_mail/sdks", PrintPayload);
            try {
                SheetData.writeAllTabsToSheet();
                new SdkSlackReportSender().send(json);
            } catch (Throwable t) {
                t.printStackTrace();
                return new ResponseEntity("Failed sending email: " + t.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity("Mail sent", HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/send_full_regression/sdks")
    public ResponseEntity sendSdkFullRegressionReport(@RequestBody String json){
        synchronized (lock) {
            newRequestPrint(json, "/send_full_regression/sdks", PrintPayload);
            Logger.info(Enums.EnvVariables.TurnOffFullRegressionEmail.value);
            if (Boolean.valueOf(Enums.EnvVariables.TurnOffFullRegressionEmail.value)) {
                return new ResponseEntity("Mail is turned off by server", HttpStatus.OK);
            }
            try {
                SheetData.writeAllTabsToSheet();
                new SdkSlackReportSender().sendFullRegression(json);
            } catch (Throwable t) {
                t.printStackTrace();
                return new ResponseEntity("Failed sending email: " + t.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity("Mail sent", HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/tests_end/eyes")
    public ResponseEntity sendEyesMailReport(@RequestBody String json){
        synchronized (lock) {
            newRequestPrint(json, "/tests_end/eyes", PrintPayload);
            try {
                if (json == null) {
                    json = "{}";
                }
                new EyesSlackReporterSender().send(json);
            } catch (Throwable throwable) {
                return new ResponseEntity("Failed sending email: " + throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity("Mail sent", HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/reset_eyes_report_data")
    public ResponseEntity deleteEntireEyesData(){
        synchronized (lock) {
            WriteEntireSheetsPeriodically.shouldStopSheetWritingTimer = false;
            WriteEntireSheetsPeriodically.start();
            newRequestPrint("", "/reset_eyes_report_data", PrintPayload);
            try {
                new EyesReportService().deleteAllData();
                new EyesSlackReporterSender().resetEndTasksCounter();
            } catch (Throwable throwable) {
                return new ResponseEntity("Failed deleting eyes report data: " + throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity("Data deleted", HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/send_mail/no_tests")
    public ResponseEntity sendReleaseReportWithoutTests(@RequestBody String json){
        synchronized (lock) {
            newRequestPrint(json, "/send_mail/no_tests", PrintPayload);
            try {
                new NoTestTableSlackReportSender().send(json);
            } catch (Throwable throwable) {
                return new ResponseEntity("Failed sending email: " + throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity("Mail sent", HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/send_mail/generic")
    public ResponseEntity sendGenericMailReport(@RequestBody String json){
        synchronized (lock) {
            newRequestPrint(json, "/send_mail/generic", PrintPayload);
            try {
                new NoTestTableSlackReportSender().send(json);
            } catch (Throwable throwable) {
                return new ResponseEntity("Failed sending email: " + throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity("Mail sent", HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/dump_results")
    public ResponseEntity dumpResults(){
        synchronized (lock) {
            try {
                SheetData.writeAllTabsToSheet();
            } catch (Throwable throwable) {
                return new ResponseEntity("Failed to dump data to sheet: " + throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity("Sheet is updated", HttpStatus.OK);
        }
    }

    private void newRequestPrint(String json, String request, boolean shouldPrintPayload){
        Logger.info("**********************************************************************************************");
        Logger.info("**********************************************************************************************");
        String timestamp = Logger.getTimaStamp();
        String jsonWithoutWhitespace = json.replace(" ", "");
        if (shouldPrintPayload) {
            System.out.println(timestamp + " == INFO: " + "New request detected: " + request + " === payload: " + jsonWithoutWhitespace);
        } else {
            System.out.println(timestamp + " == INFO: " + "New request detected: " + request);
        }
        JSONObject log = new JSONObject().put("level", "info").put("text", timestamp + " == New request detected: " + request + " === payload: " + jsonWithoutWhitespace);
        new SplunkReporter().report(Enums.SplunkSourceTypes.RawServerLog, log.toString());
    }


}
