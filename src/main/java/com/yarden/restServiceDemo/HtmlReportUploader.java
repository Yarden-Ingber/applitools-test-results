package com.yarden.restServiceDemo;

import com.yarden.restServiceDemo.awsS3Service.AwsS3Provider;
import com.yarden.restServiceDemo.pojos.ReportData;

import java.io.File;

public class HtmlReportUploader {

    ReportData reportData;

    HtmlReportUploader(ReportData reportData) {
        this.reportData = reportData;
    }

    String upload(String bucketName, String htmlLocalReportFilePath) {
        String fileNameInBucket = getFileNameInBucket();
        AwsS3Provider.uploadFileToS3(bucketName, fileNameInBucket, htmlLocalReportFilePath);
        String fileUrl = AwsS3Provider.getUrlToFileInS3(bucketName, fileNameInBucket);
        try {
            new File(htmlLocalReportFilePath).delete();
        } catch (Throwable t) {t.printStackTrace();}
        return fileUrl;
    }

    private String getFileNameInBucket(){
        if (reportData.getSdk() != null && !reportData.getSdk().isEmpty()
                && reportData.getVersion() != null && !reportData.getVersion().isEmpty()) {
            return "report" + "_" + reportData.getSdk() + "_" + reportData.getVersion();
        } else {
            return "report" + "_" + Logger.getTimaStamp().replaceAll(" ", "_").replace('.', ':');
        }
    }
}
