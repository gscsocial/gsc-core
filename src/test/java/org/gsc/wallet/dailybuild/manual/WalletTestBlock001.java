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
public class WalletTestBlock001 {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  /**
   * constructor.
   */
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
  @Test(enabled = true, description = "Get now block from fullnode")
  public void testCurrentBlock() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock is " + Long.toString(currentBlock.getBlockHeader()
        .getRawData().getNumber()));

    //Improve coverage.
    currentBlock.equals(currentBlock);
    Block newBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    newBlock.equals(currentBlock);
    newBlock.hashCode();
    newBlock.getSerializedSize();
    newBlock.getTransactionsCount();
    newBlock.getTransactionsList();
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from confirmed")
  public void testCurrentBlockFromConfirmed() {
    Block currentBlock = blockingStubConfirmed
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock in confirmednode is " + Long.toString(currentBlock
        .getBlockHeader().getRawData().getNumber()));
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


