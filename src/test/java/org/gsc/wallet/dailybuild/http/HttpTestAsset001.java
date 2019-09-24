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
public class HttpTestAsset001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private JSONObject getAssetIssueByIdContent;
  private JSONObject getAssetIssueByNameContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpConfirmednode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);


  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] assetAddress = ecKey1.getAddress();
  String assetKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey2.getAddress();
  String participateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private static final long now = System.currentTimeMillis();
  private static String name = "testAssetIssue002_" + Long.toString(now);
  private static final long totalSupply = now;
  private static String assetIssueId;
  private static String updateDescription = "Description_update_" + Long.toString(now);
  private static String updateUrl = "Url_update_" + Long.toString(now);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create asset issue by http")
  public void test01CreateAssetIssue() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, participateAddress, 10000000L, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    //Create an asset issue
    response = HttpMethed.assetIssue(httpnode, assetAddress, name, name, totalSupply, 1, 1,
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
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById by http")
  public void test02GetAssetIssueById() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAssetIssueById(httpnode, assetIssueId);
    getAssetIssueByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueById from confirmed by http")
  public void test03GetAssetIssueByIdFromConfirmed() {
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);
    response = HttpMethed.getAssetIssueByIdFromConfirmed(httpConfirmednode, assetIssueId);
    getAssetIssueByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByIdContent);
    Assert.assertTrue(totalSupply == getAssetIssueByIdContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName by http")
  public void test04GetAssetIssueByName() {
    response = HttpMethed.getAssetIssueByName(httpnode, name);
    getAssetIssueByNameContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetAssetIssueByName from confirmed by http")
  public void test05GetAssetIssueByNameFromConfirmed() {
    response = HttpMethed.getAssetIssueByNameFromConfirmed(httpConfirmednode, name);
    getAssetIssueByNameContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByNameContent);
    Assert.assertTrue(totalSupply == getAssetIssueByNameContent.getLong("total_supply"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "TransferAsset by http")
  public void test06TransferAsset() {
    logger.info("Transfer asset.");
    response = HttpMethed.transferAsset(httpnode, assetAddress, participateAddress, assetIssueId,
        100L, assetKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, participateAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("assetV2").isEmpty());
    //logger.info(responseContent.get("assetV2").toString());

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Participate asset issue by http")
  public void test07ParticipateAssetIssue() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.participateAssetIssue(httpnode, assetAddress, participateAddress,
        assetIssueId, 1000L, participateKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, participateAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Update asset issue by http")
  public void test08UpdateAssetIssue() {
    response = HttpMethed.updateAssetIssue(httpnode, assetAddress, updateDescription, updateUrl,
        290L, 390L, assetKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAssetIssueById(httpnode, assetIssueId);
    getAssetIssueByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getAssetIssueByIdContent);

    Assert.assertTrue(getAssetIssueByIdContent
        .getLong("public_free_asset_net_limit") == 390L);
    Assert.assertTrue(getAssetIssueByIdContent
        .getLong("free_asset_net_limit") == 290L);
    Assert.assertTrue(getAssetIssueByIdContent
        .getString("description").equalsIgnoreCase(HttpMethed.str2hex(updateDescription)));
    Assert.assertTrue(getAssetIssueByIdContent
        .getString("url").equalsIgnoreCase(HttpMethed.str2hex(updateUrl)));
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list by http")
  public void test09GetAssetissueList() {

    response = HttpMethed.getAssetissueList(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get asset issue list from confirmed by http")
  public void test10GetAssetissueListFromConfirmed() {
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);
    response = HttpMethed.getAssetIssueListFromConfirmed(httpConfirmednode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() >= 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list by http")
  public void test11GetPaginatedAssetissueList() {
    response = HttpMethed.getPaginatedAssetissueList(httpnode, 0, 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
    Assert.assertTrue(jsonArray.size() == 1);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated asset issue list from confirmed by http")
  public void test12GetPaginatedAssetissueListFromConfirmed() {
    response = HttpMethed.getPaginatedAssetissueListFromConfirmed(httpConfirmednode, 0, 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("assetIssue"));
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
