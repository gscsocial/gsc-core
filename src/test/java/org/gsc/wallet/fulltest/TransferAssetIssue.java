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

package org.gsc.wallet.fulltest;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.Sha256Hash;
import org.gsc.wallet.common.client.utils.TransactionUtils;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;


@Slf4j
public class TransferAssetIssue {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private static final long now = System.currentTimeMillis();
  private static String name = "PartAssetIssue_" + Long.toString(now);
  long totalSupply = now;
  private static final long sendAmount = 10250000000L;

  Long freeAssetNetLimit = 300000000L;
  Long publicFreeAssetNetLimit = 300000000L;
  String description = "f";
  String url = "h";


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] createAddress = ecKey1.getAddress();
  String testKeyForCreate = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] participateAssetAddress = ecKey2.getAddress();
  String testKeyForParticipate = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private static long beforeCreateAssetIssueBalance;
  private static long afterCreateAssetIssueBalance;
  private static long afterParticipateAssetIssueBalance;

  private static long start1;
  private static long end1;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    logger.info(testKeyForCreate);
    logger.info(testKeyForParticipate);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    //Send coin to 2 account.
    Assert.assertTrue(PublicMethed.sendcoin(createAddress, sendAmount,
        fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(participateAssetAddress,
        sendAmount, fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(participateAssetAddress, 10000000L, 5,
        testKeyForParticipate, blockingStubFull));
    //Create an asset issue.
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(createAddress, name, totalSupply, 1, 1,
        start, end, 1, description, url, freeAssetNetLimit, publicFreeAssetNetLimit,
        10L, 10L, testKeyForCreate, blockingStubFull));
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    final Account createInfo = PublicMethed.queryAccount(testKeyForCreate, blockingStubFull);
    final Account participateInfo = PublicMethed.queryAccount(testKeyForParticipate,
        blockingStubFull);

    Map<String, Long> assetIssueMap = new HashMap<String, Long>();

    Long temp = 0L;
    assetIssueMap = createInfo.getAssetMap();
    for (String key : assetIssueMap.keySet()) {

      logger.info("Name is " + key);
    }
    for (Long key : assetIssueMap.values()) {
      logger.info("Balance are " + Long.toString(key));
      temp = key;
    }
    beforeCreateAssetIssueBalance = temp;
    start1 = System.currentTimeMillis();
  }

  //@Test(enabled = false)
  @Test(enabled = false, threadPoolSize = 200, invocationCount = 200)
  public void transferAssetIssue() throws InterruptedException {
    Integer i = 0;
    Integer randNum;

    while (i < 20) {
      randNum = i % 4;
      i++;
      fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
          .get(randNum);
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

      transferAsset(participateAssetAddress, name.getBytes(), 1,
          createAddress, testKeyForCreate, blockingStubFull);
    }
  }

  /**
   * constructor.
   */


  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    //Print the duration.
    end1 = System.currentTimeMillis();
    logger.info("The time is " + Long.toString(end1 - start1));

    Map<String, Long> createAssetIssueMap = new HashMap<String, Long>();

    Long temp = 0L;
    Account createInfo = PublicMethed.queryAccount(testKeyForCreate, blockingStubFull);
    createAssetIssueMap = createInfo.getAssetMap();
    for (String key : createAssetIssueMap.keySet()) {

      logger.info("Name is " + key);
    }
    for (Long key : createAssetIssueMap.values()) {
      logger.info("Balance are " + Long.toString(key));
      temp = key;
    }
    afterCreateAssetIssueBalance = temp;

    temp = 0L;
    Account participateInfo = PublicMethed.queryAccount(testKeyForParticipate, blockingStubFull);
    Map<String, Long> participateAssetIssueMap = new HashMap<String, Long>();
    participateAssetIssueMap = participateInfo.getAssetMap();
    for (Long key : participateAssetIssueMap.values()) {
      logger.info("Balance are " + Long.toString(key));
      temp = key;
    }
    afterParticipateAssetIssueBalance = temp;

    logger.info("Create account has balance " + Long.toString(beforeCreateAssetIssueBalance)
        + " at the beginning");
    logger.info("Create account has balance " + Long.toString(afterCreateAssetIssueBalance)
        + " at the end");
    logger.info("Create account reduce balance " + Long.toString(beforeCreateAssetIssueBalance
        - afterCreateAssetIssueBalance));
    logger.info("Transfer account total success transaction is "
        + Long.toString(afterParticipateAssetIssueBalance));

    Integer blockTimes = 0;
    Integer blockTransParticipateNum = 0;
    Integer useNet = 0;
    Integer useFee = 0;

    while (blockTimes < 5) {
      blockTimes++;
      //Print the current block transaction num.
      Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();
      for (Integer m = 0; m < currentBlock.getTransactionsCount(); m++) {
        logger.info(currentBlock.getTransactions(m).getRetList().toString());
        String txId = ByteArray.toHexString(Sha256Hash.hash(currentBlock.getTransactions(m)
            .getRawData().toByteArray()));
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Transaction transaction = blockingStubFull.getTransactionById(request);
        Optional<Transaction> getTransactionById = Optional.ofNullable(transaction);
        if (getTransactionById.get().getRet(0).getFee() > 0) {
          logger.info(Long.toString(getTransactionById.get().getRet(0).getFee()));
          useFee++;
        } else {
          logger.info("No use fee");
          useNet++;
        }
      }

      logger.info("The block num " + Long.toString(currentNum)
          + " total transaction is " + Long.toString(currentBlock.getTransactionsCount()));
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    logger.info("Use Net num is " + Integer.toString(useNet));
    logger.info("Use Fee num is " + Integer.toString(useFee));

    createInfo = PublicMethed.queryAccount(testKeyForCreate, blockingStubFull);
    participateInfo = PublicMethed.queryAccount(testKeyForParticipate, blockingStubFull);
    createAssetIssueMap = new HashMap<String, Long>();
    participateAssetIssueMap = new HashMap<String, Long>();

    temp = 0L;
    createAssetIssueMap = createInfo.getAssetMap();
    for (String key : createAssetIssueMap.keySet()) {

      logger.info("Name is " + key);
    }
    for (Long key : createAssetIssueMap.values()) {
      logger.info("Balance are " + Long.toString(key));
      temp = key;
    }
    afterCreateAssetIssueBalance = temp;

    temp = 0L;
    participateAssetIssueMap = participateInfo.getAssetMap();
    for (Long key : participateAssetIssueMap.values()) {
      logger.info("Balance are " + Long.toString(key));
      temp = key;
    }
    afterParticipateAssetIssueBalance = temp;

    logger.info("Create account has balance " + Long.toString(beforeCreateAssetIssueBalance)
        + "at the beginning");
    logger.info("Create account has balance " + Long.toString(afterCreateAssetIssueBalance)
        + "at the end");
    logger.info("Participate account total success transaction is "
        + Long.toString(afterParticipateAssetIssueBalance));

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */


  public static boolean participateAssetIssue(byte[] to, byte[] assertName, long amount,
      byte[] from, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    Contract.ParticipateAssetIssueContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
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

  /**
   * constructor.
   */

  public static boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
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

    Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferAssetContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.transferAsset(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      if (transaction == null) {
        //logger.info("transaction == null");
      } else {
        //logger.info("transaction.getRawData().getContractCount() == 0");
      }
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      //logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      //Protocol.Account search = queryAccount(ecKey, blockingStubFull);
      return true;
    }
  }

}


