# applitools-test-results

## Instructions
This is a Rest API for the web service to post test results to a unified report.
Below you can find a link to the google sheets report and all the endpoints of the web service. <br>
Sandbox tab is for everyday testing. Raw data tab spans the different permutations. Other tabs are the recent status of the tests for each testing package released to customers.
To toggle the results posting between Sandbox and the release tabs toggle "sandbox" boolean field in the result json.
All results are also posted to Raw data tab.
### Please use the release sheets only for RELEASE test runs. For Dev, use sandbox sheet.

## Results queue mechanism
In order to reduce the amount of requests to google, the results server adds all the requests to a buffer. Every minute the buffer gets dumped to the sheet.
This means that a new request may take up to a minute to be shown on the google sheet.

## Report

SDK: https://docs.google.com/spreadsheets/d/1JZnUB5-nEHWouHJimwyJrTyr-TFsoC9RrKI6U66HJoY <br>
Eyes: https://docs.google.com/spreadsheets/d/1kCOwx8AP6Fg0ltivnw1o55IA3ZkV3hROB1dZ61FRQh8 <br>
Visual grid status page: https://docs.google.com/spreadsheets/d/1umqCfSK3UICmw_ycbyhLjxdcyksBHYxwFNSqAXAN4SQ/edit#gid=0

## Production Endpoint

http://sdk-test-results.herokuapp.com

## Routes

Send a `GET` to `/health` - returns a `200`

### Post new test results for sdk
Send a `POST` to `/result` with the JSON payload (below) - returns a `200` and the requested JSON.

```
{  
  "sdk":"java",
  "group":"selenium",
  "id":"1234",
  "sandbox":true,
  "mandatory":false,
  "results":[  
    {  
      "test_name": "test7",
      "parameters":{
        "browser":"chrome",
        "stitching":"css"
      },
      "isSkipped":false,
      "isGeneric":false,
      "isTestRequiredForSdk":false,
      "passed":true
    },
    {  
      "test_name": "test7",
      "parameters":{
        "browser":"firefox",
        "stitching":"scroll"
      },
      "isSkipped":false,
      "isGeneric":false,
      "isTestRequiredForSdk":false,
      "passed":false
    }
  ]
}
```

<u>`group` - String<u>

Each result json will be classified to one of the Sheet tabs names. The `group` parameter should be equal to one of the tabs according to the test cases it most resembles.

Java Selenium, Javascript Selenium and WDIO should all group to Selenium tab.

<u>`id` - UUID - optional<u>

If it matches the previous run ID for a given SDK, then it will add the currently provided results to the previous ones. Otherwise, all of the results for the target SDK will be overwritten.
Use this to group together multiple results POST to a single report

<u>`sandbox` - Boolean - optional<u>

If set to true, the ["sandbox"](https://docs.google.com/spreadsheets/d/1JZnUB5-nEHWouHJimwyJrTyr-TFsoC9RrKI6U66HJoY/edit#gid=741958923) worksheet will be written to. It's useful for verifying that your SDK reports correctly to the sheet.

Set it to `false`, or stop sending it in your request, to start using the shared worksheet for all SDKs.

<u>`mandatory` - Boolean - optional only for DotNet<u>
  
If set to true && the reporting sdk is DotNet, the mandatory column will be updated. posted results with the same id will accumulate also on the mandatory column.
Non DotNet sdks can't update the mandatory column.

### Delete previous results for a specific sdk
Send a `DELETE` to `/delete_previous_results` with the JSON payload (below) - returns a `200` and the requested JSON.

```
{  
  "sdk":"java"
}
```

### Post new test results for Eyes

Send a `POST` to `/eyes_result` with the JSON payload (below) - returns a `200` and the requested JSON.
```
{  
  "group":"Front end",
  "id":"1234",
  "sandbox":false,
  "results":[  
    {  
      "test_name": "test7",
      "feature": "feature1",
      "feature_sub_category": "sub1",
      "parameters":{
        "browser":"chrome"
      },
      "passed":true
    },
    {  
      "test_name": "test7",
      "feature": "feature2",
      "feature_sub_category": "sub1",
      "parameters":{
        "browser":"firefox"
      },
      "passed":false,
      "result_url": "test.result.dashboard.url"
    }
  ]
}
```

<u>`group` - String<u>

Each result json will be classified to one of the Sheet tabs names. The `group` parameter should be equal to one of the tabs according to the test cases it most resembles.

<u>`id` - UUID<u>

If it matches the previous run ID, then it will add the currently provided results to the previous ones. Otherwise, all of the results will be overwritten.
Use this to group together multiple results POST to a single report

<u>`sandbox` - Boolean - optional<u>

If set to true, the ["sandbox"](https://docs.google.com/spreadsheets/d/1kCOwx8AP6Fg0ltivnw1o55IA3ZkV3hROB1dZ61FRQh8/edit#gid=373346788) worksheet will be written to. It's useful for dev purposes.

Set it to `false`, or stop sending it in your request, to post to the main tabs.

### Add any string data to a result in sandbox sheet
Send a `POST` to `/extra_test_data` with the JSON payload (below) - returns a `200` and the requested JSON.

```
{  
  "sdk":"java",
  "extra_data":[  
    {  
      "test_name": "test7",
      "data": "any string"
    },
    {  
      "test_name": "test8",
      "data": "any string"
    }
  ]
}
```

### Send a `POST` to `/send_mail/sdks` with the JSON payload (below) - returns a `200`.

```
{  
  "sdk":"java",
  "id":"1234",
  "version":"RELEASE_CANDIDATE;Eyes.Appium@4.0.5;Eyes.Images@2.4.4",
  "changeLog":"### Fixed
                - Updated accessibility enums (experimental).",
  "testCoverageGap": "coverage gap",
  "frameworkVersions":"Appium 1.14.0;Selenium 4.1.1;Node 16.13.2",
  "specificRecipient":"optional_specific_mail@applitools.com"
}
```

<u>`specificRecipient` - String - optional<u>

By default the mail will be sent to a mail report group release.reports@applitools.com. To test this endpoint add a specific recipient and it will overwrite the default value.

### Send a `POST` to `/send_full_regression/sdks` with the JSON payload (below) - returns a `200`.

```
{
  "sdk": "java",
  "specificRecipient": "optional_specific_mail@applitools.com"
}
```

<u>`specificRecipient` - String - optional<u>

By default the mail will be sent to a mail report group release.reports@applitools.com. To test this endpoint add a specific recipient and it will overwrite the default value.

### Send a `POST` to `/tests_end/eyes` with the JSON payload (below) - returns a `200`.

```
{
  "id":"1234",
  "specificRecipient":"optional_specific_mail@applitools.com",
  "version":"10.9.88",
  "changeLog":"### Fixed
                - Updated accessibility enums (experimental)."
}
```

<u>`specificRecipient` - String - optional<u>

By default the mail will be sent to a mail report group release.reports@applitools.com. To test this endpoint add a specific recipient and it will overwrite the default value.

### Send a `Delete` to `/reset_eyes_report_data` - returns a `200`.

This endpoint allows Eyes report service to reset the state of the spreadsheet and the EndTaskCounter so Eyes will be able to start reporting from the beginning or rebuild with same ID.

### Send a `POST` to `/send_mail/no_tests` with the JSON payload (below) - returns a `200`.

```
{  
  "sdk":"storybook",
  "version":"3.27.3",
  "changeLog":"### Fixed
                - Updated accessibility enums (experimental).",
  "frameworkVersions":"Chrome 98.0.4758.80;Storybook 6.4.18;Node 16.13.2",
  "specificRecipient":"optional_specific_mail@applitools.com"
}
```

<u>`specificRecipient` - String - optional<u>

By default the mail will be sent to a mail report group release.reports@applitools.com. To test this endpoint add a specific recipient and it will overwrite the default value.
  
### Send a `GET` to `/get_sdk_results_by_id?id={id}&group={group}`.

<u>`id` - String<u>
  
The id of the result request sent in [result](#post-new-test-results-for-sdk) request
  
<u>`group` - String<u>
  
The group of the result request sent in [result](#post-new-test-results-for-sdk) request
  
##### Returns json object of test results
  
```
{  
  "sdk":"java",
  "group":"selenium",
  "id":"1234",
  "sandbox":true,
  "mandatory":false,
  "results":[  
    {  
      "test_name": "test7",
      "parameters":{
        "browser":"chrome",
        "stitching":"css"
      },
      "isSkipped":false,
      "passed":true
    },
    {  
      "test_name": "test7",
      "parameters":{
        "browser":"firefox",
        "stitching":"scroll"
      },
      "isSkipped":false,
      "passed":false
    }
  ]
}
```

### Post results for VG status page
Send a `POST` to `/vg_status` with the JSON payload (below) - returns a `200`.

```
{
  "status": [
    {
      "system": "Chrome",
      "status": true
    },
    {
      "system": "Firefox",
      "status": true
    }
  ]
}
```
