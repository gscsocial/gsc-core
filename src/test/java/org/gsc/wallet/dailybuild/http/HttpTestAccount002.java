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
public class HttpTestAccount002 {

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
  byte[] freezeBalanceAddress = ecKey1.getAddress();
  String freezeBalanceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverResourceAddress = ecKey2.getAddress();
  String receiverResourceKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long berforeBalance;
  Long afterBalance;

  Long amount = 10000000L;
  Long frozenBalance = 2000000L;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for net by http")
  public void test01FreezebalanceForNet() {
    PublicMethed.printAddress(freezeBalanceKey);
    //Send Gsc to test account
    response = HttpMethed.sendCoin(httpnode, fromAddress, freezeBalanceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        0, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for net by http")
  public void test02UnFreezebalanceForNet() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance for net
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 0, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for cpu by http")
  public void test03FreezebalanceForCpu() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance for cpu
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        1, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for cpu by http")
  public void test04UnFreezebalanceForCpu() {

    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    HttpMethed.waitToProduceOneBlock(httpnode);
    //UnFreeze balance for cpu
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 1, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with net for others by http")
  public void test05FreezebalanceOfNetForOthers() {
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with net for others
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        0, receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource by http")
  public void test06GetDelegatedResource() {
    response = HttpMethed.getDelegatedResource(
        httpnode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("delegatedResource").toString());
    Assert.assertTrue(jsonArray.size() >= 1);
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("from"),
        ByteArray.toHexString(freezeBalanceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("to"),
        ByteArray.toHexString(receiverResourceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getLong("frozen_balance_for_net"),
        frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource from confirmed by http")
  public void test07GetDelegatedResourceFromConfirmed() {
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);
    HttpMethed.waitToProduceOneBlockFromConfirmed(httpnode, httpConfirmednode);
    response = HttpMethed.getDelegatedResourceFromConfirmed(
        httpConfirmednode, freezeBalanceAddress, receiverResourceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.get("delegatedResource").toString());
    Assert.assertTrue(jsonArray.size() >= 1);
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("from"),
        ByteArray.toHexString(freezeBalanceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getString("to"),
        ByteArray.toHexString(receiverResourceAddress));
    Assert.assertEquals(jsonArray.getJSONObject(0).getLong("frozen_balance_for_net"),
        frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index by http")
  public void test08GetDelegatedResourceAccountIndex() {
    response = HttpMethed.getDelegatedResourceAccountIndex(httpnode, freezeBalanceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Delegated Resource Account Index from confirmed by http")
  public void test09GetDelegatedResourceAccountIndexFromConfirmed() {
    response = HttpMethed.getDelegatedResourceAccountIndexFromConfirmed(httpConfirmednode,
        freezeBalanceAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertFalse(responseContent.get("toAccounts").toString().isEmpty());
    String toAddress = responseContent.getJSONArray("toAccounts").get(0).toString();
    Assert.assertEquals(toAddress, ByteArray.toHexString(receiverResourceAddress));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with net for others by http")
  public void test10UnFreezebalanceOfNetForOthers() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with net for others
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 0,
        receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with cpu for others by http")
  public void test11FreezebalanceOfCpuForOthers() {
    response = HttpMethed
        .sendCoin(httpnode, fromAddress, receiverResourceAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //Freeze balance with cpu for others
    response = HttpMethed.freezeBalance(httpnode, freezeBalanceAddress, frozenBalance, 0,
        1, receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with cpu for others by http")
  public void test12UnFreezebalanceOfCpuForOthers() {
    berforeBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);

    //UnFreeze balance with cpu for others
    response = HttpMethed.unFreezeBalance(httpnode, freezeBalanceAddress, 1,
        receiverResourceAddress, freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    afterBalance = HttpMethed.getBalance(httpnode, freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
