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

package org.gsc.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;

@Slf4j
public class WalletTestBlock002 {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass
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

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetBlockByNum from fullnode")
  public void testGetBlockByNum() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    if (currentBlockNum == 1) {
      logger.info("Now has very little block, Please test this case by manual");
      Assert.assertTrue(currentBlockNum == 1);
    }

    //The number is large than the currently number, there is no exception when query this number.
    Long outOfCurrentBlockNum = currentBlockNum + 10000L;
    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(outOfCurrentBlockNum);
    Block outOfCurrentBlock = blockingStubFull.getBlockByNum(builder1.build());
    Assert.assertFalse(outOfCurrentBlock.hasBlockHeader());

    //Query the first block.
    NumberMessage.Builder builder2 = NumberMessage.newBuilder();
    builder2.setNum(1);
    Block firstBlock = blockingStubFull.getBlockByNum(builder2.build());
    Assert.assertTrue(firstBlock.hasBlockHeader());
    Assert.assertFalse(firstBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getWitnessId() >= 0);

    //Query the second latest block.
    NumberMessage.Builder builder3 = NumberMessage.newBuilder();
    builder3.setNum(currentBlockNum - 1);
    Block lastSecondBlock = blockingStubFull.getBlockByNum(builder3.build());
    Assert.assertTrue(lastSecondBlock.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(
        lastSecondBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
  }

  @Test(enabled = true, description = "GetBlockByNum from confirmed")
  public void testGetBlockByNumFromConfirmed() {
    Block currentBlock = blockingStubConfirmed
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    if (currentBlockNum == 1) {
      logger.info("Now has very little block, Please test this case by manual");
      Assert.assertTrue(currentBlockNum == 1);
    }

    //The number is large than the currently number, there is no exception when query this number.
    Long outOfCurrentBlockNum = currentBlockNum + 10000L;
    NumberMessage.Builder builder1 = NumberMessage.newBuilder();
    builder1.setNum(outOfCurrentBlockNum);
    Block outOfCurrentBlock = blockingStubConfirmed.getBlockByNum(builder1.build());
    Assert.assertFalse(outOfCurrentBlock.hasBlockHeader());

    //Query the first block.
    NumberMessage.Builder builder2 = NumberMessage.newBuilder();
    builder2.setNum(1);
    Block firstBlock = blockingStubConfirmed.getBlockByNum(builder2.build());
    Assert.assertTrue(firstBlock.hasBlockHeader());
    Assert.assertFalse(firstBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getNumber() == 1);
    Assert.assertFalse(firstBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(firstBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("firstblock test from confirmed succesfully");

    //Query the second latest block.
    NumberMessage.Builder builder3 = NumberMessage.newBuilder();
    builder3.setNum(currentBlockNum - 1);
    Block lastSecondBlock = blockingStubConfirmed.getBlockByNum(builder3.build());
    Assert.assertTrue(lastSecondBlock.hasBlockHeader());
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(
        lastSecondBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlockNum);
    Assert.assertFalse(lastSecondBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(lastSecondBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("Last second test from confirmed succesfully");
  }

  @Test(enabled = true, description = "get block by id")
  public void testGetBlockById() {

    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    ByteString currentHash = currentBlock.getBlockHeader().getRawData().getParentHash();
    GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(currentHash).build();
    Block setIdOfBlock = blockingStubFull.getBlockById(request);
    Assert.assertTrue(setIdOfBlock.hasBlockHeader());
    Assert.assertFalse(setIdOfBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(setIdOfBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(setIdOfBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    logger.info(Long.toString(setIdOfBlock.getBlockHeader().getRawData().getNumber()));
    logger.info(Long.toString(currentBlock.getBlockHeader().getRawData().getNumber()));
    Assert.assertTrue(
        setIdOfBlock.getBlockHeader().getRawData().getNumber() + 1 == currentBlock.getBlockHeader()
            .getRawData().getNumber());
    Assert.assertFalse(setIdOfBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(setIdOfBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("By ID test succesfully");
  }

  /**
   * constructor.
   */

  @AfterClass
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

  public Account queryAccount(String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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


