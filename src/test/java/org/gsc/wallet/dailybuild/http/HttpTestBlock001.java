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
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

@Slf4j
public class HttpTestBlock001 {

  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpConfirmednode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private Integer currentBlockNum;
  private JSONObject blockContent;
  private String blockId;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block by http")
  public void get01NowBlock() {
    response = HttpMethed.getNowBlock(httpnode);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    blockContent = responseContent;
    blockId = responseContent.get("blockID").toString();
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertFalse(responseContent.get("witness_signature").toString().isEmpty());
    HttpMethed.printJsonContent(responseContent);
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.get("number").toString()) > 0);
    currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Assert.assertTrue(Long.parseLong(responseContent.get("timestamp").toString()) > 1550724114000L);
    Assert.assertFalse(responseContent.get("witness_address").toString().isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from confirmed by http")
  public void get02NowBlockFromConfirmed() {
    response = HttpMethed.getNowBlockFromConfirmed(httpConfirmednode);
    logger.info("code is " + response.getStatusLine().getStatusCode());
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    blockContent = responseContent;
    blockId = responseContent.get("blockID").toString();
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() >= 2);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    Assert.assertTrue(responseContent.size() >= 2);
    Assert.assertFalse(responseContent.get("witness_signature").toString().isEmpty());
    HttpMethed.printJsonContent(responseContent);
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(Integer.parseInt(responseContent.get("number").toString()) > 0);
    currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Assert.assertTrue(Long.parseLong(responseContent.get("timestamp").toString()) > 1550724114000L);
    Assert.assertFalse(responseContent.get("witness_address").toString().isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by num by http")
  public void get03BlockByNum() {
    response = HttpMethed.getBlockByNum(httpnode, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(responseContent, blockContent);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get block by num from confirmed by http")
  public void get04BlockByNumFromConfirmed() {
    response = HttpMethed.getBlockByNumFromConfirmed(httpConfirmednode, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(responseContent, blockContent);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLimitNext by http")
  public void get05BlockByLimitNext() {
    response = HttpMethed.getBlockByLimitNext(httpnode, currentBlockNum - 10, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 10);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLastNum by http")
  public void get06BlockByLastNum() {
    response = HttpMethed.getBlockByLastNum(httpnode, 8);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 8);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockById by http")
  public void get07BlockById() {
    response = HttpMethed.getBlockById(httpnode, blockId);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(blockId, responseContent.get("blockID").toString());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockById by http")
  public void get08BlockByIdFromConfirmed() {
    response = HttpMethed.getBlockByIdFromConfirmed(httpConfirmednode, blockId);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(blockId, responseContent.get("blockID").toString());
  }



  /**
   * constructor.
   */
  @Test(enabled = true, description = "List nodes by http")
  public void get09ListNodes() {
    response = HttpMethed.listNodes(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "get next maintenance time by http")
  public void get10NextMaintaenanceTime() {
    response = HttpMethed.getNextmaintenanceTime(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("num").toString().isEmpty());
    Assert.assertTrue(responseContent.getLong("num") >= System.currentTimeMillis());
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "get chain parameter by http")
  public void get11ChainParameter() {
    response = HttpMethed.getChainParameter(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("chainParameter").toString());
    Assert.assertTrue(jsonArray.size() >= 26);
    Boolean exsistDelegated = false;
    for (int i = 0; i < jsonArray.size(); i++) {
      if (jsonArray.getJSONObject(i).getString("key").equals("getAllowDelegateResource")) {
        exsistDelegated = true;
        Assert.assertTrue(jsonArray.getJSONObject(i).getString("value").equals("1"));
      }
    }
    Assert.assertTrue(exsistDelegated);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "get Node Info by http")
  public void get12NodeInfo() {
    response = HttpMethed.getNodeInfo(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("configNodeInfo").toString().isEmpty());
    Assert.assertTrue(responseContent.getString("configNodeInfo").contains("\"dbVersion\":2"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get transaction count by blocknum from confirmed by http")
  public void get13TransactionCountByBlocknumFromConfirmed() {
    response = HttpMethed.getTransactionCountByBlocknumFromConfirmed(httpConfirmednode,
        currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() == 1);
    Assert.assertTrue(Integer.parseInt(responseContent.get("count").toString()) >= 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLimitNext by http")
  public void get14BlockByLimitNextFromConfirmed() {
    response = HttpMethed.getBlockByLimitNextFromConfirmed(httpConfirmednode,
        currentBlockNum - 10, currentBlockNum);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 10);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByLastNum by http")
  public void get15BlockByLastNumFromConfirmed() {
    response = HttpMethed.getBlockByLastNumFromConfirmed(httpConfirmednode, 8);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    logger.info(responseContent.get("block").toString());
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("block").toString());
    Assert.assertEquals(jsonArray.size(), 8);
  }




  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
