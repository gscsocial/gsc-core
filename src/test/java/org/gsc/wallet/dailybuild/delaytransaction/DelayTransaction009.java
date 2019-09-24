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
public class DelayTransaction009 {

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
  private static String accountId;

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
  byte[] doSetIdAddress = ecKey.getAddress();
  String doSetIdKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

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

  @Test(enabled = false, description = "Delay set account id contract")
  public void test1DelaySetAccountId() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doSetIdAddress = ecKey.getAddress();
    doSetIdKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doSetIdKey);
    
    Assert.assertTrue(PublicMethed.sendcoin(doSetIdAddress, 10000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long beforeSetAccountIdBalance = PublicMethed.queryAccount(doSetIdKey,
        blockingStubFull).getBalance();
    accountId = "accountId_" + Long.toString(System.currentTimeMillis());
    byte[] accountIdBytes = ByteArray.fromString(accountId);
    final String txid = PublicMethed.setAccountIdDelayGetTxid(accountIdBytes,
        delaySecond,doSetIdAddress,doSetIdKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String getAccountId = new String(PublicMethed.queryAccount(doSetIdKey,
        blockingStubFull).getAccountId().toByteArray(), Charset.forName("UTF-8"));
    Assert.assertTrue(getAccountId.isEmpty());

    Long balanceInDelay = PublicMethed.queryAccount(doSetIdKey,blockingStubFull)
        .getBalance();
    Assert.assertTrue(beforeSetAccountIdBalance - balanceInDelay == delayTransactionFee);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    getAccountId = new String(PublicMethed.queryAccount(doSetIdKey,blockingStubFull)
        .getAccountId().toByteArray(), Charset.forName("UTF-8"));
    logger.info(accountId);
    Assert.assertTrue(accountId.equalsIgnoreCase(getAccountId));
    Long afterCreateAccountBalance = PublicMethed.queryAccount(doSetIdKey,blockingStubFull)
        .getBalance();
    Long netFee = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getNetFee();
    Long fee = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getFee();
    Assert.assertTrue(fee - netFee == delayTransactionFee);
    Assert.assertTrue(beforeSetAccountIdBalance - afterCreateAccountBalance
        == delayTransactionFee);

  }

  @Test(enabled = false, description = "Cancel delay set account id contract")
  public void test2CancelDelayUpdateAccount() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doSetIdAddress = ecKey.getAddress();
    doSetIdKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(doSetIdKey);

    Assert.assertTrue(PublicMethed.sendcoin(doSetIdAddress, 10000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    final Long beforeSetAccountIdBalance = PublicMethed.queryAccount(doSetIdKey,
        blockingStubFull).getBalance();
    accountId = "accountId_" + Long.toString(System.currentTimeMillis());
    byte[] accountIdBytes = ByteArray.fromString(accountId);
    final String txid = PublicMethed.setAccountIdDelayGetTxid(accountIdBytes,
        delaySecond,doSetIdAddress,doSetIdKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,fromAddress,testKey002,
        blockingStubFull));
    final String cancelTxid = PublicMethed.cancelDeferredTransactionByIdGetTxid(txid,
        doSetIdAddress,doSetIdKey,blockingStubFull);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,doSetIdAddress,
        doSetIdKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long afterUpdateBalance = PublicMethed.queryAccount(doSetIdKey,blockingStubFull)
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
    Assert.assertTrue(beforeSetAccountIdBalance - afterUpdateBalance
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


