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

package org.gsc.wallet.dailybuild.delaytransaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
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
//import org.gsc.protos.Protocol.DeferredTransaction;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class DelayTransaction001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");


  public static final long MAX_DEFERRED_TRANSACTION_DELAY_SECONDS = 45 * 24 * 3_600L; //45 days
  Optional<TransactionInfo> infoById = null;
  //Optional<DeferredTransaction> deferredTransactionById = null;
  Optional<Transaction> getTransactionById = null;


  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] delayAccount1Address = ecKey.getAddress();
  String delayAccount1Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] delayAccount2Address = ecKey2.getAddress();
  String delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiverAccountAddress = ecKey3.getAddress();
  String receiverAccountKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] delayAccount3Address = ecKey4.getAddress();
  String delayAccount3Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] receiverAccount4Address = ecKey5.getAddress();
  String receiverAccount4Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Delayed send coin to test delayed second")
  public void test1DelayedSecond() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    delayAccount1Address = ecKey.getAddress();
    delayAccount1Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount1Key);
    Assert.assertTrue(PublicMethed.sendcoin(delayAccount1Address, 100000000L,fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.sendcoinDelayed(fromAddress, 100000000L, 23L,
        delayAccount1Address, delayAccount1Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoinDelayed(fromAddress, 1L, 0L,delayAccount1Address,
        delayAccount1Key, blockingStubFull));
    Assert.assertFalse(PublicMethed.sendcoinDelayed(fromAddress, 1L, -1L,delayAccount1Address,
        delayAccount1Key, blockingStubFull));
    Assert.assertFalse(PublicMethed.sendcoinDelayed(fromAddress, 1L,
        MAX_DEFERRED_TRANSACTION_DELAY_SECONDS + 1L,delayAccount1Address, delayAccount1Key,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoinDelayed(fromAddress, 1L,
        MAX_DEFERRED_TRANSACTION_DELAY_SECONDS,delayAccount1Address, delayAccount1Key,
        blockingStubFull));
  }


  @Test(enabled = true, description = "Delay send coin")
  public void test3DelaySendCoin() {
    ecKey4 = new ECKey(Utils.getRandom());
    delayAccount3Address = ecKey4.getAddress();
    delayAccount3Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount3Key);

    ecKey5 = new ECKey(Utils.getRandom());
    receiverAccount4Address = ecKey5.getAddress();
    receiverAccount4Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
    PublicMethed.printAddress(receiverAccount4Key);

    Long sendCoinAmount = 100000000L;
    //Pre sendcoin to the test account
    Assert.assertTrue(PublicMethed.sendcoin(delayAccount3Address, sendCoinAmount,fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("----------------No balance to send coin--------------------");
    //Do delay send coin transaction.
    Long delaySecond = 4L;

    //Test no balance to send coin.
    //Query balance before send coin.
    Long deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    Long recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    Assert.assertFalse(PublicMethed.sendcoinDelayed(receiverAccount4Address, sendCoinAmount,
        delaySecond,delayAccount3Address, delayAccount3Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance after delay send coin.
    Long deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    Long recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);

    Assert.assertTrue(recevierAccountAfterDelayalance == 0);
    logger.info("deplayAccountBeforeBalance: " + deplayAccountBeforeBalance);
    logger.info("deplayAccountAfterBalance: " + deplayAccountAfterBalance);

    Assert.assertEquals(deplayAccountBeforeBalance,deplayAccountAfterBalance);


    logger.info("----------------No balance to create account send coin--------------------");
    //Test delay send coin to create account.
    deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);

    Long createAccountFee = 100000L;
    Assert.assertTrue(PublicMethed.sendcoinDelayed(receiverAccount4Address,
        deplayAccountBeforeBalance - createAccountFee, delaySecond,delayAccount3Address,
        delayAccount3Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance after delay send coin.
    deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);

    Assert.assertTrue(recevierAccountAfterDelayalance == 0);
    Assert.assertTrue(deplayAccountBeforeBalance - deplayAccountAfterBalance == 100000);


    logger.info("---------------Balance enough to create account send coin--------------------");
    //Test delay send coin to create account.
    createAccountFee = 100000L;
    deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);
    Assert.assertTrue(PublicMethed.sendcoinDelayed(receiverAccount4Address,
        deplayAccountBeforeBalance - createAccountFee - delayTransactionFee,
        delaySecond,delayAccount3Address, delayAccount3Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance after delay send coin.
    deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);
    Long receiverBalanceShouldBe = deplayAccountBeforeBalance - createAccountFee
        - delayTransactionFee;

    Assert.assertEquals(recevierAccountAfterDelayalance, receiverBalanceShouldBe);
    Assert.assertTrue(deplayAccountAfterBalance == 0);


  }

  @Test(enabled = true, description = "Not enough money to send coin.")
  public void test4DelaySendCoin() {
    ecKey4 = new ECKey(Utils.getRandom());
    delayAccount3Address = ecKey4.getAddress();
    delayAccount3Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount3Key);

    ecKey5 = new ECKey(Utils.getRandom());
    receiverAccount4Address = ecKey5.getAddress();
    receiverAccount4Key = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
    PublicMethed.printAddress(receiverAccount4Key);

    Long sendCoinAmount = 100000000L;
    //Pre sendcoin to the test account
    Assert.assertTrue(PublicMethed.sendcoin(delayAccount3Address, sendCoinAmount, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAccount4Address, sendCoinAmount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Do delay send coin transaction.
    Long createAccountFee = 100000L;

    logger.info("----------------Send all balance to exist account--------------------");
    //Test no balance to send coin.
    //Query balance before send coin.
    Long deplayAccountBeforeBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    Long recevierAccountBeforeBalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountBeforeBalance " + deplayAccountBeforeBalance);
    logger.info("recevierAccountBeforeBalance " + recevierAccountBeforeBalance);

    Long delaySecond = 4L;
    Assert.assertTrue(PublicMethed.sendcoinDelayed(receiverAccount4Address, sendCoinAmount,
        delaySecond,delayAccount3Address, delayAccount3Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Query balance after delay send coin.
    Long deplayAccountAfterBalance = PublicMethed.queryAccount(delayAccount3Address,
        blockingStubFull).getBalance();
    Long recevierAccountAfterDelayalance = PublicMethed.queryAccount(receiverAccount4Address,
        blockingStubFull).getBalance();
    logger.info("deplayAccountAfterBalance " + deplayAccountAfterBalance);
    logger.info("recevierAccountAfterDelayalance " + recevierAccountAfterDelayalance);

    Assert.assertTrue(recevierAccountAfterDelayalance == 0);
    logger.info("deplayAccountBeforeBalance: " + deplayAccountBeforeBalance);
    logger.info("deplayAccountAfterBalance: " + deplayAccountAfterBalance);

    Assert.assertEquals(deplayAccountBeforeBalance,deplayAccountAfterBalance);



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


