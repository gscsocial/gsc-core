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
import org.gsc.wallet.common.client.utils.PublicMethed;
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
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.Account;

@Slf4j
public class WalletTestAssetIssue020 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private static final long now = System.currentTimeMillis();
  private static final String name = "Assetissue020_" + Long.toString(now);
  private static final String char33Name = "To_long_asset_name_a" + Long.toString(now);
  private static final long totalSupply = now;
  String description = "just-test";
  String url = "https://github.com/gscsocial/wallet-cli/";
  Account assetIssue020Account;
  ByteString assetAccountId;


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset020Address = ecKey1.getAddress();
  String asset020Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] asset020SecondAddress = ecKey2.getAddress();
  String asset020SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
  public void testAssetIssueSupportPrecision() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset020Address = ecKey1.getAddress();
    asset020Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(asset020Key);

    ecKey2 = new ECKey(Utils.getRandom());
    asset020SecondAddress = ecKey2.getAddress();
    asset020SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(asset020SecondKey);
    logger.info(name);

    Assert.assertTrue(PublicMethed.sendcoin(asset020Address, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(asset020SecondAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Can create 32 char token name.
    Long start = System.currentTimeMillis() + 2000000;
    Long end = System.currentTimeMillis() + 1000000000;

    //When precision is -1, can not create asset issue
    Assert.assertFalse(PublicMethed.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, -1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    //When precision is 7, can not create asset issue
    Assert.assertFalse(PublicMethed.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, 7, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    //When precision is 6, is equal to default.
    Assert.assertTrue(PublicMethed.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, 6, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset020Address, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    Contract.AssetIssueContract assetIssueInfo = PublicMethed
        .getAssetIssueByName(name, blockingStubFull);
    final Integer preCisionByName = assetIssueInfo.getPrecision();
    final Long TotalSupplyByName = assetIssueInfo.getTotalSupply();

    assetIssueInfo = PublicMethed.getAssetIssueById(ByteArray.toStr(assetAccountId
        .toByteArray()), blockingStubFull);
    final Integer preCisionById = assetIssueInfo.getPrecision();
    final Long TotalSupplyById = assetIssueInfo.getTotalSupply();

    assetIssueInfo = PublicMethed.getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    final Integer preCisionByListName = assetIssueInfo.getPrecision();
    final Long TotalSupplyByListName = assetIssueInfo.getTotalSupply();

    logger.info("precision is " + preCisionByName);
    logger.info("precision is " + preCisionById);
    logger.info("precision is " + preCisionByListName);
    logger.info("totalsupply is " + TotalSupplyByName);
    logger.info("totalsupply is " + TotalSupplyById);
    logger.info("totalsupply is " + TotalSupplyByListName);
    Assert.assertEquals(preCisionById, preCisionByListName);
    Assert.assertEquals(preCisionById, preCisionByName);
    Assert.assertEquals(TotalSupplyById, TotalSupplyByListName);
    Assert.assertEquals(TotalSupplyById, TotalSupplyByName);

    //When precision is 6, is equal to default.
    Assert.assertTrue(PublicMethed.createAssetIssue(asset020SecondAddress,
        name, totalSupply, 1, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020SecondKey, blockingStubFull));

    assetIssueInfo = PublicMethed.getAssetIssueByName(name, blockingStubFull);
    Assert.assertTrue(assetIssueInfo.getName().isEmpty());

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