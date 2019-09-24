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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
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
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TestExceptionCodeAndAbi {

  //testng001、testng002、testng003、testng004
  private final String testNetAccountKey =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  //"FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  //"BC70ADC5A0971BA3F7871FBB7249E345D84CE7E5458828BE1E28BF8F98F2795B";
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  private ManagedChannel channelFull2 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull2 = null;

  private ManagedChannel channelFull3 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull3 = null;

  private ManagedChannel channelFull4 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull4 = null;


  private ManagedChannel channelFull5 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull5 = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode2 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode3 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(2);
  private String fullnode4 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(3);
  private String fullnode5 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(4);

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


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
    PublicMethed.printAddress(testKeyForAssetIssue016);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2)
        .usePlaintext(true)
        .build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);

    channelFull3 = ManagedChannelBuilder.forTarget(fullnode3)
        .usePlaintext(true)
        .build();
    blockingStubFull3 = WalletGrpc.newBlockingStub(channelFull3);

    channelFull4 = ManagedChannelBuilder.forTarget(fullnode4)
        .usePlaintext(true)
        .build();
    blockingStubFull4 = WalletGrpc.newBlockingStub(channelFull4);

    channelFull5 = ManagedChannelBuilder.forTarget(fullnode5)
        .usePlaintext(true)
        .build();
    blockingStubFull5 = WalletGrpc.newBlockingStub(channelFull5);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
    PublicMethed.sendcoin(asset016Address, 10000000000000L, testNetAccountAddress,
        testNetAccountKey, blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalance(asset016Address, 1000000L, 5,
        testKeyForAssetIssue016, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(asset016Address, 3000000000L,
        5, 1, testKeyForAssetIssue016, blockingStubFull));
  }

  @Test(enabled = false)
  public void testExceptionCodeAndAbi() {
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
    Long maxFeeLimit = 100000000000L;
    Integer times = 0;
    String txid;
    byte[] contractAddress;
    Optional<TransactionInfo> infoById = null;
    Long cpuTotal;

    while (times++ < 1) {
      String contractName = "Fomo3D";
      String code = "60806040" + getRandomCode(2734) + getRandomCode(1000) + "0029";
      String abi = Configuration.getByPath("testng.conf")
          .getString("abi.abi_TestExceptionCodeAndAbi_testExceptionCodeAndAbi");
      txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
          maxFeeLimit, 0L, 100, null, testNetAccountKey, testNetAccountAddress, blockingStubFull);
      logger.info("createGen0 " + txid);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      cpuTotal = infoById.get().getReceipt().getCpuUsageTotal();
      //writeCsv(code,abi,"", "","",txid,cpuTotal.toString());

    }
    //final SmartContract smartContract = PublicMethed.getContract(contractAddress,
    // blockingStubFull);
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
    //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    //Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    //Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    //logger.info(smartContract.getName());
    //logger.info(smartContract.getAbi().toString());
  }

  @Test(enabled = false)
  public void testtimeout() {
    String txid = "";
    Long cpuTotal;
    String contractName = "timeout";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestExceptionCodeAndAbi_testtimeout");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestExceptionCodeAndAbi_testtimeout");
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit, 0L,
        100, null, testKeyForAssetIssue016, asset016Address, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    Integer triggerNum = 10000000;
    Long cpu;
    Integer times = 0;
    Account info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
    Long beforeBalance;
    Long beforeNetLimit;
    Long beforeFreeNetLimit;
    Long beforeFreeNetUsed;
    Long beforeNetUsed;
    Long beforeCpuLimit;
    Long beforeCpuUsed;

    Long afterBalance;
    Long afterNetLimit;
    Long afterFreeNetLimit;
    Long afterFreeNetUsed;
    Long afterNetUsed;
    Long afterCpuLimit;
    Long afterCpuUsed;

    Long cpuUsed;
    Long netUsed;
    Long cpuFee;
    Long fee;
    Long cpuUsageTotal;
    Long netFee;
    Long minBalance;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(asset016Address,
        blockingStubFull);

    while (times++ < 10000) {
      info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull);
      beforeBalance = info.getBalance();
      beforeCpuLimit = resourceInfo.getCpuLimit();
      beforeCpuUsed = resourceInfo.getCpuUsed();
      beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
      beforeNetLimit = resourceInfo.getNetLimit();
      beforeNetUsed = resourceInfo.getNetUsed();
      beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
      logger.info("beofre free net used:" + Long.toString(beforeFreeNetUsed));

      logger.info("before in 1 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull1).getBalance());
      logger.info("before in 2 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull2).getBalance());
      logger.info("before in 3 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull3).getBalance());
      logger.info("before in 4 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull4).getBalance());
      logger.info("before in 5 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull5).getBalance());

      txid = PublicMethed.triggerContract(contractAddress,
          "testUseCpu(uint256)", triggerNum.toString(), false,
          0, maxFeeLimit, asset016Address, testKeyForAssetIssue016, blockingStubFull);
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull2);
      cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();
      fee = infoById.get().getFee();
      cpuFee = infoById.get().getReceipt().getCpuFee();
      netUsed = infoById.get().getReceipt().getNetUsage();
      cpuUsed = infoById.get().getReceipt().getCpuUsage();
      netFee = infoById.get().getReceipt().getNetFee();

      info = PublicMethed.queryAccount(testKeyForAssetIssue016, blockingStubFull3);
      afterBalance = info.getBalance();
      afterCpuLimit = resourceInfo.getCpuLimit();
      afterCpuUsed = resourceInfo.getCpuUsed();
      afterFreeNetLimit = resourceInfo.getFreeNetLimit();
      afterNetLimit = resourceInfo.getNetLimit();
      afterNetUsed = resourceInfo.getNetUsed();
      minBalance = beforeBalance - afterBalance;
      resourceInfo = PublicMethed.getAccountResource(asset016Address,
          blockingStubFull2);
      afterFreeNetUsed = resourceInfo.getFreeNetUsed();

      serachInfo(txid, blockingStubFull1);
      serachInfo(txid, blockingStubFull2);
      serachInfo(txid, blockingStubFull3);
      serachInfo(txid, blockingStubFull4);
      serachInfo(txid, blockingStubFull5);
      logger.info("after free net used:" + Long.toString(afterFreeNetUsed));

      logger.info("after in 1 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull1).getBalance());
      logger.info("after in 2 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull2).getBalance());
      logger.info("after in 3 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull3).getBalance());
      logger.info("after in 4 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull4).getBalance());
      logger.info("after in 5 the balance is " + PublicMethed.queryAccount(asset016Address,
          blockingStubFull5).getBalance());

      writeCsv(minBalance.toString(), beforeBalance.toString(), beforeNetLimit.toString(),
          beforeFreeNetLimit.toString(), beforeNetUsed.toString(),
          beforeCpuLimit.toString(), beforeCpuUsed.toString(), beforeFreeNetUsed.toString(),
          cpuUsageTotal.toString(), fee.toString(), cpuFee.toString(),
          netUsed.toString(), cpuUsed.toString(), netFee.toString(), afterBalance.toString(),
          afterCpuLimit.toString(), afterCpuUsed.toString(), afterFreeNetUsed.toString(),
          afterFreeNetLimit.toString(), afterNetLimit.toString(), afterNetUsed.toString(), txid,
          testKeyForAssetIssue016);
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

  /**
   * constructor.
   */

  public static void serachInfo(String txid, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid,
        blockingStubFull);
    logger.info("---------------------------------------------");

    logger.info("fee is " + infoById.get().getFee());
    logger.info("Cpu fee is " + infoById.get().getReceipt().getCpuFee());
    logger.info("Total cpu is " + infoById.get().getReceipt().getCpuUsageTotal());
    logger.info("Cpu used is " + infoById.get().getReceipt().getCpuUsage());
    logger.info("Net used is " + infoById.get().getReceipt().getNetUsage());
    logger.info("Net fee is " + infoById.get().getReceipt().getNetFee());

  }


  /**
   * constructor.
   */

  public static String getRandomCode(int length) {
    String str = "0123456789";
    Random random = new Random();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < length; i++) {
      int number = random.nextInt(10);
      sb.append(str.charAt(number));
    }
    return sb.toString();
  }


  /**
   * constructor.
   */

  public static void writeCsv(String minBalance, String beforeBalance, String beforeNetLimit,
      String beforeFreeNet, String beforeNetUsed, String beforeCpuLimit, String beforeCpuUsed,
      String beforeFreeNetUsed, String cpuUsageTotal, String fee, String cpuFee,
      String netUsed, String cpuUsed,
      String netFee, String afterBalance, String afterCpuLimit, String afterCpuUsed,
      String afterFreeNetUsed, String afterFreeNet, String afterNetLimit,
      String afterNetUsed, String txid, String testKeyForAssetIssue016) {
    try {
      File csv = new File("/Users/wangzihe/Documents/costFee.csv");
      String time = Long.toString(System.currentTimeMillis());
      BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
      bw.write("TestTimeoutusefeelimit:(HaveCpuAndHaveFreezeNet)" + "," + "timestamp" + time
          + "," + "min:" + minBalance + "," + "beforeBalance:" + beforeBalance + ","
          + "beforeNetLimit:"
          + beforeNetLimit + "," + "beforeFreeNet:" + beforeFreeNet + "," + "beforeNetused:"
          + beforeNetUsed
          + "," + "beforeCpuLimit:" + beforeCpuLimit + "," + "beforeCpuUsed:"
          + beforeCpuUsed
          + "," + "beforeFreeNetUsed" + beforeFreeNetUsed + "," + "cpuUsageTotal:"
          + cpuUsageTotal
          + "," + "fee:" + fee + "," + "cpuFee:" + cpuFee + "," + "netUsed:" + netUsed + ","
          + "cpuUsed:" + cpuUsed + "," + "netFee:" + netFee + "," + "afterBalance:"
          + afterBalance + "," + "afterCpuLimit:" + afterCpuLimit + ","
          + "afterCpuUsed:" + afterCpuUsed + "," + "afterFreeNetUsed:" + afterFreeNetUsed
          + "," + "afterFreeNet:" + afterFreeNet + "," + "afterNetLimit:" + afterNetLimit + ","
          + "afterNetUsed:" + afterNetUsed + "," + txid + "," + testKeyForAssetIssue016);
      bw.newLine();
      bw.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}



