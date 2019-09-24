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

import static org.gsc.wallet.common.client.utils.PublicMethed.getTransactionInfoById;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TestNetFomo3D {

  //testng001、testng002、testng003、testng004
  private final String testNetAccountKey =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  //"BC70ADC5A0971BA3F7871FBB7249E345D84CE7E5458828BE1E28BF8F98F2795B";
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  Optional<TransactionInfo> infoById = null;


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
    PublicMethed.printAddress(testNetAccountKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
    //Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(testNetAccountAddress,10000000L,
    //5,1,testNetAccountKey,blockingStubFull));
    /*    Assert.assertTrue(PublicMethed.buyStorage(50000000L,testNetAccountAddress,
    testNetAccountKey,
       blockingStubFull));*/

  }

  @Test(enabled = false)
  public void deployErc721CryptoKitties() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(testNetAccountAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    //Long storageUsage = accountResource.getStorageUsed();
    Account account = PublicMethed.queryAccount(testNetAccountKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));
    Long maxFeeLimit = 3900000000L;
    String contractName = "Fomo3D";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetFomo3D_deployErc721CryptoKitties");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetFomo3D_deployErc721CryptoKitties");
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testNetAccountKey, testNetAccountAddress, blockingStubFull);

    String code1 = Configuration.getByPath("testng.conf")
        .getString("code.code1_TestNetFomo3D_deployErc721CryptoKitties");
    String abi1 = Configuration.getByPath("testng.conf")
        .getString("abi.abi1_TestNetFomo3D_deployErc721CryptoKitties");
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi1,
        code1, "", maxFeeLimit, 0L, 100, null,
        testNetAccountKey, testNetAccountAddress, blockingStubFull);

    final SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    accountResource = PublicMethed.getAccountResource(testNetAccountAddress, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getCpuUsed();
    //storageUsage = accountResource.getStorageUsed();
    account = PublicMethed.queryAccount(testNetAccountKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));
    //Assert.assertTrue(storageUsage > 0);
    //Assert.assertTrue(storageLimit > 0);
    Assert.assertTrue(cpuLimit > 0);
    Assert.assertTrue(cpuUsage > 0);

    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    //logger.info(smartContract.getName());
    //logger.info(smartContract.getAbi().toString());

  }

  @Test(enabled = false)
  public void tooLargeStorage() throws IOException {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(testNetAccountAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Account account = PublicMethed.queryAccount(testNetAccountKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    Long maxFeeLimit = 100000000000000000L;
    String contractName = "tooLargeStorage";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestNetFomo3D_tooLargeStorage");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestNetFomo3D_tooLargeStorage");
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi,
        code, "", maxFeeLimit, 0L, 100, null,
        testNetAccountKey, testNetAccountAddress, blockingStubFull);
    infoById = getTransactionInfoById(txid, blockingStubFull);
    accountResource = PublicMethed.getAccountResource(testNetAccountAddress, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    //storageLimit = accountResource.getStorageLimit();
    cpuUsage = accountResource.getCpuUsed();
    //storageUsage = accountResource.getStorageUsed();
    account = PublicMethed.queryAccount(testNetAccountKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));

    /*    String name = readFromXieChang();*/
    String stringTimes = Integer.toString(7);
    byte[] contractAddress = infoById.get().getContractAddress().toByteArray();
    txid = PublicMethed.triggerContract(contractAddress, "slice(uint256)", stringTimes, false,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    logger.info("slice  " + txid);
    logger.info(Integer.toString(infoById.get().getResultValue()));
    infoById = getTransactionInfoById(txid, blockingStubFull);

    txid = PublicMethed.triggerContract(contractAddress, "s()", "#", false,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    logger.info(txid);
    logger.info(Integer.toString(infoById.get().getResultValue()));


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

  /**
   * constructor.
   */

  public String readFromXieChang() throws IOException {
    File file = new File(
        "/Users/wangzihe/Desktop/ddd.txt");
    FileReader reader = null;
    try {
      reader = new FileReader(file);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    BufferedReader reAder = new BufferedReader(reader);
    StringBuilder sb = new StringBuilder();
    String s = "";
    while ((s = reAder.readLine()) != null) {
      sb.append(s);
    }

    String code = sb.toString();
    reAder.close();
    return code;
  }
}


