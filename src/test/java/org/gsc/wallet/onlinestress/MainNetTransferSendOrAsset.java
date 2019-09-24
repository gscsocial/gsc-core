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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class MainNetTransferSendOrAsset {

  //testng001、testng002、testng003、testng004
  //fromAssetIssue
  private final String testKey001 =
      "BC70ADC5A0971BA3F7871FBB7249E345D84CE7E5458828BE1E28BF8F98F2795B";
  //toAssetIssue
  private final String testKey002 =
      "F153A0E1A65193846A3D48A091CD0335594C0A3D9817B3441390FDFF71684C84";
  //fromSend
  private final String testKey003 =
      "2514B1DD2942FF07F68C2DDC0EE791BC7FBE96FDD95E89B7B9BB3B4C4770FFAC";
  //toSend
  private final String testKey004 =
      "56244EE6B33C14C46704DFB67ED5D2BBCBED952EE46F1FD88A50C32C8C5C64CE";
  //Default
  private final String defaultKey =
      "8DFBB4513AECF779A0803C7CEBF2CDCC51585121FAB1E086465C4E0B40724AF1";

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey001);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] fromSendAddress = PublicMethed.getFinalAddress(testKey003);
  private final byte[] toSendAddress = PublicMethed.getFinalAddress(testKey004);
  private final byte[] defaultAddress = PublicMethed.getFinalAddress(defaultKey);


  private final Long transferAmount = 1L;
  private Long start;
  private Long end;
  private Long beforeToBalance;
  private Long afterToBalance;
  private Long beforeToAssetBalance = 0L;
  private Long afterToAssetBalance = 0L;
  private final Long sendAmount = 1L;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);


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
    Account fromAccount = PublicMethed.queryAccount(testKey001, blockingStubFull);
    Account toAccount = PublicMethed.queryAccount(testKey002, blockingStubFull);
    if (fromAccount.getBalance() < 10000000000L) {
      PublicMethed
          .sendcoin(fromAddress, 10000000000L, defaultAddress, defaultKey, blockingStubFull);
    }
    if (fromAccount.getAssetCount() == 0) {
      start = System.currentTimeMillis() + 2000;
      end = System.currentTimeMillis() + 1000000000;
      PublicMethed.createAssetIssue(fromAddress, "testNetAsset", 1000000000000L,
          1, 1, start, end, 1, "wwwwww", "wwwwwwww", 100000L,
          100000L, 1L, 1L, testKey001, blockingStubFull);
    }
    beforeToBalance = toAccount.getBalance();
    beforeToAssetBalance = toAccount.getAssetMap().get("testNetAsset");

    Account fromSendAccount = PublicMethed.queryAccount(testKey003, blockingStubFull);
    Account toSendAccount = PublicMethed.queryAccount(testKey004, blockingStubFull);
    if (fromSendAccount.getBalance() < 1000000000L) {
      PublicMethed
          .sendcoin(fromSendAddress, 1000000000L, defaultAddress, defaultKey, blockingStubFull);
    }
    beforeToBalance = toAccount.getBalance();
    logger.info("Before From account balance is " + Long.toString(fromAccount.getBalance()));
    logger.info("Before To account balance is " + Long.toString(toAccount.getBalance()));
    start = System.currentTimeMillis();
  }

  @Test(enabled = false, threadPoolSize = 20, invocationCount = 100000)
  public void freezeAnd() throws InterruptedException {
    Random rand = new Random();
    Integer randNum = 0;
    randNum = rand.nextInt(1000);
    try {
      Thread.sleep(randNum);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Integer i = 0;
    while (i < 60) {
      PublicMethed
          .transferAsset(toAddress, "testNetAsset".getBytes(), transferAmount, fromAddress,
              testKey001, blockingStubFull);
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      PublicMethed.sendcoin(toSendAddress, sendAmount, fromSendAddress, testKey003,
          blockingStubFull);
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    end = System.currentTimeMillis();
    logger.info("Time is " + Long.toString(end - start));
    Account fromAccount = PublicMethed.queryAccount(testKey001, blockingStubFull);
    Account toAccount = PublicMethed.queryAccount(testKey002, blockingStubFull);
    afterToBalance = toAccount.getBalance();
    afterToAssetBalance = toAccount.getAssetMap().get("testNetAsset");

    logger.info("Success times is " + Long.toString(afterToAssetBalance - beforeToAssetBalance));
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }
}


