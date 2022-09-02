package com.yarden.restServiceDemo.repoMonitoringService;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;
import com.yarden.restServiceDemo.Enums;
import com.yarden.restServiceDemo.Logger;
import com.yarden.restServiceDemo.repoMonitoringService.githubApiService.GithubApiEndpoints;
import com.yarden.restServiceDemo.repoMonitoringService.githubApiService.GithubApiRestRequests;
import com.yarden.restServiceDemo.repoMonitoringService.githubApiService.pojos.GithubRepoPojo;
import com.yarden.restServiceDemo.repoMonitoringService.npmApiService.NpmApiRestRequests;
import com.yarden.restServiceDemo.repoMonitoringService.npmApiService.pojos.NpmPackageData;
import com.yarden.restServiceDemo.repoMonitoringService.npmApiService.pojos.NpmPackagesListPojo;
import com.yarden.restServiceDemo.repoMonitoringService.npmApiService.pojos.Object;
import com.yarden.restServiceDemo.reportService.SdkReportService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RepoMonitor extends TimerTask{

    private static boolean isRunning = false;
    private static Timer timer;
    private static final String FEATURE_FLAG_REPO_MONITOR = System.getenv("FEATURE_FLAG_REPO_MONITOR");

    @EventListener(ApplicationReadyEvent.class)
    public static synchronized void start() {
        if (!isRunning) {
            timer = new Timer("RepoMonitor");
            timer.scheduleAtFixedRate(new RepoMonitor(), 30, 1000 * 60 * 60 * 5);
            isRunning = true;
            Logger.info("RepoMonitor started");
        }
    }

    @Override
    public void run() {
        try {
            Logger.info("RepoMonitor saying: \"tick...\"");
            checkGithubPublicRepos();
            checkNpmPublicPackages();
        } catch (Throwable t) {
            Logger.warn("RepoMonitor error");
            t.printStackTrace();
        }
    }

    public void checkNpmPublicPackages() throws IOException, MailjetSocketTimeoutException, MailjetException {
        List<String> npmPublicPackagesNames = getAllNpmPublicPackagesNames();
        List<String> knownPublicPackages = getKnownPublicNpmPackages();
        if (!areListsEqual(npmPublicPackagesNames, knownPublicPackages)) {
            npmPublicPackagesNames = getAllNpmPublicPackagesNames();
            knownPublicPackages = getKnownPublicNpmPackages();
            if (!areListsEqual(npmPublicPackagesNames, knownPublicPackages)) {
                sendMailWarning("NPM", findNewPackageName(knownPublicPackages, npmPublicPackagesNames));
            }
        } else {
            Logger.info("RepoMonitor: No changes in NPM packages");
        }
    }

    public void checkGithubPublicRepos() throws IOException, MailjetSocketTimeoutException, MailjetException {
        List<String> knownPublicRepos = getKnownPublicGithubRepos();
        List<String> repoNames = getPublicGithubRepoNames();
        if (!areListsEqual(knownPublicRepos, repoNames)) {
            sendMailWarning("Github", findNewPackageName(knownPublicRepos, repoNames));
        } else {
            Logger.info("RepoMonitor: No changes in Github repos");
        }
    }

    private PackageDifference findNewPackageName(List<String> knownList, List<String> receivedList) {
        for (String packageName : receivedList) {
            if (!knownList.contains(packageName)) {
                PackageDifference result = new PackageDifference();
                result.packageName = packageName;
                result.WarningMessage = packageName + ", is public but doesn't exist in the known list of public packages";
                return result;
            }
        }
        for (String packageName : knownList) {
            if (!receivedList.contains(packageName)) {
                PackageDifference result = new PackageDifference();
                result.packageName = packageName;
                result.WarningMessage = packageName + ", was public but not any more";
                return result;
            }
        }
        return new PackageDifference();
    }

    private void sendMailWarning(String codeManagerName, PackageDifference packageDifference) throws MailjetSocketTimeoutException, MailjetException {
        Logger.warn("RepoMonitor: A difference in repo\\package was discovered in " + codeManagerName + ": " + packageDifference.WarningMessage);
        if (StringUtils.equals(FEATURE_FLAG_REPO_MONITOR, "true")) {
            MailjetClient client;
            MailjetRequest request;
            MailjetResponse response;
            client = new MailjetClient(Enums.EnvVariables.MailjetApiKeyPublic.value, Enums.EnvVariables.MailjetApiKeyPrivate.value, new ClientOptions("v3.1"));
            request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", "yarden.ingber@applitools.com")
                                            .put("Name", "Yarden Ingber"))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", "yarden.ingber@applitools.com")
                                                    .put("Name", "Yarden Ingber"))
                                            .put(new JSONObject()
                                                    .put("Email", "adam.carmi@applitools.com")
                                                    .put("Name", "Adam Carmi"))
                                            .put(new JSONObject()
                                                    .put("Email", "daniel.puterman@applitools.com")
                                                    .put("Name", "Daniel Puterman"))
                                            .put(new JSONObject()
                                                    .put("Email", "yotam.madem@applitools.com")
                                                    .put("Name", "Yotam Madem"))
                                            .put(new JSONObject()
                                                    .put("Email", "amit.zur@applitools.com")
                                                    .put("Name", "Amit Zur"))
                                    )
                                    .put(Emailv31.Message.SUBJECT, "WARNING!! Public package difference found in " + codeManagerName + ": " + packageDifference.packageName)
                                    .put(Emailv31.Message.TEXTPART, "A difference in the expected public packages list found in " + codeManagerName + ". package name: " + packageDifference.WarningMessage)
                                    .put(Emailv31.Message.CUSTOMID, "RepoMonitor")));
            response = client.post(request);
            Logger.info("RepoMonitor: " + response.getStatus());
            Logger.info("RepoMonitor: " + response.getData().toString());
        }
    }

    private List<String> getPublicGithubRepoNames() throws IOException {
        List<GithubRepoPojo> repos = new LinkedList<>();
        for(int i = 1; i <= 10; i++) {
            GithubApiEndpoints endpoints = GithubApiRestRequests.getAPIService();
            Call<List<GithubRepoPojo>> call = endpoints.getRepos(100, i);
            Response<List<GithubRepoPojo>> response = call.execute();
            repos.addAll(response.body());
        }
        List<String> repoNames = new LinkedList<>();
        for (GithubRepoPojo repo: repos) {
            repoNames.add(repo.getName());
        }
        return repoNames;
    }

    private List<String> getAllNpmPublicPackagesNames() throws IOException {
        NpmPackagesListPojo npmPackagesResponse = NpmApiRestRequests.getAPIService().getRepos("@applitools", 100).execute().body();
        return extractOnlyPublicNpmPackages(npmPackagesResponse);
    }

    private List<String> extractOnlyPublicNpmPackages(NpmPackagesListPojo npmPackagesListPojo) throws IOException {
        List<String> result = new LinkedList<>();
        for (Object npmPackageData : npmPackagesListPojo.getObjects()) {
            String packageName = npmPackageData.getPackage().getName();
            Response<NpmPackageData> response = NpmApiRestRequests.getAPIService().getPackageData(packageName).execute();
            if (response.isSuccessful()) {
                result.add(packageName);
            }
        }
        return result;
    }

    private List<String> getKnownPublicNpmPackages() throws IOException {
        InputStream inputStream = SdkReportService.class.getResourceAsStream("/known-public-npm-packages.txt");
        return Arrays.asList(IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()).split("\n"));
    }

    private List<String> getKnownPublicGithubRepos() throws IOException {
        InputStream inputStream = SdkReportService.class.getResourceAsStream("/known-public-github-repos.txt");
        return Arrays.asList(IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()).split("\n"));
    }

    private boolean areListsEqual(List list1, List list2){
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    private class PackageDifference {

        public String packageName = "";
        public String WarningMessage = "";

    }

    @Test
    public void unitTests() throws IOException {
        List<String> knownPublicPackages = getKnownPublicGithubRepos();
        List<String> repoNames = new LinkedList<>();
        for (String repo : knownPublicPackages) {
            repoNames.add(repo);
        }
        repoNames.add("false package");
        PackageDifference packageDifference = findNewPackageName(knownPublicPackages, repoNames);
        Assert.assertEquals("false package", packageDifference.packageName);
    }

}
