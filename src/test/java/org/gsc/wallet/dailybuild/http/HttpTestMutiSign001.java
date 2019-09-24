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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
public class HttpTestMutiSign001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey1.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 1000000000L;
  JsonArray keys = new JsonArray();
  JsonArray activeKeys = new JsonArray();
  JsonObject manager1Wight = new JsonObject();
  JsonObject manager2Wight = new JsonObject();
  JsonObject manager3Wight = new JsonObject();
  JsonObject manager4Wight = new JsonObject();
  JsonObject ownerObject = new JsonObject();
  JsonObject witnessObject = new JsonObject();
  JsonObject activeObject = new JsonObject();

  private final String manager1Key = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] manager1Address = PublicMethed.getFinalAddress(manager1Key);

  private final String manager2Key = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] manager2Address = PublicMethed.getFinalAddress(manager2Key);

  private final String manager3Key = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] manager3Address = PublicMethed.getFinalAddress(manager3Key);

  private final String manager4Key = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  private final byte[] manager4Address = PublicMethed.getFinalAddress(manager4Key);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Account Permission Up Date by http")
  public void test1AccountPermissionUpDate() {
    PublicMethed.printAddress(ownerKey);
    response = HttpMethed.sendCoin(httpnode, fromAddress, ownerAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    manager1Wight.addProperty("address", ByteArray.toHexString(manager1Address));
    manager1Wight.addProperty("weight", 1);

    logger.info(manager1Wight.toString());
    manager2Wight.addProperty("address", ByteArray.toHexString(manager2Address));
    manager2Wight.addProperty("weight", 1);

    logger.info(manager2Wight.toString());

    keys.add(manager1Wight);
    keys.add(manager2Wight);

    ownerObject.addProperty("type", 0);
    ownerObject.addProperty("permission_name", "owner");
    ownerObject.addProperty("threshold", 2);
    ownerObject.add("keys", keys);


    manager3Wight.addProperty("address", ByteArray.toHexString(manager3Address));
    manager3Wight.addProperty("weight", 1);

    logger.info(manager3Wight.toString());
    manager4Wight.addProperty("address", ByteArray.toHexString(manager4Address));
    manager4Wight.addProperty("weight", 1);

    logger.info(manager4Wight.toString());

    activeKeys.add(manager3Wight);
    activeKeys.add(manager4Wight);



    activeObject.addProperty("type", 2);
    activeObject.addProperty("permission_name", "active0");
    activeObject.addProperty("threshold", 2);
    activeObject.addProperty("operations",
        "7fff1fc0037e0000000000000000000000000000000000000000000000000000");
    activeObject.add("keys", activeKeys);

    response = HttpMethed.accountPermissionUpdate(httpnode, ownerAddress, ownerObject,
        witnessObject, activeObject, ownerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Add transaction sign by http with permission id")
  public void test2AddTransactionSign() {

    HttpMethed.waitToProduceOneBlock(httpnode);
    String[] permissionKeyString = new String[2];
    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;

    String[] permissionKeyActive = new String[2];
    permissionKeyActive[0] = manager3Key;
    permissionKeyActive[1] = manager4Key;

    response = HttpMethed.sendCoin(httpnode, ownerAddress, fromAddress, 10L, 0,permissionKeyString);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    response = HttpMethed.sendCoin(httpnode, ownerAddress, fromAddress, 10L, 2,permissionKeyString);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    logger.info("start permission id 2");
    response = HttpMethed.sendCoin(httpnode, ownerAddress, fromAddress, 12L, 2,permissionKeyActive);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    response = HttpMethed.sendCoin(httpnode, ownerAddress, fromAddress, 12L, 0,permissionKeyActive);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    response = HttpMethed.sendCoin(httpnode, ownerAddress, fromAddress, 11L, 1,permissionKeyActive);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    response = HttpMethed.sendCoin(httpnode, ownerAddress, fromAddress, 11L, 3,permissionKeyString);
    Assert.assertFalse(HttpMethed.verificationResult(response));


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
