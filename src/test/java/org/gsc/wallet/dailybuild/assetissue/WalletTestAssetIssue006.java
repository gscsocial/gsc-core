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
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue006 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  private static final long now = System.currentTimeMillis();
  private static String name = "assetissue006" + Long.toString(now);
  private static final long totalSupply = now;
  String description = "test query assetissue by timestamp from confirmednode";
  String url = "https://testqueryassetissue.com/bytimestamp/from/confirmednode/";

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] queryAssetIssueFromSoliAddress = ecKey.getAddress();
  String queryAssetIssueKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);


  }

  /*  @Test(enabled = true)
  public void testGetAssetIssueListByTimestamp() {
      Assert.assertTrue(PublicMethed.freezeBalance(fromAddress,10000000,5,testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(queryAssetIssueFromSoliAddress,2048000000,fromAddress,
        testKey002,blockingStubFull));
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(queryAssetIssueFromSoliAddress, name,
        totalSupply, 1, 100, start, end, 1, description, url, 1000L,
        1000L,1L,1L,queryAssetIssueKey,blockingStubFull));
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Block confirmedCurrentBlock = blockingStubConfirmed.getNowBlock(GrpcAPI.EmptyMessage
        .newBuilder().build());
    Integer wait = 0;
    while (confirmedCurrentBlock.getBlockHeader().getRawData().getNumber()
        < currentBlock.getBlockHeader().getRawData().getNumber() + 1 && wait < 10) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      logger.info("Confirmed didn't synchronize the fullnode block,please wait");
      confirmedCurrentBlock = blockingStubConfirmed.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
          .build());
      wait++;
      if (wait == 9) {
        logger.info("Didn't syn,skip to next case.");
      }
    }


    long time = now;
    NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
    timeStamp.setNum(time);
    GrpcAPI.AssetIssueList assetIssueList = blockingStubConfirmed
        .getAssetIssueListByTimestamp(timeStamp.build());
    Optional<GrpcAPI.AssetIssueList> getAssetIssueListByTimestamp = Optional
        .ofNullable(assetIssueList);

    Assert.assertTrue(getAssetIssueListByTimestamp.isPresent());
    Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssueCount() > 0);
    logger.info(Integer.toString(getAssetIssueListByTimestamp.get().getAssetIssueCount()));
    for (Integer j = 0; j < getAssetIssueListByTimestamp.get().getAssetIssueCount(); j++) {
      Assert.assertFalse(getAssetIssueListByTimestamp.get().getAssetIssue(j).getName().isEmpty());
      Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssue(j).getTotalSupply() > 0);
      Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssue(j).getNum() > 0);
      logger.info(
          Long.toString(getAssetIssueListByTimestamp.get().getAssetIssue(j).getTotalSupply()));
    }

  }

  @Test(enabled = true)
  public void testExceptionGetAssetIssueListByTimestamp() {
    //Time stamp is below zero.
    long time = -1000000000;
    NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
    timeStamp.setNum(time);
    GrpcAPI.AssetIssueList assetIssueList = blockingStubConfirmed
        .getAssetIssueListByTimestamp(timeStamp.build());
    Optional<GrpcAPI.AssetIssueList> getAssetIssueListByTimestamp = Optional
        .ofNullable(assetIssueList);
    Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssueCount() == 0);

    //No asset issue was create
    time = 1000000000;
    timeStamp = NumberMessage.newBuilder();
    timeStamp.setNum(time);
    assetIssueList = blockingStubConfirmed.getAssetIssueListByTimestamp(timeStamp.build());
    getAssetIssueListByTimestamp = Optional.ofNullable(assetIssueList);
    Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssueCount() == 0);

  }*/

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelConfirmed != null) {
      channelConfirmed.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }
}

