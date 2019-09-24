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
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.TransactionUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.PaginatedMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.db.Manager;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue017 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  private static long start;
  private static long end;
  private static long now = System.currentTimeMillis();
  private static String name = "AssetIssue017_" + Long.toString(now);
  private static long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;

  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "for case assetissue017";
  String url = "https://stest.assetissue016.url";

  private Manager dbManager;


  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset017Address = ecKey1.getAddress();
  String testKeyForAssetIssue017 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
    logger.info(testKeyForAssetIssue017);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
  }

  @Test(enabled = true)
  public void atestGetPaginatedAssetIssueList() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset017Address = ecKey1.getAddress();
    testKeyForAssetIssue017 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed
        .sendcoin(asset017Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    now = System.currentTimeMillis();
    name = "AssetIssue017_" + Long.toString(now);
    totalSupply = now;
    Assert.assertTrue(createAssetIssue(asset017Address, name, totalSupply, 1, 1,
        start, end, 1, description, url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L,
        1L, testKeyForAssetIssue017, blockingStubFull));

    Integer offset = 0;
    Integer limit = 100;

    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);

    AssetIssueList assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);
    for (Integer i = 0; i < assetIssueListPaginated.get().getAssetIssueCount(); i++) {
      Assert.assertTrue(assetIssueListPaginated.get().getAssetIssue(i).getTotalSupply() > 0);
    }
    PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull, blockingStubConfirmed);
  }

  @Test(enabled = true)
  public void btestGetPaginatedAssetIssueListException() {
    //offset is 0, limit is 0.
    Integer offset = 0;
    Integer limit = 0;
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    AssetIssueList assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is -1, limit is 100.
    offset = -1;
    limit = 100;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is -1.
    offset = 0;
    limit = -1;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is 50.
    offset = 0;
    limit = 50;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);
  }

  @Test(enabled = true)
  public void ctestGetPaginatedAssetIssueListOnConfirmedNode() {

    Integer offset = 0;
    Integer limit = 100;

    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    Assert.assertTrue(PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull,
        blockingStubConfirmed));
    AssetIssueList assetIssueList = blockingStubConfirmed
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);

    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);
    for (Integer i = 0; i < assetIssueListPaginated.get().getAssetIssueCount(); i++) {
      Assert.assertTrue(assetIssueListPaginated.get().getAssetIssue(i).getTotalSupply() > 0);
    }
  }

  @Test(enabled = true)
  public void dtestGetPaginatedAssetIssueListExceptionOnConfirmedNode() {
    //offset is 0, limit is 0.
    Integer offset = 0;
    Integer limit = 0;
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    AssetIssueList assetIssueList = blockingStubConfirmed
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is -1.
    offset = 0;
    limit = -1;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubConfirmed
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is 50.
    offset = 0;
    limit = 50;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubConfirmed
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);

    //offset is 0, limit is 1000.
    offset = 0;
    limit = 1000;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubConfirmed
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);

    //offset is -1, limit is 100.
    offset = -1;
    limit = 100;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubConfirmed
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
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

  public static Boolean createAssetIssue(byte[] address, String name, Long totalSupply,
      Integer gscNum, Integer icoNum, Long startTime, Long endTime, Integer voteScore,
      String description, String url, Long freeAssetNetLimit, Long publicFreeAssetNetLimit,
      Long fronzenAmount, Long frozenDay, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(ecKey, blockingStubFull);
    try {
      Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setGscNum(gscNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          Contract.AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = signTransaction(ecKey, transaction);

      GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
      if (response.getResult() == false) {
        logger.info("failed reason is " + ByteArray.toStr(response.getMessage().toByteArray()));
        return false;
      } else {
        return true;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /**
   * constructor.
   */

  public static Protocol.Transaction signTransaction(ECKey ecKey,
      Protocol.Transaction transaction) {
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
    if (ecKey == null || ecKey.getPrivKey() == null) {
      //logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}