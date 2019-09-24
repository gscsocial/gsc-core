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

package org.gsc.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.utils.HttpMethed;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

@Slf4j
public class HttpTestAccount001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpConfirmednode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by http")
  public void getAccount() {
    response = HttpMethed.getAccount(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account from confirmed by http")
  public void getAccountFromConfirmed() {
    response = HttpMethed.getAccountFromConfirmed(httpConfirmednode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get accountNet by http")
  public void getAccountNet() {
    response = HttpMethed.getAccountNet(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(Integer.parseInt(responseContent.get("freeNetLimit").toString()), 4000);
    Assert.assertEquals(
        Long.parseLong(responseContent.get("TotalNetLimit").toString()), 51840000000L);
    Assert.assertTrue(responseContent.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get accountResource by http")
  public void getAccountResource() {
    response = HttpMethed.getAccountReource(httpnode, fromAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(
        Long.parseLong(responseContent.get("TotalCpuLimit").toString()) >= 50000000000L);
    Assert.assertTrue(responseContent.size() >= 3);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
