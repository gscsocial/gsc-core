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

package org.gsc.wallet.block;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;

@Slf4j
public class WalletTestBlock002 {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void testGetBlockByExceptionLimitNext() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now has very little block, Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }

    //From -1 to 1
    GrpcAPI.BlockLimit.Builder builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(-1);
    builder.setEndNum(1);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    Optional<GrpcAPI.BlockList> getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);

    //From 3 to 3
    builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(3);
    builder.setEndNum(3);
    blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);

    //From 4 to 2
    builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(4);
    builder.setEndNum(2);
    blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);

    //From 999999990 to 999999999
    builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(999999990);
    builder.setEndNum(999999999);
    blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 0);
  }

  @Test(enabled = true)
  public void testGetBlockByLimitNext() {
    //
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Assert.assertFalse(currentBlockNum < 0);
    while (currentBlockNum <= 5) {
      logger.info("Now has very little block, Please wait");
      currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    }

    GrpcAPI.BlockLimit.Builder builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(2);
    builder.setEndNum(4);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    Optional<GrpcAPI.BlockList> getBlockByLimitNext = Optional.ofNullable(blockList);
    Assert.assertTrue(getBlockByLimitNext.isPresent());
    Assert.assertTrue(getBlockByLimitNext.get().getBlockCount() == 2);
    logger.info(Long.toString(
            getBlockByLimitNext.get().getBlock(0).getBlockHeader().getRawData().getNumber()));
    logger.info(Long.toString(
            getBlockByLimitNext.get().getBlock(1).getBlockHeader().getRawData().getNumber()));
    Assert.assertTrue(
            getBlockByLimitNext.get().getBlock(0).getBlockHeader().getRawData().getNumber() < 4);
    Assert.assertTrue(
            getBlockByLimitNext.get().getBlock(1).getBlockHeader().getRawData().getNumber() < 4);
    Assert.assertTrue(getBlockByLimitNext.get().getBlock(0).hasBlockHeader());
    Assert.assertTrue(getBlockByLimitNext.get().getBlock(1).hasBlockHeader());
    Assert.assertFalse(
            getBlockByLimitNext.get().getBlock(0).getBlockHeader().getRawData().getParentHash()
                    .isEmpty());
    Assert.assertFalse(
            getBlockByLimitNext.get().getBlock(1).getBlockHeader().getRawData().getParentHash()
                    .isEmpty());
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

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


