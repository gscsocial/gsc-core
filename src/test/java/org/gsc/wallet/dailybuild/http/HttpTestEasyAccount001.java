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
import org.gsc.core.Wallet;

@Slf4j
public class HttpTestEasyAccount001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private long now = System.currentTimeMillis();
  private String userPassword = "ps_" + Long.toString(now);
  private String assetName = "httpEasyAsset_" + Long.toString(now);
  private final long totalSupply = now;
  private static String assetIssueId;

  private String easyAddress = null;

  private String generatePriKey = null;
  private String generateAddress = null;
  private String generateHexAddress = null;

  long beforeEasyBalance = 0L;
  long afterEasyBalance = 0L;

  long beforeGenerateBalance = 0L;
  long afterGenerateBalance = 0L;

  long beforeEasyAsset = 0L;
  long afterEasyAsset = 0L;

  long beforeGenerateAsset = 0L;
  long afterGenerateAsset = 0L;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] assetAddress = ecKey1.getAddress();
  String assetKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create address by http")
  public void test01CreateAddress() {
    logger.info(userPassword);
    response = HttpMethed.createAddress(httpnode, userPassword);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    easyAddress = responseContent.get("base58checkAddress").toString();

    //Send Gsc to easy account
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, Wallet.decodeFromBase58Check(easyAddress), 5000000L,
            testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    beforeEasyBalance = HttpMethed.getBalance(httpnode, Wallet.decodeFromBase58Check(easyAddress));
    logger.info("beforeEasyBalance: " + beforeEasyBalance);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Generate address by http")
  public void test02GenerateAddress() {
    response = HttpMethed.generateAddress(httpnode);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    generateAddress = responseContent.get("address").toString();
    generateHexAddress = responseContent.get("hexAddress").toString();
    generatePriKey = responseContent.get("privateKey").toString();

    //Send Gsc to easy account
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, Wallet.decodeFromBase58Check(generateAddress), 5000000L,
            testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    beforeGenerateBalance = HttpMethed
        .getBalance(httpnode, Wallet.decodeFromBase58Check(generateAddress));
    logger.info("beforeGenerateBalance: " + beforeGenerateBalance);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Validate address by http")
  public void test03ValideteAddress() {
    // Base58check format
    response = HttpMethed.validateAddress(httpnode, generateAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    // Hex string format
    response = HttpMethed.validateAddress(httpnode, generateHexAddress);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Easy transfer by http")
  public void test04EasyTransfer() {
    response = HttpMethed
        .easyTransfer(httpnode, userPassword, Wallet.decodeFromBase58Check(generateAddress),
            1000000L);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    HttpMethed.waitToProduceOneBlock(httpnode);

    //Send Gsc to easy account
    afterEasyBalance = HttpMethed.getBalance(httpnode, Wallet.decodeFromBase58Check(easyAddress));
    logger.info("afterEasyBalance: " + afterEasyBalance);

    afterGenerateBalance = HttpMethed
        .getBalance(httpnode, Wallet.decodeFromBase58Check(generateAddress));
    logger.info("afterGenerateBalance: " + afterGenerateBalance);

    Assert.assertEquals(beforeEasyBalance - afterEasyBalance,
        afterGenerateBalance - beforeGenerateBalance);
    beforeEasyBalance = afterEasyBalance;
    beforeGenerateBalance = afterGenerateBalance;
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Easy transfer by privateKey by http")
  public void test05EasyTransferByPrivateKey() {
    response = HttpMethed
        .easyTransferByPrivate(httpnode, generatePriKey,
            Wallet.decodeFromBase58Check(easyAddress),
            1000000L);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    HttpMethed.waitToProduceOneBlock(httpnode);

    //Send Gsc to easy account
    afterEasyBalance = HttpMethed.getBalance(httpnode, Wallet.decodeFromBase58Check(easyAddress));
    logger.info("beforeEasyBalance: " + beforeEasyBalance);
    logger.info("afterEasyBalance: " + afterEasyBalance);

    afterGenerateBalance = HttpMethed
        .getBalance(httpnode, Wallet.decodeFromBase58Check(generateAddress));
    logger.info("beforeGenerateBalance: " + beforeGenerateBalance);
    logger.info("afterGenerateBalance: " + afterGenerateBalance);

    Assert.assertEquals(beforeGenerateBalance - afterGenerateBalance,
        afterEasyBalance - beforeEasyBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create asset issue by http")
  public void test06CreateAssetIssue() {
    Long amount = 2048000000L;
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    //Create an asset issue
    response = HttpMethed
        .assetIssue(httpnode, assetAddress, assetName, assetName, totalSupply, 1, 1,
            System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
            2, 3, description, url, 1000L, 1000L, assetKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, assetAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    assetIssueId = responseContent.getString("asset_issued_ID");
    logger.info(assetIssueId);
    Assert.assertTrue(Integer.parseInt(assetIssueId) > 1000000);

    response = HttpMethed
        .transferAsset(httpnode, assetAddress,
            Wallet.decodeFromBase58Check(easyAddress), assetIssueId, 100L, assetKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, Wallet.decodeFromBase58Check(easyAddress));
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    beforeEasyAsset = responseContent.getJSONArray("assetV2").getJSONObject(0)
        .getLongValue("value");
    logger.info("beforeEasyAsset:" + beforeEasyAsset);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Easy transfer asset by http")
  public void test07EasyTransferAsset() {
    response = HttpMethed
        .easyTransferAsset(httpnode, userPassword, Wallet.decodeFromBase58Check(generateAddress),
            10L, assetIssueId);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, Wallet.decodeFromBase58Check(easyAddress));
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    afterEasyAsset = responseContent.getJSONArray("assetV2").getJSONObject(0)
        .getLongValue("value");
    logger.info("afterEasyAsset:" + afterEasyAsset);

    response = HttpMethed.getAccount(httpnode, Wallet.decodeFromBase58Check(generateAddress));
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    beforeGenerateAsset = responseContent.getJSONArray("assetV2").getJSONObject(0)
        .getLongValue("value");
    logger.info("beforeGenerateAsset:" + beforeGenerateAsset);
    Assert.assertEquals(beforeEasyAsset - afterEasyAsset, beforeGenerateAsset);
    beforeEasyAsset = afterEasyAsset;
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Easy transfer asset by private key by http")
  public void test08EasyTransferAssetByPrivateKey() {
    response = HttpMethed
        .easyTransferAssetByPrivate(httpnode, generatePriKey,
            Wallet.decodeFromBase58Check(easyAddress),
            5L, assetIssueId);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, Wallet.decodeFromBase58Check(easyAddress));
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    afterEasyAsset = responseContent.getJSONArray("assetV2").getJSONObject(0)
        .getLongValue("value");
    logger.info("beforeEasyAsset:" + beforeEasyAsset);
    logger.info("afterEasyAsset:" + afterEasyAsset);

    response = HttpMethed.getAccount(httpnode, Wallet.decodeFromBase58Check(generateAddress));
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    afterGenerateAsset = responseContent.getJSONArray("assetV2").getJSONObject(0)
        .getLongValue("value");
    logger.info("afterGenerateAsset:" + afterGenerateAsset);
    Assert
        .assertEquals(beforeGenerateAsset - afterGenerateAsset, afterEasyAsset - beforeEasyAsset);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }

}
