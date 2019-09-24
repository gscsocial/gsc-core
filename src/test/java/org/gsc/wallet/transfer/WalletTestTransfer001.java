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

package org.gsc.wallet.transfer;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.TransactionUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
public class WalletTestTransfer001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  //send account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  final byte[] sendAccountAddress = ecKey1.getAddress();
  String sendAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  //receipt account
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  final byte[] receiptAccountAddress = ecKey2.getAddress();
  String receiptAccountKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


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

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);
  }

  @Test
  public void testSendCoin() {
    //send account
    ecKey1 = new ECKey(Utils.getRandom());
    final byte[] sendAccountAddress = ecKey1.getAddress();
    sendAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    //receipt account
    ecKey2 = new ECKey(Utils.getRandom());
    final byte[] receiptAccountAddress = ecKey2.getAddress();
    receiptAccountKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(sendAccountAddress, 90000000000L,
        fromAddress, testKey002, blockingStubFull));

    logger.info(receiptAccountKey);
    //Test send coin.
    Account sendAccount = PublicMethed.queryAccount(sendAccountKey, blockingStubFull);
    Long sendAccountBeforeBalance = sendAccount.getBalance();
    Assert.assertTrue(sendAccountBeforeBalance == 90000000000L);
    Account receiptAccount = PublicMethed.queryAccount(receiptAccountKey, blockingStubFull);
    Long receiptAccountBeforeBalance = receiptAccount.getBalance();
    Assert.assertTrue(receiptAccountBeforeBalance == 0);

    //Test send coin
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.sendcoin(receiptAccountAddress, 49880000000L,
        sendAccountAddress, sendAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    sendAccount = PublicMethed.queryAccount(sendAccountKey, blockingStubFull);
    Long sendAccountAfterBalance = sendAccount.getBalance();
    logger.info(Long.toString(sendAccountAfterBalance));
    Assert.assertTrue(sendAccountAfterBalance == 90000000000L - 49880000000L - 100000L);

    receiptAccount = PublicMethed.queryAccount(receiptAccountKey, blockingStubFull);
    Long receiptAccountAfterBalance = receiptAccount.getBalance();
    logger.info(Long.toString(receiptAccountAfterBalance));
    Assert.assertTrue(receiptAccountAfterBalance == 49880000000L);

    //Send coin failed due to no enough balance.
    Assert.assertFalse(sendcoin(toAddress, 9199999999999999999L, fromAddress, testKey002));
    //Send coin failed due to the amount is 0.
    Assert.assertFalse(sendcoin(toAddress, 0L, fromAddress, testKey002));
    //Send coin failed due to the amount is -1Gsc.
    Assert.assertFalse(sendcoin(toAddress, -1000000L, fromAddress, testKey002));
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey) {
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Account beforeFronzen = queryAccount(ecKey, blockingStubFull);
    Long beforeFrozenBalance = 0L;
    if (beforeFronzen.getFrozenCount() != 0) {
      beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
      logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
    }

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    }

    Long afterBlockNum = 0L;
    Integer wait = 0;
    while (afterBlockNum < beforeBlockNum + 1 && wait < 10) {
      Block currentBlock1 = searchBlockingStubFull
          .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
      wait++;
      try {
        Thread.sleep(2000);
        logger.info("wait 2 second");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Account afterFronzen = queryAccount(ecKey, searchBlockingStubFull);
    Long afterFrozenBalance = afterFronzen.getFrozen(0).getFrozenBalance();
    logger.info(Long.toString(afterFronzen.getFrozen(0).getFrozenBalance()));
    logger.info(
        "beforefronen" + beforeFrozenBalance.toString() + "    afterfronzen" + afterFrozenBalance
            .toString());
    Assert.assertTrue(afterFrozenBalance - beforeFrozenBalance == freezeBalance);
    return true;
  }

  /**
   * constructor.
   */

  public Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey) {

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account search = queryAccount(ecKey, blockingStubFull);

    Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferContract contract = builder.build();
    Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
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

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}


