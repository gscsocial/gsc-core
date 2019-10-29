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

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class Retry implements IRetryAnalyzer {

  private int retryCount = 0;
  private int maxRetryCount = 2;

  /**
   * constructor.
   */

  public boolean retry(ITestResult result) {
    if (retryCount < maxRetryCount) {
      System.out.println("Retrying test " + result.getName() + " with status "
          + getResultStatusName(result.getStatus()) + " for the " + (retryCount + 1) + " time(s).");
      retryCount++;
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return true;
    }
    return false;
  }

  /**
   * constructor.
   */

  public String getResultStatusName(int status) {
    String resultName = null;
    if (status == 1) {
      resultName = "SUCCESS";
    }
    if (status == 2) {
      resultName = "FAILURE";
    }
    if (status == 3) {
      resultName = "SKIP";
    }
    return resultName;
  }
}