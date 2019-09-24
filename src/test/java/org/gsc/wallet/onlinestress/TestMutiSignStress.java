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

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class TestMutiSignStress {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey001);


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  ByteString assetAccountId1;
  String[] ownerKeyString = new String[1];
  String accountPermissionJson = "";

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, threadPoolSize = 30, invocationCount = 30)
  public void testMutiSignForAccount() {
    PublicMethed.printAddress(testKey002);

    ECKey ecKey4 = new ECKey(Utils.getRandom());
    byte[] newAddress = ecKey4.getAddress();
    String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey3.getAddress();

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, 9968981537400L, fromAddress, testKey002,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] manager1Address = ecKey1.getAddress();
    String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    String[] permissionKeyString = new String[3];
    permissionKeyString[0] = manager1Key;
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] manager2Address = ecKey2.getAddress();
    String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    permissionKeyString[1] = manager2Key;
    String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    permissionKeyString[2] = ownerKey;
    String[] ownerKeyString = new String[1];
    ownerKeyString[0] = ownerKey;

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":3,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);

    //permissionKeyString[0] = ownerKey;

    String[] ownerKeyString1 = new String[3];
    ownerKeyString1[0] = ownerKey;
    ownerKeyString1[1] = manager1Key;
    ownerKeyString1[2] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Integer i = 0;
    while (i++ <= 1000) {
      ecKey4 = new ECKey(Utils.getRandom());
      newAddress = ecKey4.getAddress();
      newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
      PublicMethed.printAddress(newKey);

      PublicMethedForMutiSign.sendcoin(
          newAddress, 4000000L, ownerAddress, ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethedForMutiSign.freezeBalance(
          ownerAddress, 1000000L, 0, ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethedForMutiSign.freezeBalanceGetCpu(
          ownerAddress, 1000000L, 0, 1, ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethedForMutiSign.freezeBalanceForReceiver(
          ownerAddress, 1000000L, 0, 0, ByteString.copyFrom(newAddress),
          ownerKey, blockingStubFull, ownerKeyString1);
      PublicMethedForMutiSign.unFreezeBalance(
          ownerAddress, ownerKey, 0, null, blockingStubFull, ownerKeyString1);
      PublicMethedForMutiSign.unFreezeBalance(
          ownerAddress, ownerKey, 0, newAddress, blockingStubFull, ownerKeyString1);
      PublicMethedForMutiSign.updateAccount(
          ownerAddress, Long.toString(System.currentTimeMillis()).getBytes(), ownerKey,
          blockingStubFull, ownerKeyString1);
    }


  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


