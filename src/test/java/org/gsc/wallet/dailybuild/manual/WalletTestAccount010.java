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

package org.gsc.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;

@Slf4j
public class WalletTestAccount010 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private static final long now = System.currentTimeMillis();

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account010Address = ecKey1.getAddress();
  String account010Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] account010SecondAddress = ecKey2.getAddress();
  String account010SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] account010InvalidAddress = ecKey3.getAddress();
  String account010InvalidKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());


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
    PublicMethed.printAddress(account010Key);
    PublicMethed.printAddress(account010SecondKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


  }

  @Test(enabled = false)
  public void testGetStorage() {
    Assert.assertTrue(PublicMethed.sendcoin(account010Address, 100000000,
        fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(account010SecondAddress, 100000000,
        fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(account010InvalidAddress, 100000000,
        fromAddress, testKey002, blockingStubFull));
    Account account010Info = PublicMethed.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account010Info.getAccountResource().getStorageLimit() == 0);
    Assert.assertTrue(account010Info.getAccountResource().getLatestExchangeStorageTime() == 0);

    Assert.assertTrue(PublicMethed.buyStorage(100000000L, account010Address, account010Key,
        blockingStubFull));

    account010Info = PublicMethed.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account010Info.getAccountResource().getStorageLimit() > 0);
    Assert.assertTrue(account010Info.getAccountResource().getLatestExchangeStorageTime() > 0);

    AccountResourceMessage account010Resource = PublicMethed.getAccountResource(account010Address,
        blockingStubFull);
    Assert.assertTrue(account010Resource.getStorageLimit() > 0);
  }

  @Test(enabled = false)
  public void testSellStorage() {
    AccountResourceMessage account010Resource = PublicMethed.getAccountResource(account010Address,
        blockingStubFull);
    Long storageLimit = account010Resource.getStorageLimit();
    Account account001Info = PublicMethed.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account001Info.getBalance() == 0);
    //When there is no enough storage,sell failed.
    Assert.assertFalse(PublicMethed.sellStorage(storageLimit + 1, account010Address, account010Key,
        blockingStubFull));
    //Can not sell 0 storage
    Assert.assertFalse(PublicMethed.sellStorage(0, account010Address, account010Key,
        blockingStubFull));
    //Sell all storage.
    Assert.assertTrue(PublicMethed.sellStorage(storageLimit, account010Address, account010Key,
        blockingStubFull));
    account010Resource = PublicMethed.getAccountResource(account010Address,
        blockingStubFull);
    storageLimit = account010Resource.getStorageLimit();
    Assert.assertTrue(storageLimit == 0);
    account001Info = PublicMethed.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account001Info.getBalance() > 0);


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


