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

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Parameter;
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
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class DelayTransaction011 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");

  public static final long ONE_DELAY_SECONDS = 60 * 60 * 24L;
  //Optional<DeferredTransaction> deferredTransactionById = null;

  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] noNetAddress = ecKey.getAddress();
  String noNetKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] delayAccount2Address = ecKey2.getAddress();
  String delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false, description = "When Net not enough, create delay transaction.")
  public void test1NetInDelayTransaction() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    noNetAddress = ecKey.getAddress();
    noNetKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(noNetKey);
    ecKey2 = new ECKey(Utils.getRandom());
    delayAccount2Address = ecKey2.getAddress();
    delayAccount2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(delayAccount2Key);


    Assert.assertTrue(PublicMethed.sendcoin(noNetAddress, 10000000000L,fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    while (PublicMethed.queryAccount(noNetAddress,blockingStubFull).getFreeNetUsage()
        < 4700L) {
      PublicMethed.sendcoin(delayAccount2Address,1L, noNetAddress, noNetKey,
          blockingStubFull);
    }
    PublicMethed.sendcoin(delayAccount2Address,1L, noNetAddress, noNetKey,
        blockingStubFull);
    PublicMethed.sendcoin(delayAccount2Address,1L, noNetAddress, noNetKey,
        blockingStubFull);
    Assert.assertTrue(PublicMethed.sendcoin(fromAddress,PublicMethed.queryAccount(
            noNetAddress,blockingStubFull).getBalance() - 3000L, noNetAddress,
            noNetKey,blockingStubFull));
    logger.info("balance is: " +  PublicMethed.queryAccount(noNetAddress,
        blockingStubFull).getBalance());
    logger.info("Free net usage is " + PublicMethed.queryAccount(noNetAddress,
        blockingStubFull).getFreeNetUsage());

    String updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    String txid = PublicMethed.updateAccountDelayGetTxid(noNetAddress,accountNameBytes,
        10L, noNetKey,blockingStubFull);
    logger.info(txid);
    Assert.assertTrue(PublicMethed.getTransactionById(txid,blockingStubFull)
        .get().getRawData().getContractCount() == 0);

    Assert.assertTrue(PublicMethed.sendcoin(noNetAddress, 103332L - 550L,fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    txid = PublicMethed.updateAccountDelayGetTxid(noNetAddress,accountNameBytes,
        10L, noNetKey,blockingStubFull);

  }


  /**
     * constructor.
   * */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


