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

import static org.hamcrest.core.StringContains.containsString;

import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
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
public class HttpTestClearAbiContract001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey2.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String contractAddress;
  String abi;
  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private static final long now = System.currentTimeMillis();
  private static String name = "testAssetIssue002_" + Long.toString(now);
  private static final long totalSupply = now;
  private static String assetIssueId;
  private static String contractName;


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Deploy smart contract by http")
  public void test1DeployContract() {
    PublicMethed.printAddress(assetOwnerKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.sendCoin(httpnode, fromAddress, assetOwnerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, assetOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    String filePath = "src/test/resources/soliditycode_v0.5.4/TriggerConstant003.sol";
    contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    abi = retMap.get("abi").toString();
    logger.info("abi:" + abi);
    logger.info("code:" + code);

    String txid = HttpMethed.deployContractGetTxid(httpnode, contractName, abi, code, 1000000L,
        1000000000L, 100, 11111111111111L,
        0L, 0, 0L, assetOwnerAddress, assetOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txid);
    response = HttpMethed.getTransactionById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("contract_address").isEmpty());
    contractAddress = responseContent.getString("contract_address");

    response = HttpMethed.getTransactionInfoById(httpnode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    String receiptString = responseContent.getString("receipt");
    Assert
        .assertEquals(HttpMethed.parseStringContent(receiptString).getString("result"), "SUCCESS");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get contract by http")
  public void test2GetContract() {
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "100");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert
        .assertThat(responseContent.getString("abi"),
            containsString("testView"));

    Assert.assertEquals(responseContent.getString("origin_cpu_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger contract by http")
  public void test3TriggerConstantContract() {

    HttpResponse httpResponse = HttpMethed
        .triggerConstantContract(httpnode, assetOwnerAddress, contractAddress,
            "testView()",
            "");

    responseContent = HttpMethed.parseResponseContent(httpResponse);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("result"), "{\"result\":true}");
    Assert.assertEquals(responseContent.getString("constant_result"),
        "[\"0000000000000000000000000000000000000000000000000000000000000001\"]");
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Trigger contract by http")
  public void test4ClearAbiContract() {

    HttpResponse httpResponse = HttpMethed
        .clearABiGetTxid(httpnode, assetOwnerAddress, contractAddress, assetOwnerKey);

    responseContent = HttpMethed.parseResponseContent(httpResponse);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("result"), "true");

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get contract by http")
  public void test5GetContract() {
    response = HttpMethed.getContract(httpnode, contractAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("consume_user_resource_percent"), "100");
    Assert.assertEquals(responseContent.getString("contract_address"), contractAddress);
    Assert.assertEquals(responseContent.getString("origin_address"),
        ByteArray.toHexString(assetOwnerAddress));
    Assert.assertEquals(responseContent.getString("abi"), "{}");
    Assert.assertEquals(responseContent.getString("origin_cpu_limit"), "11111111111111");
    Assert.assertEquals(responseContent.getString("name"), contractName);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
