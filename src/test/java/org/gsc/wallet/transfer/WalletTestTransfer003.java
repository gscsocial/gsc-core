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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.Sha256Hash;
import org.gsc.wallet.common.client.utils.TransactionUtils;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.WalletExtensionGrpc;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.TransactionInfo;


@Slf4j
public class WalletTestTransfer003 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private final Long createUseFee = 100000L;

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;

  private static final long now = System.currentTimeMillis();
  private static final String name = "transaction007_" + Long.toString(now);
  private static Protocol.Transaction sendCoinTransaction;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sendCoinAddress = ecKey1.getAddress();
  String testKeyForSendCoin = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey2.getAddress();
  String testKeyForNewAccount = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /*  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }*/

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    logger.info(testKeyForSendCoin);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
    blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelConfirmed);

  }


  @Test(enabled = true)
  public void test1UseFeeOrNet() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    sendCoinAddress = ecKey1.getAddress();
    testKeyForSendCoin = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    newAccountAddress = ecKey2.getAddress();
    testKeyForNewAccount = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(sendCoinAddress, 200000L,
        fromAddress, testKey002, blockingStubFull));
    Long feeNum = 0L;
    Long netNum = 0L;
    Long sendNum = 0L;
    Long feeCost = 0L;
    Long times = 0L;
    Account sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    final Long beforeBalance = sendAccountInfo.getBalance();
    Long netUsed1 = 0L;
    Long netUsed2 = 1L;
    logger.info("Before test, the account balance is " + Long.toString(beforeBalance));

    while (!(netUsed1.equals(netUsed2))) {
      sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
      netUsed1 = sendAccountInfo.getFreeNetUsage();
      sendCoinTransaction = sendcoin(fromAddress, 1L, sendCoinAddress,
          testKeyForSendCoin, blockingStubFull);

      sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
      netUsed2 = sendAccountInfo.getFreeNetUsage();

      if (times++ < 1) {
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        //PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull,blockingStubConfirmed);
        String txId = ByteArray.toHexString(Sha256Hash.hash(sendCoinTransaction
            .getRawData().toByteArray()));
        logger.info(txId);
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        TransactionInfo transactionInfo = blockingStubFull.getTransactionInfoById(request);
        Optional<TransactionInfo> getTransactionById = Optional.ofNullable(transactionInfo);
        logger.info("confirmed block num is " + Long.toString(getTransactionById
            .get().getBlockNumber()));
        Assert.assertTrue(getTransactionById.get().getBlockNumber() > 0);
      }

      logger.info(Long.toString(netUsed1));
      logger.info(Long.toString(netUsed2));
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Assert.assertTrue(netUsed2 > 4500);
    //Next time, use fee
    sendCoinTransaction = sendcoin(fromAddress, 1L, sendCoinAddress,
        testKeyForSendCoin, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull,blockingStubConfirmed);
    String txId = ByteArray.toHexString(Sha256Hash.hash(sendCoinTransaction
        .getRawData().toByteArray()));
    logger.info(txId);
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo = blockingStubFull.getTransactionInfoById(request);
    Optional<TransactionInfo> getTransactionById = Optional.ofNullable(transactionInfo);
    logger.info(getTransactionById.get().toString());
    logger.info("when use fee, the block num is " + Long.toString(getTransactionById
        .get().getBlockNumber()));
    Assert.assertTrue(getTransactionById.get().getFee() > 0);
    Assert.assertTrue(getTransactionById.get().getBlockNumber() > 0);
  }

  @Test(enabled = true)
  public void test2CreateAccountUseFee() {
    Account sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    final Long beforeBalance = sendAccountInfo.getBalance();
    logger.info("before balance " + Long.toString(beforeBalance));
    Long times = 0L;
    sendCoinTransaction = sendcoin(newAccountAddress, 1L, sendCoinAddress,
        testKeyForSendCoin, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull,blockingStubConfirmed);
    String txId = ByteArray.toHexString(Sha256Hash.hash(sendCoinTransaction
        .getRawData().toByteArray()));
    logger.info(txId);
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo = blockingStubFull.getTransactionInfoById(request);
    Optional<TransactionInfo> getTransactionById = Optional.ofNullable(transactionInfo);

    logger.info("In create account case, the fee is " + getTransactionById.get().getFee());
    Assert.assertTrue(getTransactionById.get().getFee() == createUseFee);

    sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    final Long afterBalance = sendAccountInfo.getBalance();
    logger.info("after balance " + Long.toString(afterBalance));
    Assert.assertTrue(afterBalance + 1L + createUseFee == beforeBalance);
  }

  @Test(enabled = true)
  public void test3InvalidGetTransactionById() {
    String txId = "";
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubFull.getTransactionById(request);
    Optional<Transaction> getTransactionById = Optional.ofNullable(transaction);
    Assert.assertTrue(getTransactionById.get().getRawData().getContractCount() == 0);

    txId = "1";
    bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    request = BytesMessage.newBuilder().setValue(bsTxid).build();
    transaction = blockingStubFull.getTransactionById(request);
    getTransactionById = Optional.ofNullable(transaction);
    Assert.assertTrue(getTransactionById.get().getRawData().getContractCount() == 0);
  }

  @Test(enabled = true)
  public void test4NoBalanceCanSend() {
    Long feeNum = 0L;
    Account sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    Long beforeBalance = sendAccountInfo.getBalance();
    logger.info("Before test, the account balance is " + Long.toString(beforeBalance));
    while (feeNum < 250) {
      sendCoinTransaction = sendcoin(fromAddress, 10L, sendCoinAddress,
          testKeyForSendCoin, blockingStubFull);
      feeNum++;
    }
    Assert.assertTrue(PublicMethed.waitProduceNextBlock(blockingStubFull));

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
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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

  private static Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /**
   * constructor.
   */

  public static Protocol.Transaction sendcoin(byte[] to, long amount, byte[] owner, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
    }

    return transaction;
  }

  /**
   * constructor.
   */

  public Protocol.Transaction updateAccount(byte[] addressBytes, byte[] accountNameBytes,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(addressBytes);
    ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(basAddreess);

    Contract.AccountUpdateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
    }

    return transaction;


  }
}


