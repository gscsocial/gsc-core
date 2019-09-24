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

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.Charset;
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
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class DelayTransaction008 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final String name = "Asset008_" + Long.toString(now);
  String description = "just-test";
  String url = "https://github.com/gscsocial/wallet-cli/";
  Long delaySecond = 10L;
  private static String updateAccountName;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");
  ByteString assetId;
  private byte[] contractAddress = null;
  SmartContract smartContract;

  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] doUpdateAccountAddress = ecKey.getAddress();
  String doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey1.getAddress();
  String newAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
  }

  @Test(enabled = false, description = "Delay account update contract")
  public void test1DelayAccountUpdate() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doUpdateAccountAddress = ecKey.getAddress();
    doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doUpdateAccountKey);
    
    Assert.assertTrue(PublicMethed.sendcoin(doUpdateAccountAddress, 1000000L, fromAddress,
        testKey002, blockingStubFull));

    final Long beforeUpdateAccountBalance = PublicMethed.queryAccount(doUpdateAccountKey,
        blockingStubFull).getBalance();
    updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    final String txid = PublicMethed.updateAccountDelayGetTxid(doUpdateAccountAddress,
        accountNameBytes,delaySecond,doUpdateAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String accountName = new String(PublicMethed.queryAccount(doUpdateAccountKey,
        blockingStubFull).getAccountName().toByteArray(), Charset.forName("UTF-8"));
    Assert.assertTrue(accountName.isEmpty());
    Assert.assertTrue(PublicMethed.queryAccount(newAccountAddress,blockingStubFull)
        .getAccountName().isEmpty());
    Long balanceInDelay = PublicMethed.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getBalance();
    Assert.assertTrue(beforeUpdateAccountBalance - balanceInDelay == delayTransactionFee);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountName = new String(PublicMethed.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getAccountName().toByteArray(), Charset.forName("UTF-8"));
    logger.info(accountName);
    Assert.assertTrue(accountName.equalsIgnoreCase(updateAccountName));
    Long afterCreateAccountBalance = PublicMethed.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getBalance();
    Long netFee = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getNetFee();
    Long fee = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getFee();
    Assert.assertTrue(fee - netFee == delayTransactionFee);
    Assert.assertTrue(beforeUpdateAccountBalance - afterCreateAccountBalance
        == delayTransactionFee);

  }

  @Test(enabled = false, description = "Cancel delay account update contract")
  public void test2CancelDelayUpdateAccount() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doUpdateAccountAddress = ecKey.getAddress();
    doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doUpdateAccountKey);

    final Long beforeUpdateAccountBalance = PublicMethed.queryAccount(doUpdateAccountKey,
        blockingStubFull).getBalance();
    updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    final String txid = PublicMethed.updateAccountDelayGetTxid(doUpdateAccountAddress,
        accountNameBytes,delaySecond,doUpdateAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,fromAddress,testKey002,
        blockingStubFull));
    final String cancelTxid = PublicMethed.cancelDeferredTransactionByIdGetTxid(txid,
        doUpdateAccountAddress,doUpdateAccountKey,blockingStubFull);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,doUpdateAccountAddress,
        doUpdateAccountKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long afterUpdateBalance = PublicMethed.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getBalance();
    final Long netFee = PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull).get()
        .getReceipt().getNetFee();
    final Long fee = PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull).get()
        .getFee();
    logger.info("net fee : " + PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull)
        .get().getReceipt().getNetFee());
    logger.info("Fee : " + PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull)
        .get().getFee());

    Assert.assertTrue(fee - netFee == cancleDelayTransactionFee);
    Assert.assertTrue(beforeUpdateAccountBalance - afterUpdateBalance
        == cancleDelayTransactionFee + delayTransactionFee);

  }


  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


