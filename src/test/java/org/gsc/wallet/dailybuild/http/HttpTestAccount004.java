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
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;

@Slf4j
public class HttpTestAccount004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpConfirmednode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] setAccountIdAddress = ecKey1.getAddress();
  String setAccountIdKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 10000000L;
  String accountId;


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Set account by http")
  public void test1setAccountId() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, setAccountIdAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);


    response = HttpMethed.setAccountId(httpnode,setAccountIdAddress,
        System.currentTimeMillis() + "id",false,setAccountIdKey);
    Assert.assertFalse(HttpMethed.verificationResult(response));


    //Set account id.
    accountId = System.currentTimeMillis() + "id";
    response = HttpMethed.setAccountId(httpnode,setAccountIdAddress,
        accountId,true,setAccountIdKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via http")
  public void test2getAccountId() {
    response = HttpMethed.getAccountById(httpnode,accountId,true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);

    response = HttpMethed.getAccountById(httpnode,accountId,false);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() <= 1);


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get account by id via http")
  public void test3getAccountIdFromConfirmed() {
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode,httpConfirmednode);
    response = HttpMethed.getAccountByIdFromConfirmed(httpConfirmednode,accountId,true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.get("account_id"), accountId);
    Assert.assertTrue(responseContent.size() >= 10);
  }



  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
