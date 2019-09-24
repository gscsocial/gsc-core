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

package org.gsc.wallet.onlinestress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.gsc.wallet.common.client.Configuration;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.wallet.common.client.utils.HttpMethed;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpStressTest {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] exchangeOwnerAddress = ecKey1.getAddress();
  String exchangeOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] asset2Address = ecKey2.getAddress();
  String asset2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private static final long now = System.currentTimeMillis();
  private static String name = "testAssetIssue002_" + Long.toString(now);
  private static final long totalSupply = 10000000000000000L;
  private static String assetIssueId1;
  private static String assetIssueId2;
  private static Integer exchangeId;
  private static Long beforeInjectBalance;
  static Integer connectionTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpConnectionTimeout");
  static Integer soTimeout = Configuration.getByPath("testng.conf")
      .getInt("defaultParameter.httpSoTimeout");


  /**
   * constructor.
   */
  @Test(enabled = true, threadPoolSize = 10, invocationCount = 10)
  public void test4InjectExchange() {
    final long now = System.currentTimeMillis();
    final long totalSupply = 10000000000000000L;
    Long beforeInjectBalance;
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httppost;
    httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
        connectionTimeout);
    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
    httppost = new HttpPost(url);
    httppost.setHeader("Content-type", "application/json; charset=utf-8");
    httppost.setHeader("Connection", "Close");

    response = HttpMethed
        .sendCoin(httpnode, fromAddress, exchangeOwnerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.sendCoin(httpnode, fromAddress, asset2Address, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    //Create an asset issue
    response = HttpMethed.assetIssue(httpnode, exchangeOwnerAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
        2, 3, description, url, 1000L, 1000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed.assetIssue(httpnode, asset2Address, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
        2, 3, description, url, 1000L, 1000L, asset2Key);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, exchangeOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    assetIssueId1 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId1) > 1000000);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, asset2Address);
    responseContent = HttpMethed.parseResponseContent(response);
    assetIssueId2 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId2) > 1000000);

    response = HttpMethed
        .transferAsset(httpnode, asset2Address, exchangeOwnerAddress, assetIssueId2,
            100000000000000L, asset2Key);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);

    //Create exchange.
    response = HttpMethed.exchangeCreate(httpnode, exchangeOwnerAddress, assetIssueId1,
        50000000000000L, assetIssueId2, 50000000000000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.listExchanges(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() >= 1);
    exchangeId = jsonArray.size();

    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    Integer times = 0;
    while (times++ <= 10000) {
      HttpMethed.sendCoin(httpnode, fromAddress, exchangeOwnerAddress, 100L, testKey002);
      HttpMethed.sendCoin(httpnode, fromAddress, asset2Address, 100L, testKey002);
      //Inject exchange.
      HttpMethed.exchangeInject(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
          10L, exchangeOwnerKey);
      HttpMethed.exchangeWithdraw(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
          10L, exchangeOwnerKey);
      HttpMethed.exchangeTransaction(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
          100L, 1L, exchangeOwnerKey);
      HttpMethed.exchangeTransaction(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId2,
          100L, 1L, exchangeOwnerKey);
      HttpMethed.transferAsset(httpnode, asset2Address, exchangeOwnerAddress, assetIssueId2,
          1L, asset2Key);
      HttpMethed.transferAsset(httpnode, exchangeOwnerAddress, asset2Address, assetIssueId1,
          1L, exchangeOwnerKey);
      HttpMethed.participateAssetIssue(httpnode, exchangeOwnerAddress, asset2Address,
          assetIssueId1, 1L, asset2Key);
      HttpMethed.participateAssetIssue(httpnode, asset2Address, exchangeOwnerAddress,
          assetIssueId2, 1L, exchangeOwnerKey);
      HttpMethed.freezeBalance(httpnode, fromAddress, 10000000000L, 0, 0,
          exchangeOwnerAddress, testKey002);
      HttpMethed.freezeBalance(httpnode, fromAddress, 10000000000L, 0, 1,
          exchangeOwnerAddress, testKey002);
      HttpMethed.unFreezeBalance(httpnode, fromAddress, 0, exchangeOwnerAddress, testKey002);
      HttpMethed.unFreezeBalance(httpnode, fromAddress, 1, exchangeOwnerAddress, testKey002);
    }
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
