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

package org.gsc.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountNetMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private static final long now = System.currentTimeMillis();
  private static String name = "AssetIssue007_" + Long.toString(now);
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static final Integer gscNum = 1;
  private static final Integer icoNum = 1;

  Long freeAssetNetLimit = 10000L;
  Long publicFreeAssetNetLimit = 10000L;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset007Address = ecKey1.getAddress();
  String testKeyForAssetIssue007 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] participateAssetAddress = ecKey2.getAddress();
  String participateAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForAssetIssue007);
    PublicMethed.printAddress(participateAssetCreateKey);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Participate asset issue use participate net")
  public void testParticipateAssetIssueUseParticipateNet() {
    Assert.assertTrue(PublicMethed
        .sendcoin(asset007Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long start = System.currentTimeMillis() + 5000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed
        .createAssetIssue(asset007Address, name, totalSupply, gscNum, icoNum, start, end, 1,
            description, url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L,
            testKeyForAssetIssue007, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(name);
    //Assert.assertTrue(PublicMethed.waitProduceNextBlock(blockingStubFull));
    //When no balance, participate an asset issue
    Assert.assertFalse(PublicMethed.participateAssetIssue(asset007Address, name.getBytes(),
        1L, participateAssetAddress, participateAssetCreateKey, blockingStubFull));

    ByteString addressBs = ByteString.copyFrom(asset007Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage asset007NetMessage = blockingStubFull.getAccountNet(request);
    final Long asset007BeforeFreeNetUsed = asset007NetMessage.getFreeNetUsed();

    //SendCoin to participate account.
    Assert.assertTrue(PublicMethed.sendcoin(participateAssetAddress, 10000000L,
        fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    addressBs = ByteString.copyFrom(participateAssetAddress);
    request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage participateAccountNetMessage = blockingStubFull.getAccountNet(request);
    final Long participateAccountBeforeNetUsed = participateAccountNetMessage.getFreeNetUsed();
    Assert.assertTrue(participateAccountBeforeNetUsed == 0);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset007Address, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    logger.info(assetAccountId.toString());

    //Participate an assetIssue, then query the net information.
    Assert.assertTrue(PublicMethed.participateAssetIssue(
        asset007Address, assetAccountId.toByteArray(),
        1L, participateAssetAddress, participateAssetCreateKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    addressBs = ByteString.copyFrom(asset007Address);
    request = Account.newBuilder().setAddress(addressBs).build();
    asset007NetMessage = blockingStubFull.getAccountNet(request);
    final Long asset007AfterFreeNetUsed = asset007NetMessage.getFreeNetUsed();

    addressBs = ByteString.copyFrom(participateAssetAddress);
    request = Account.newBuilder().setAddress(addressBs).build();
    participateAccountNetMessage = blockingStubFull.getAccountNet(request);
    final Long participateAccountAfterNetUsed = participateAccountNetMessage.getFreeNetUsed();

    logger.info(Long.toString(asset007BeforeFreeNetUsed));
    logger.info(Long.toString(asset007AfterFreeNetUsed));
    logger.info(Long.toString(participateAccountBeforeNetUsed));
    logger.info(Long.toString(participateAccountAfterNetUsed));
    Assert.assertTrue(asset007AfterFreeNetUsed <= asset007BeforeFreeNetUsed);
    Assert.assertTrue(participateAccountAfterNetUsed - participateAccountBeforeNetUsed > 150);

    Assert.assertTrue(PublicMethed.participateAssetIssue(
        asset007Address, assetAccountId.toByteArray(),
        1L, participateAssetAddress, participateAssetCreateKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.participateAssetIssue(
        asset007Address, assetAccountId.toByteArray(),
        1L, participateAssetAddress, participateAssetCreateKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account participateInfo = PublicMethed
        .queryAccount(participateAssetCreateKey, blockingStubFull);
    final Long beforeBalance = participateInfo.getBalance();
    Assert.assertTrue(PublicMethed.participateAssetIssue(
        asset007Address, assetAccountId.toByteArray(),
        1L, participateAssetAddress, participateAssetCreateKey, blockingStubFull));
    participateInfo = PublicMethed.queryAccount(participateAssetCreateKey, blockingStubFull);
    final Long afterBalance = participateInfo.getBalance();

    Assert.assertTrue(beforeBalance - gscNum * 1 * icoNum >= afterBalance);
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


