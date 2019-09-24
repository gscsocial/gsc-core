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
public class MutiSignStress {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey001);


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[1];
  String accountPermissionJson = "";

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

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

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20)
  public void testMutiSignForAccount() {
    Integer i = 0;
    while (i < 20) {
      ecKey1 = new ECKey(Utils.getRandom());
      manager1Address = ecKey1.getAddress();
      manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

      ecKey2 = new ECKey(Utils.getRandom());
      manager2Address = ecKey2.getAddress();
      manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

      ecKey3 = new ECKey(Utils.getRandom());
      ownerAddress = ecKey3.getAddress();
      ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
      PublicMethed.printAddress(ownerKey);

      ecKey4 = new ECKey(Utils.getRandom());
      newAddress = ecKey4.getAddress();
      newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

      PublicMethed.sendcoin(ownerAddress, 4000000L, fromAddress, testKey002,
          blockingStubFull);
      PublicMethed.sendcoin(ownerAddress, 4000000L, fromAddress, testKey002,
          blockingStubFull);
      PublicMethed.sendcoin(ownerAddress, 4000000L, fromAddress, testKey002,
          blockingStubFull);
      permissionKeyString[0] = manager1Key;
      permissionKeyString[1] = manager2Key;
      ownerKeyString[0] = ownerKey;
      accountPermissionJson = "[{\"keys\":[{\"address\":\""
          + PublicMethed.getAddressString(ownerKey)
          + "\",\"weight\":2}],\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\"},"
          + "{\"parent\":\"owner\",\"keys\":[{\"address\":\""
          + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},{\"address\":\""
          + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}],\"name\":\"active\","
          + "\"threshold\":2}]";
      //logger.info(accountPermissionJson);
      PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
          blockingStubFull, ownerKeyString);

      String updateName = Long.toString(System.currentTimeMillis());

      PublicMethedForMutiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.sendcoin(newAddress, 1000000L, ownerAddress, ownerKey,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.freezeBalance(ownerAddress, 1000000L, 0,
          ownerKey, blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
      PublicMethedForMutiSign.unFreezeBalance(ownerAddress, ownerKey, 0, null,
          blockingStubFull, permissionKeyString);
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


