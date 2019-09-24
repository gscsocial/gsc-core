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

import com.alibaba.fastjson.JSONArray;
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
public class HttpTestSendCoin001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(1);
  private String httpConfirmednode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 1000L;
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "SendCoin by http")
  public void test1SendCoin() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, receiverAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    Assert.assertEquals(HttpMethed.getBalance(httpnode, receiverAddress), amount);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction by id from confirmed by http")
  public void test2GetTransactionByIdFromConfirmed() {
    String txid = HttpMethed.sendCoinGetTxid(httpnode, fromAddress, receiverAddress, amount,
        testKey002);
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);

    response = HttpMethed.getTransactionByIdFromConfirmed(httpConfirmednode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    String retString = responseContent.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(HttpMethed.parseStringContent(array.get(0).toString()).getString(
        "contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() > 4);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction info by id from confirmed by http")
  public void test3GetTransactionInfoByIdFromConfirmed() {
    String txid = HttpMethed.sendCoinGetTxid(httpnode, fromAddress, receiverAddress, amount,
        testKey002);
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);

    response = HttpMethed.getTransactionInfoByIdFromConfirmed(httpConfirmednode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() > 4);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transactions from this from confirmed by http")
  public void test4GetTransactionsFromThisFromConfirmed() {
    response = HttpMethed
        .getTransactionsFromThisFromConfirmed(httpConfirmednode, fromAddress, 0, 100);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONObject transactionObject = HttpMethed.parseStringContent(JSONArray.parseArray(
        responseContent.getString("transaction")).get(0).toString());
    String retString = transactionObject.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(HttpMethed.parseStringContent(array.get(0).toString())
        .getString("contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transactions to this from confirmed by http")
  public void test5GetTransactionsToThisFromConfirmed() {
    response = HttpMethed
        .getTransactionsFromThisFromConfirmed(httpConfirmednode, fromAddress, 0, 100);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONObject transactionObject = HttpMethed.parseStringContent(
        JSONArray.parseArray(responseContent.getString("transaction")).get(0).toString());
    String retString = transactionObject.getString("ret");
    JSONArray array = JSONArray.parseArray(retString);
    Assert.assertEquals(HttpMethed.parseStringContent(array.get(0).toString()).getString(
        "contractRet"), "SUCCESS");
    Assert.assertTrue(responseContent.size() == 1);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }

}

