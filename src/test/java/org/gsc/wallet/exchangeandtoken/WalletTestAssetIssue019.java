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

package org.gsc.wallet.exchangeandtoken;

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
import org.gsc.protos.Protocol.Account;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue019 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  String description = "just-test";
  String url = "https://github.com/gscsocial/wallet-cli/";
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset019Address = ecKey1.getAddress();
  String asset019Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] asset019SecondAddress = ecKey2.getAddress();
  String asset019SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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

  @Test(enabled = true)
  public void testCanNotCreateTokenNameByGsc() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset019Address = ecKey1.getAddress();
    asset019Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(asset019Key);

    ecKey2 = new ECKey(Utils.getRandom());
    asset019SecondAddress = ecKey2.getAddress();
    asset019SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(asset019SecondKey);

    Assert.assertTrue(PublicMethed.sendcoin(asset019Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(asset019SecondAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));

    //Can create 32 char token name.
    Long start = System.currentTimeMillis() + 20000000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "Gsc", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "Gsc", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "Gsc", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "Gsc", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "Gsc", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "GSC", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "GsC", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.createAssetIssue(asset019Address,
        "GSc", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.createAssetIssue(asset019Address,
        "Gsc", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.createAssetIssue(asset019SecondAddress,
        "_", totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset019SecondKey, blockingStubFull));
  }

  @Test(enabled = true)
  public void testGetAssetLastOperationTimeAndAssetIssueFreeNetUsed() {
    Assert.assertTrue(PublicMethed.freezeBalance(asset019Address, 100000000L, 5,
        asset019Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(asset019SecondAddress, 100000000L, 5,
        asset019SecondKey, blockingStubFull));
    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset019Address, blockingStubFull);
    ByteString asset019AccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset019SecondAddress, blockingStubFull);
    ByteString asset019SecondAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    PublicMethed.transferAsset(asset019SecondAddress, asset019AccountId.toByteArray(), 100L,
        asset019Address, asset019Key, blockingStubFull);
    PublicMethed.transferAsset(asset019Address, asset019SecondAccountId.toByteArray(), 100L,
        asset019SecondAddress, asset019SecondKey, blockingStubFull);

    PublicMethed.transferAsset(asset019Address, asset019AccountId.toByteArray(), 10L,
        asset019SecondAddress, asset019SecondKey, blockingStubFull);
    PublicMethed.transferAsset(asset019SecondAddress, asset019SecondAccountId.toByteArray(),
        10L, asset019Address, asset019Key, blockingStubFull);

    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset019Address, blockingStubFull);
    for (String id : getAssetIdFromThisAccount.getFreeAssetNetUsageV2Map().keySet()) {
      if (asset019SecondAccountId.toStringUtf8().equalsIgnoreCase(id)) {
        Assert.assertTrue(getAssetIdFromThisAccount.getFreeAssetNetUsageV2Map().get(id) > 0);
      }
    }

    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset019SecondAddress, blockingStubFull);
    for (String id : getAssetIdFromThisAccount.getLatestAssetOperationTimeV2Map().keySet()) {
      if (asset019AccountId.toStringUtf8().equalsIgnoreCase(id)) {
        Assert.assertTrue(getAssetIdFromThisAccount.getLatestAssetOperationTimeV2Map().get(id) > 0);
      }
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