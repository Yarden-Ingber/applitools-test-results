package com.yarden.restServiceDemo.firebaseService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.pojos.EyesResultRequestJson;
import com.yarden.restServiceDemo.pojos.RequestInterface;
import com.yarden.restServiceDemo.pojos.SdkResultRequestJson;
import javassist.NotFoundException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class FirebaseResultsJsonsService extends TimerTask {
    private static AtomicReference<HashMap<String, RequestInterface>> sdkRequestMap = new AtomicReference<>();
    private static AtomicReference<HashMap<String, RequestInterface>> eyesRequestMap = new AtomicReference<>();
    private static boolean isRunning = false;
    private static final String lockQueue = "LOCK_QUEUE";
    private static final String lockFirebaseConnection = "LOCK_FIREBASE_CONNECTION";
    private static Timer timer;

    @EventListener(ApplicationReadyEvent.class)
    public static void start() {
        synchronized (lockQueue) {
            if (!isRunning) {
                timer = new Timer("FirebaseQueue");
                if (sdkRequestMap.get() == null) {
                    sdkRequestMap.set(new HashMap<>());
                }
                if (eyesRequestMap.get() == null) {
                    eyesRequestMap.set(new HashMap<>());
                }
                timer.scheduleAtFixedRate(new FirebaseResultsJsonsService(), 30, 1000 * 60 * 5);
                isRunning = true;
                Logger.info("FirebaseQueue started");
            }
        }
    }

    @Override
    public synchronized void run() {
        try {
            dumpMappedRequestsToFirebase();
        } catch (Throwable t) {
            Logger.error("FirebaseResultsJsonsService: Failed to dump requests to firebase");
            t.printStackTrace();
        }
    }

    public static void dumpMappedRequestsToFirebase(){
        synchronizedDumpRequests(sdkRequestMap, FirebasePrefixStrings.Sdk);
        synchronizedDumpRequests(eyesRequestMap, FirebasePrefixStrings.Eyes);
        System.gc();
    }

    public static void addSdkRequestToFirebase(SdkResultRequestJson request) {
        addRequestToMap(request, sdkRequestMap, FirebasePrefixStrings.Sdk);
    }

    public static void addEyesRequestToFirebase(String json) {
        EyesResultRequestJson request = new Gson().fromJson(json, EyesResultRequestJson.class);
        addRequestToMap(request, eyesRequestMap, FirebasePrefixStrings.Eyes);
    }

    public static String getCurrentSdkRequestFromFirebase(RequestInterface request) throws NotFoundException {
        try {
            return getCurrentRequestFromFirebase(request, FirebasePrefixStrings.Sdk);
        } catch (Throwable t) {
            throw new NotFoundException("");
        }
    }

    public static String getCurrentEyesRequestFromFirebase(RequestInterface request) throws NotFoundException {
        try {
            return getCurrentRequestFromFirebase(request, FirebasePrefixStrings.Eyes);
        } catch (Throwable t) {
            throw new NotFoundException("");
        }
    }

    private static synchronized void synchronizedDumpRequests(AtomicReference<HashMap<String, RequestInterface>> queue, FirebasePrefixStrings prefix){
        synchronized (lockFirebaseConnection) {
            Set<String> queueKeys = new HashSet<>();
            synchronized (lockQueue) {
                queueKeys.addAll(queue.get().keySet());
            }
            for (String key : queueKeys) {
                RequestInterface request;
                synchronized (lockQueue) {
                    request = queue.get().remove(key);
                }
                addRequestToFirebase(request, prefix);
            }
        }
    }

    private static void addRequestToMap(RequestInterface request, AtomicReference<HashMap<String, RequestInterface>> requestMap, FirebasePrefixStrings fileNamePrefixInFirebase){
        if (isSandbox(request)) {
            return;
        }
        String requestMapKey = getResultRequestJsonFileName(request, fileNamePrefixInFirebase.value);
        synchronized (lockQueue) {
            try {
                if (requestMap.get().containsKey(requestMapKey)) {
                    request = joinRequests(requestMap.get().get(requestMapKey), request);
                }
                Logger.info("FirebaseResultsJsonsService: adding request to queue: " + request.getSdk() + "-" + request.getId());
                requestMap.get().put(requestMapKey, request);
            } catch (NullPointerException e) {
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static void addRequestToFirebase(RequestInterface request, FirebasePrefixStrings fileNamePrefixInFirebase) {
        if (isSandbox(request)) {
            return;
        }
        RequestInterface resultRequestJsonFromFirebase = null;
        try {
            String currentRequestFromFirebase = getCurrentRequestFromFirebase(request, fileNamePrefixInFirebase);
            resultRequestJsonFromFirebase = joinRequests(request, new Gson().fromJson(currentRequestFromFirebase, request.getClass()));
        } catch (Throwable t) {
            resultRequestJsonFromFirebase = request;
        }
        try {
            logRequestSentToFirebase(request, fileNamePrefixInFirebase);
            resultRequestJsonFromFirebase.setTimestamp(Logger.getTimaStamp());
            String jsonUrlInFirebase = getFirebaseUrl(request, fileNamePrefixInFirebase);
            patchFirebaseRequest(jsonUrlInFirebase, new Gson().toJson(resultRequestJsonFromFirebase));
        } catch (IOException | InterruptedException e) {
            Logger.error("FirebaseResultsJsonsService: Failed to add result to firebase");
        }
    }

    private static void logRequestSentToFirebase(RequestInterface request, FirebasePrefixStrings fileNamePrefixInFirebase){
        String sdk = "";
        if (request.getClass().isInstance(SdkResultRequestJson.class)) {
            sdk = ((SdkResultRequestJson) request).getSdk();
        }
        Logger.info("FirebaseResultsJsonsService: sending request to firebase: " + request.getId() + "; " + request.getGroup() + "; " + fileNamePrefixInFirebase + "; " + sdk);
    }

    private static RequestInterface joinRequests(RequestInterface firstRequest, RequestInterface secondRequest) {
        RequestInterface resultRequest = firstRequest;
        JsonArray results = firstRequest.getResults();
        results.addAll(secondRequest.getResults());
        resultRequest.setResults(results);
        return resultRequest;
    }

    private static String getResultRequestJsonFileName(RequestInterface request, String requestFileNamePrefix){
        return requestFileNamePrefix + "-" + request.getSdk() + "-" + request.getId() + "-" + request.getGroup().toLowerCase();
    }

    private static String getCurrentRequestFromFirebase(RequestInterface request, FirebasePrefixStrings fileNamePrefixInFirebase) throws IOException, InterruptedException, NotFoundException {
        String url = getFirebaseUrl(request, fileNamePrefixInFirebase);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        if (result.equals("null")) {
            throw new NotFoundException("");
        }
        return result;
    }

    private static void patchFirebaseRequest(String url, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
                .header("Content-Type", "application/json")
                .build();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpResponse response = httpClient.send(request,HttpResponse.BodyHandlers.ofString());
        Logger.info("FirebaseResultsJsonsService: response from firebase: " + response.statusCode());
    }

    private static String getFirebaseUrl(RequestInterface request, FirebasePrefixStrings fileNamePrefixInFirebase){
        String url = "https://sdk-reports.firebaseio.com/" + getResultRequestJsonFileName(request, fileNamePrefixInFirebase.value) + ".json";
        return url.replace(" ", "_");
    }

    private static boolean isSandbox(RequestInterface request) {
        return (request.getSandbox() != null) && request.getSandbox();
    }

    @Test
    public void unitTests() throws IOException {
        testAddingMultipleRequestsToFirebaseResultsInCorrectResultSize();
        testAddingMultipleRequestsToFirebaseResultsInCorrectRequestBody();
    }

    private void testAddingMultipleRequestsToFirebaseResultsInCorrectResultSize() throws IOException {
        if (sdkRequestMap.get() == null) {
            sdkRequestMap.set(new HashMap<>());
        }
        InputStream inputStream = null;
        String json = null;
        inputStream = FirebaseResultsJsonsService.class.getResourceAsStream("/testResources/request1.txt");
        json = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        SdkResultRequestJson sdkResultRequestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
        addSdkRequestToFirebase(sdkResultRequestJson);
        inputStream = FirebaseResultsJsonsService.class.getResourceAsStream("/testResources/request2.txt");
        json = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        sdkResultRequestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
        addSdkRequestToFirebase(sdkResultRequestJson);
        String key = (String)sdkRequestMap.get().keySet().toArray()[0];
        Assert.assertTrue(sdkRequestMap.get().get(key).getResults().size() == 337);
        sdkRequestMap.get().clear();
    }

    private void testAddingMultipleRequestsToFirebaseResultsInCorrectRequestBody() throws IOException {
        if (sdkRequestMap.get() == null) {
            sdkRequestMap.set(new HashMap<>());
        }
        InputStream inputStream = null;
        String json = null;
        inputStream = FirebaseResultsJsonsService.class.getResourceAsStream("/testResources/request3.txt");
        json = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        SdkResultRequestJson sdkResultRequestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
        addSdkRequestToFirebase(sdkResultRequestJson);
        inputStream = FirebaseResultsJsonsService.class.getResourceAsStream("/testResources/request4.txt");
        json = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        sdkResultRequestJson = new Gson().fromJson(json, SdkResultRequestJson.class);
        addSdkRequestToFirebase(sdkResultRequestJson);
        String key = (String)sdkRequestMap.get().keySet().toArray()[0];
        inputStream = FirebaseResultsJsonsService.class.getResourceAsStream("/testResources/resultRequestAfterJoin.txt");
        json = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        Assert.assertEquals(json, new Gson().toJson(sdkRequestMap.get().get(key)));
        sdkRequestMap.get().clear();
    }

    public enum FirebasePrefixStrings {
        Sdk("sdk"), Eyes("Eyes");

        public final String value;

        FirebasePrefixStrings(String value) {
            this.value = value;
        }

    }

}
