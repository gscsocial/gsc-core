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
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue018 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private static final long now = System.currentTimeMillis();
  private static final String name = "Asset008_" + Long.toString(now);
  private static final String char32Name = "To_long_asset_name_" + Long.toString(now);
  private static final String char33Name = "To_long_asset_name_a" + Long.toString(now);
  private static final long totalSupply = now;
  String description = "just-test";
  String url = "https://github.com/gscsocial/wallet-cli/";

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] assetAccount1Address = ecKey1.getAddress();
  String assetAccount1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] assetAccount2Address = ecKey2.getAddress();
  String assetAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] assetAccount3Address = ecKey3.getAddress();
  String assetAccount3Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] assetAccount4Address = ecKey4.getAddress();
  String assetAccount4Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());


  ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] assetAccount5Address = ecKey5.getAddress();
  String assetAccount5Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());

  ECKey ecKey6 = new ECKey(Utils.getRandom());
  byte[] assetAccount6Address = ecKey6.getAddress();
  String assetAccount6Key = ByteArray.toHexString(ecKey6.getPrivKeyBytes());

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
    PublicMethed.printAddress(assetAccount1Key);
    PublicMethed.printAddress(assetAccount2Key);
    PublicMethed.printAddress(assetAccount3Key);
    PublicMethed.printAddress(assetAccount4Key);
    PublicMethed.printAddress(assetAccount5Key);
    PublicMethed.printAddress(assetAccount6Key);
  }

  @Test(enabled = true)
  public void test1AssetIssueNameBelow32Char() {

    ecKey4 = new ECKey(Utils.getRandom());
    assetAccount4Address = ecKey4.getAddress();
    assetAccount4Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    ecKey5 = new ECKey(Utils.getRandom());
    assetAccount5Address = ecKey5.getAddress();
    assetAccount5Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());

    ecKey6 = new ECKey(Utils.getRandom());
    assetAccount6Address = ecKey6.getAddress();
    assetAccount6Key = ByteArray.toHexString(ecKey6.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(assetAccount4Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount5Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount6Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));

    //Can create 32 char token name.
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount4Address,
        char32Name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, assetAccount4Key, blockingStubFull));

    //Can't create 33 char token name.
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertFalse(PublicMethed.createAssetIssue(assetAccount5Address,
        char33Name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, assetAccount5Key, blockingStubFull));

    //
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount6Address,
        char32Name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, assetAccount6Key, blockingStubFull));

  }

  @Test(enabled = true)
  public void test2SameAssetissueName() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    assetAccount1Address = ecKey1.getAddress();
    assetAccount1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    assetAccount2Address = ecKey2.getAddress();
    assetAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    assetAccount3Address = ecKey3.getAddress();
    assetAccount3Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

    logger.info(name);
    logger.info("total supply is " + Long.toString(totalSupply));
    //send coin to the new account
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount1Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount2Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(assetAccount3Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Create 3 the same name token.
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount1Address,
        name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, assetAccount1Key, blockingStubFull));
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount2Address,
        name, totalSupply, 2, 2, start, end, 2, description, url,
        3000L, 3000L, 2L, 2L, assetAccount2Key, blockingStubFull));
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetAccount3Address,
        name, totalSupply, 3, 3, start, end, 3, description, url,
        4000L, 4000L, 3L, 3L, assetAccount3Key, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Get asset issue by name
    String asset1Name = name;
    ByteString assetNameBs = ByteString.copyFrom(asset1Name.getBytes());

    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();

    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueListByName(request);
    Assert.assertTrue(assetIssueList.getAssetIssueCount() == 3);
    for (AssetIssueContract assetIssue : assetIssueList.getAssetIssueList()) {
      Assert.assertTrue(assetIssue.getTotalSupply() == totalSupply);

    }

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(assetAccount1Key, blockingStubFull);
    final ByteString assetAccount1Id = getAssetIdFromThisAccount.getAssetIssuedID();

    getAssetIdFromThisAccount = PublicMethed.queryAccount(assetAccount2Key, blockingStubFull);
    final ByteString assetAccount2Id = getAssetIdFromThisAccount.getAssetIssuedID();

    getAssetIdFromThisAccount = PublicMethed.queryAccount(assetAccount3Key, blockingStubFull);
    final ByteString assetAccount3Id = getAssetIdFromThisAccount.getAssetIssuedID();

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Transfer asset issue.
    Assert.assertTrue(PublicMethed.transferAsset(assetAccount2Address, assetAccount1Id
        .toByteArray(), 1L, assetAccount1Address, assetAccount1Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.transferAsset(assetAccount3Address, assetAccount2Id
        .toByteArray(), 2L, assetAccount2Address, assetAccount2Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.transferAsset(assetAccount1Address, assetAccount3Id
        .toByteArray(), 3L, assetAccount3Address, assetAccount3Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.transferAsset(assetAccount1Address, assetAccount2Id
        .toByteArray(), 3L, assetAccount3Address, assetAccount3Key, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Participate asset issue.
    Assert.assertTrue(PublicMethed.participateAssetIssue(assetAccount3Address, assetAccount3Id
        .toByteArray(), 1L, assetAccount2Address, assetAccount2Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.participateAssetIssue(assetAccount1Address, assetAccount1Id
        .toByteArray(), 2L, assetAccount3Address, assetAccount3Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.participateAssetIssue(assetAccount2Address, assetAccount2Id
        .toByteArray(), 3L, assetAccount1Address, assetAccount1Key, blockingStubFull));

    Assert.assertFalse(PublicMethed.participateAssetIssue(assetAccount2Address, assetAccount3Id
        .toByteArray(), 3L, assetAccount1Address, assetAccount1Key, blockingStubFull));


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