package com.yarden.restServiceDemo;

import com.yarden.restServiceDemo.reportService.SheetData;
import com.yarden.restServiceDemo.reportService.SheetTabIdentifier;
import org.junit.Test;

import java.io.IOException;

public class TestClass {

    @Test
    public void test() throws IOException {
        SheetData rawDataSheetData = new SheetData(new SheetTabIdentifier(Enums.SpreadsheetIDs.SDK.value, Enums.SdkGroupsSheetTabNames.Selenium.value));
        String res = rawDataSheetData.getSheetDataAsCsvString();
        System.out.println(res);
    }

}
