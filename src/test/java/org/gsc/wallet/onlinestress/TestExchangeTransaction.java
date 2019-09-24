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

package org.gsc.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.ExchangeList;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.wallet.common.client.utils.PublicMethed;


@Slf4j
public class TestExchangeTransaction {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key17");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  Optional<ExchangeList> listExchange;
  Optional<Exchange> exchangeIdInfo;
  Integer exchangeId = 0;
  Integer exchangeRate = 10;
  Long firstTokenInitialBalance = 10000L;
  Long secondTokenInitialBalance = firstTokenInitialBalance * exchangeRate;


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

  }

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20)
  public void testExchangeTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] exchangeAddress = ecKey1.getAddress();
    String exchangeKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    final byte[] transactionAddress = ecKey2.getAddress();
    String transactionKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.printAddress(exchangeKey);
    PublicMethed.printAddress(transactionKey);

    Assert.assertTrue(PublicMethed.sendcoin(exchangeAddress, 1500000000000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(transactionAddress, 1500000000000000L, fromAddress,
        testKey002, blockingStubFull));
    Long totalSupply = 1500000000000000L;
    Random rand = new Random();
    Integer randNum = rand.nextInt(900000000) + 1;
    String name = "exchange_" + Long.toString(randNum);
    Long start = System.currentTimeMillis() + 20000;
    Long end = System.currentTimeMillis() + 10000000000L;
    String description = "This asset issue is use for exchange transaction stress";
    String url = "This asset issue is use for exchange transaction stress";
    Assert.assertTrue(PublicMethed.createAssetIssue(exchangeAddress, name, totalSupply, 1, 1,
        start, end, 1, description, url, 100000000L, 10000000000L,
        10L, 10L, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(PublicMethed.transferAsset(transactionAddress, name.getBytes(),
        1500000000L, exchangeAddress, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //500000000000000L  //5000000L
    Assert.assertTrue(PublicMethed.exchangeCreate(name.getBytes(), 500000000000000L,
        "_".getBytes(), 500000000000000L, exchangeAddress, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(300000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    Integer i = 0;
    while (i++ < 10000) {
      PublicMethed.exchangeTransaction(exchangeId, "_".getBytes(), 100000, 99,
          transactionAddress, transactionKey, blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      PublicMethed.exchangeTransaction(exchangeId, name.getBytes(), 100000, 1,
          transactionAddress, transactionKey, blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


