/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.wallet.common.client.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class DailyBuildReport extends TestListenerAdapter {

  private Integer passedNum = 0;
  private Integer failedNum = 0;
  private Integer skippedNum = 0;
  private String reportPath;
  StringBuilder passedDescriptionList = new StringBuilder("");
  StringBuilder failedDescriptionList = new StringBuilder("");
  StringBuilder skippedDescriptionList = new StringBuilder("");

  @Override
  public void onStart(ITestContext context) {
    reportPath = "Daily_Build_Report";
    StringBuilder sb = new StringBuilder("3.Stest report:  ");
    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    passedDescriptionList.append(result.getMethod().getRealClass() + ": "
        + result.getMethod().getDescription() + "\n");
    passedNum++;
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    skippedDescriptionList.append(result.getMethod().getRealClass() + ": "
            + result.getMethod().getDescription() + "\n");
    skippedNum++;
  }

  @Override
  public void onTestFailure(ITestResult result) {
    failedDescriptionList.append(result.getMethod().getRealClass() + ": "
        + result.getMethod().getDescription() + "\n");
    failedNum++;
  }

  @Override
  public void onFinish(ITestContext testContext) {
    StringBuilder sb = new StringBuilder();
    sb.append("Total: " + (passedNum + failedNum + skippedNum) + ",  " + "Passed: " + passedNum
        + ",  " + "Failed: " + failedNum + ",  " + "Skipped: " + skippedNum + "\n");
    sb.append("------------------------------------------------------------------------------\n");
    sb.append("Passed list " + "\n");
    //sb.append("Passed case List: " + "\n");
    sb.append(passedDescriptionList.toString());
    sb.append("------------------------------------------------------------------------------\n");
    sb.append("Failed list: " + "\n");
    //sb.append("Failed case List: " + "\n");
    sb.append(failedDescriptionList.toString());
    sb.append("------------------------------------------------------------------------------\n");
    sb.append("Skipped list: " + "\n");
    //sb.append("Skipped case List: " + "\n");
    sb.append(skippedDescriptionList.toString());
    sb.append("----------------------------------------------------------------\n");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}

