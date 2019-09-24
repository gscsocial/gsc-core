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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.junit.Assert;
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
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class GSCDice {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  byte[] contractAddress;
  Long maxFeeLimit = 1000000000L;
  Optional<TransactionInfo> infoById = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract008Address = ecKey1.getAddress();
  String contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ArrayList<String> txidList = new ArrayList<String>();

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract008Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(testKey002);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract008Address,
        blockingStubFull);


  }

  @Test(enabled = true, threadPoolSize = 30, invocationCount = 30)
  public void gscDice() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] gscDiceAddress = ecKey1.getAddress();
    String gscDiceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed
        .sendcoin(gscDiceAddress, 100000000000L, fromAddress, testKey002, blockingStubFull);
    String contractName = "GSCDice";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_GSCDice_gscDice");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_GSCDice_gscDice");
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 1000000000L, 100, null, gscDiceKey, gscDiceAddress, blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Assert.assertTrue(smartContract.getAbi() != null);

    String txid;

    for (Integer i = 0; i < 100; i++) {
      String initParmes = "\"" + "10" + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "rollDice(uint256)", initParmes, false,
          1000000, maxFeeLimit, gscDiceAddress, gscDiceKey, blockingStubFull);
      logger.info(txid);
      txidList.add(txid);

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

  @AfterClass
  public void shutdown() throws InterruptedException {
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Integer successTimes = 0;
    Integer failedTimes = 0;
    Integer totalTimes = 0;
    for (String txid1 : txidList) {
      totalTimes++;
      infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
      if (infoById.get().getBlockNumber() > 3523732) {
        logger.info("blocknum is " + infoById.get().getBlockNumber());
        successTimes++;
      } else {
        failedTimes++;
      }
    }
    logger.info("Total times is " + totalTimes.toString());
    logger.info("success times is " + successTimes.toString());
    logger.info("failed times is " + failedTimes.toString());
    logger.info("success percent is " + successTimes / totalTimes);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


