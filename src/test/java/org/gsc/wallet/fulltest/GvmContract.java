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
import org.gsc.protos.Protocol.Account;

@Slf4j
public class GvmContract {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract008Address = ecKey1.getAddress();
  String contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
    PublicMethed.printAddress(contract008Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(contract008Address, 500000000L, fromAddress,
        testKey002, blockingStubFull));
    logger.info(Long.toString(PublicMethed.queryAccount(contract008Key, blockingStubFull)
        .getBalance()));
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(contract008Address, 1000000L,
        5, 1, contract008Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.buyStorage(50000000L, contract008Address, contract008Key,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalance(contract008Address, 5000000L,
        5, contract008Key, blockingStubFull));

  }

  @Test(enabled = false)
  public void deployErc721CryptoKitties() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract008Address,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Long storageUsage = accountResource.getStorageUsed();

    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    logger.info("before storage limit is " + Long.toString(storageLimit));
    logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 50000000L;
    String contractName = "ERC721";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TvmContract_deployErc721CryptoKitties");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TvmContract_deployErc721CryptoKitties");
    Long m = 0L;
    Long freeNet;
    accountResource = PublicMethed.getAccountResource(contract008Address, blockingStubFull);
    Long net = accountResource.getFreeNetUsed();
    Account account = PublicMethed.queryAccount(contract008Key, blockingStubFull);
    Long netUsed = account.getNetUsage();
    logger.info("before net used is " + Long.toString(netUsed));
    logger.info("before balance is " + account.getBalance());

    for (Integer i = 0; i < 1; i++) {
      byte[] contractAddress = PublicMethed.deployContract("1", abi, code, "",
          30000000L, 0L, 1, null, contract008Key, contract008Address, blockingStubFull);
      accountResource = PublicMethed.getAccountResource(contract008Address, blockingStubFull);
      freeNet = accountResource.getFreeNetUsed();
      cpuUsage = accountResource.getCpuUsed();
      logger.info(
          "Time " + Integer.toString(i) + ": cpu usage is " + Long.toString(cpuUsage - m));
      logger.info("Time " + Integer.toString(i) + ": free net used is " + Long
          .toString(freeNet - net));
      account = PublicMethed.queryAccount(contract008Key, blockingStubFull);
      logger.info("after balance is " + account.getBalance());
      netUsed = account.getNetUsage();
      logger.info("after net used is " + Long.toString(netUsed));
      net = freeNet;
      m = cpuUsage;
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
    //SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);

    //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    //Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    //Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    //logger.info(smartContract.getName());
    //logger.info(smartContract.getAbi().toString());
    accountResource = PublicMethed.getAccountResource(contract008Address, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getCpuUsed();
    storageUsage = accountResource.getStorageUsed();
    //Assert.assertTrue(storageUsage > 0);
    //Assert.assertTrue(storageLimit > 0);
    //Assert.assertTrue(cpuLimit > 0);
    //Assert.assertTrue(cpuUsage > 0);

    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    logger.info("after storage limit is " + Long.toString(storageLimit));
    logger.info("after storage usaged is " + Long.toString(storageUsage));
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


