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
public class HttpTestExchange001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpConfirmednode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);

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
  private static final long totalSupply = now;
  private static String assetIssueId1;
  private static String assetIssueId2;
  private static Integer exchangeId;
  private static Long beforeInjectBalance;
  private static Long afterInjectBalance;
  private static Long beforeWithdrawBalance;
  private static Long afterWithdrawBalance;
  private static Long beforeTransactionBalance;
  private static Long afterTransactionBalance;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create asset issue by http")
  public void test01CreateExchange() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, exchangeOwnerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.sendCoin(httpnode, fromAddress, asset2Address, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //Create an asset issue
    response = HttpMethed.assetIssue(httpnode, exchangeOwnerAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
        2, 3, description, url, 1000L, 1000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.assetIssue(httpnode, asset2Address, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000,
        2, 3, description, url, 1000L, 1000L, asset2Key);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, exchangeOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    assetIssueId1 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId1) > 1000000);

    response = HttpMethed.getAccount(httpnode, asset2Address);
    responseContent = HttpMethed.parseResponseContent(response);
    assetIssueId2 = responseContent.getString("asset_issued_ID");
    Assert.assertTrue(Integer.parseInt(assetIssueId2) > 1000000);

    response = HttpMethed
        .transferAsset(httpnode, asset2Address, exchangeOwnerAddress, assetIssueId2,
            10000000000L, asset2Key);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //Create exchange.
    response = HttpMethed.exchangeCreate(httpnode, exchangeOwnerAddress, assetIssueId1,
        1000000L, assetIssueId2, 1000000L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "List exchanges by http")
  public void test02ListExchange() {
    response = HttpMethed.listExchanges(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() >= 1);
    exchangeId = jsonArray.size();
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "List exchanges from confirmed by http")
  public void test03ListExchangeFromConfirmed() {
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);
    response = HttpMethed.listExchangesFromConfirmed(httpConfirmednode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() >= 1);
    exchangeId = jsonArray.size();
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetExchangeById by http")
  public void test04GetExchangeById() {
    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getInteger("exchange_id") == exchangeId);
    Assert.assertEquals(responseContent.getString("creator_address"),
        ByteArray.toHexString(exchangeOwnerAddress));
    beforeInjectBalance = responseContent.getLong("first_token_balance");

    logger.info("beforeInjectBalance" + beforeInjectBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetExchangeById from confirmed by http")
  public void test05GetExchangeByIdFromConfirmed() {
    response = HttpMethed.getExchangeByIdFromConfirmed(httpConfirmednode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getInteger("exchange_id") == exchangeId);
    Assert.assertEquals(responseContent.getString("creator_address"),
        ByteArray.toHexString(exchangeOwnerAddress));
    beforeInjectBalance = responseContent.getLong("first_token_balance");

    logger.info("beforeInjectBalance" + beforeInjectBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Inject exchange by http")
  public void test06InjectExchange() {
    //Inject exchange.
    response = HttpMethed.exchangeInject(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
        300L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    afterInjectBalance = responseContent.getLong("first_token_balance");
    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info("afterInjectBalance" + afterInjectBalance);
    Assert.assertTrue(afterInjectBalance - beforeInjectBalance == 300L);
    beforeWithdrawBalance = afterInjectBalance;
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Withdraw exchange by http")
  public void test07WithdrawExchange() {
    //Withdraw exchange.
    response = HttpMethed
        .exchangeWithdraw(httpnode, exchangeOwnerAddress, exchangeId, assetIssueId1,
            170L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    afterWithdrawBalance = responseContent.getLong("first_token_balance");
    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(beforeWithdrawBalance - afterWithdrawBalance == 170L);
    beforeTransactionBalance = afterWithdrawBalance;
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Transaction exchange by http")
  public void test08TransactionExchange() {
    //Transaction exchange.
    response = HttpMethed.exchangeTransaction(httpnode, exchangeOwnerAddress, exchangeId,
        assetIssueId1, 100L, 1L, exchangeOwnerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    afterTransactionBalance = responseContent.getLong("first_token_balance");
    response = HttpMethed.getExchangeById(httpnode, exchangeId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(afterTransactionBalance - beforeTransactionBalance >= 1);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get asset issue list by name by http")
  public void test09GetAssetIssueListByName() {
    response = HttpMethed.getAssetIssueListByName(httpnode, name);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("assetIssue").toString());
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get asset issue list by name from confirmed by http")
  public void test10GetAssetIssueListByNameFromConfirmed() {
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);
    response = HttpMethed.getAssetIssueListByNameFromConfirmed(httpConfirmednode, name);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("assetIssue").toString());
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated exchange list by http")
  public void test11GetPaginatedExchangeList() {

    response = HttpMethed.getPaginatedExchangeList(httpnode, 0, 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("exchanges"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
