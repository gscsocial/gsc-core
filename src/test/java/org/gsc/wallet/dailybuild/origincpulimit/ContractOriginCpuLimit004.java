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

package org.gsc.wallet.dailybuild.origincpulimit;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
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
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractOriginCpuLimit004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  private long getAvailableFrozenCpu(byte[] accountAddress) {
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(accountAddress,
        blockingStubFull);
    long cpuLimit = resourceInfo.getCpuLimit();
    long cpuUsed = resourceInfo.getCpuUsed();
    return cpuLimit - cpuUsed;
  }

  private long getUserAvailableCpu(byte[] userAddress) {
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(userAddress,
        blockingStubFull);
    Account info = PublicMethed.queryAccount(userAddress, blockingStubFull);
    long balance = info.getBalance();
    long cpuLimit = resourceInfo.getCpuLimit();
    long userAvaliableFrozenCpu = getAvailableFrozenCpu(userAddress);
    return balance / 100 + userAvaliableFrozenCpu;
  }

  private long getFeeLimit(String txid) {
    Optional<Transaction> trsById = PublicMethed.getTransactionById(txid, blockingStubFull);
    return trsById.get().getRawData().getFeeLimit();
  }

  private long getUserMax(byte[] userAddress, long feelimit) {
    logger.info("User feeLimit: " + feelimit / 100);
    logger.info("User UserAvaliableCpu: " + getUserAvailableCpu(userAddress));
    return Math.min(feelimit / 100, getUserAvailableCpu(userAddress));
  }

  private long getOriginalCpuLimit(byte[] contractAddress) {
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    return smartContract.getOriginCpuLimit();
  }

  private long getConsumeUserResourcePercent(byte[] contractAddress) {
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    return smartContract.getConsumeUserResourcePercent();
  }

  private long getDevMax(byte[] devAddress, byte[] userAddress, long feeLimit,
      byte[] contractAddress) {
    long devMax = Math.min(getAvailableFrozenCpu(devAddress),
        getOriginalCpuLimit(contractAddress));
    long p = getConsumeUserResourcePercent(contractAddress);
    if (p != 0) {
      logger.info("p: " + p);
      devMax = Math.min(devMax, getUserMax(userAddress, feeLimit) * (100 - p) / p);
      logger.info("Dev byUserPercent: " + getUserMax(userAddress, feeLimit) * (100 - p) / p);
    }
    logger.info("Dev AvaliableFrozenCpu: " + getAvailableFrozenCpu(devAddress));
    logger.info("Dev OriginalCpuLimit: " + getOriginalCpuLimit(contractAddress));
    return devMax;
  }

  @Test(enabled = true, description = "Contract use Origin_cpu_limit")
  public void testOriginCpuLimit() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 1000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    // A2B1

    //dev balance and Cpu
    long devTargetBalance = 10_000_000;
    long devTargetCpu = 70000;

    // deploy contract parameters
    final long deployFeeLimit = maxFeeLimit;
    final long consumeUserResourcePercent = 0;
    final long originCpuLimit = 1000;

    //dev balance and Cpu
    final long devTriggerTargetBalance = 0;
    final long devTriggerTargetCpu = 592;

    // user balance and Cpu
    final long userTargetBalance = 0;
    final long userTargetCpu = 2000L;

    // trigger contract parameter, maxFeeLimit 10000000
    final long triggerFeeLimit = maxFeeLimit;
    final boolean expectRet = true;

    // count dev cpu, balance
    long devFreezeBalanceSun = PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key,
        devTargetCpu, blockingStubFull);

    long devNeedBalance = devTargetBalance + devFreezeBalanceSun;

    logger.info("need balance:" + devNeedBalance);

    // get balance
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, devNeedBalance, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    // get cpu
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(dev001Address, devFreezeBalanceSun,
        5, 1, dev001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devCpuLimitBefore = accountResource.getCpuLimit();
    long devCpuUsageBefore = accountResource.getCpuUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("before deploy, dev cpu limit is " + Long.toString(devCpuLimitBefore));
    logger.info("before deploy, dev cpu usage is " + Long.toString(devCpuUsageBefore));
    logger.info("before deploy, dev balance is " + Long.toString(devBalanceBefore));

    String filePath = "src/test/resources/soliditycode_v0.5.4/contractOriginCpuLimit004.sol";
    String contractName = "findArgsContractTest";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    final String deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            deployFeeLimit, 0L, consumeUserResourcePercent, originCpuLimit, "0",
            0, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devCpuLimitAfter = accountResource.getCpuLimit();
    long devCpuUsageAfter = accountResource.getCpuUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after deploy, dev cpu limit is " + Long.toString(devCpuLimitAfter));
    logger.info("after deploy, dev cpu usage is " + Long.toString(devCpuUsageAfter));
    logger.info("after deploy, dev balance is " + Long.toString(devBalanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);

    ByteString contractAddressString = infoById.get().getContractAddress();
    contractAddress = contractAddressString.toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);

    Assert.assertTrue(smartContract.getAbi() != null);

    Assert.assertTrue(devCpuLimitAfter > 0);
    Assert.assertTrue(devCpuUsageAfter > 0);
    Assert.assertEquals(devBalanceBefore, devBalanceAfter);

    // count dev cpu, balance
    devFreezeBalanceSun = PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key,
        devTriggerTargetCpu, blockingStubFull);

    devNeedBalance = devTriggerTargetBalance + devFreezeBalanceSun;
    logger.info("dev need  balance:" + devNeedBalance);

    // count user cpu, balance
    long userFreezeBalanceSun = PublicMethed.getFreezeBalanceCount(user001Address, user001Key,
        userTargetCpu, blockingStubFull);

    long userNeedBalance = userTargetBalance + userFreezeBalanceSun;

    logger.info("User need  balance:" + userNeedBalance);

    // get balance
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, devNeedBalance, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, userNeedBalance, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // get cpu
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(dev001Address, devFreezeBalanceSun,
        5, 1, dev001Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(user001Address, userFreezeBalanceSun,
        5, 1, user001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    devCpuLimitBefore = accountResource.getCpuLimit();
    devCpuUsageBefore = accountResource.getCpuUsed();
    devBalanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("before trigger, dev devCpuLimitBefore is "
        + Long.toString(devCpuLimitBefore));
    logger.info("before trigger, dev devCpuUsageBefore is "
        + Long.toString(devCpuUsageBefore));
    logger.info("before trigger, dev devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitBefore = accountResource.getCpuLimit();
    long userCpuUsageBefore = accountResource.getCpuUsed();
    long userBalanceBefore = PublicMethed.queryAccount(
        user001Address, blockingStubFull).getBalance();

    logger.info("before trigger, user userCpuLimitBefore is "
        + Long.toString(userCpuLimitBefore));
    logger.info("before trigger, user userCpuUsageBefore is "
        + Long.toString(userCpuUsageBefore));
    logger.info("before trigger, user userBalanceBefore is " + Long.toString(userBalanceBefore));

    logger.info("==================================");
    long userMax = getUserMax(user001Address, triggerFeeLimit);
    long devMax = getDevMax(dev001Address, user001Address, triggerFeeLimit, contractAddress);

    logger.info("userMax: " + userMax);
    logger.info("devMax: " + devMax);
    logger.info("==================================");

    String param = "\"" + 0 + "\"";
    final String triggerTxid = PublicMethed
        .triggerContract(contractAddress, "findArgsByIndexTest(uint256)",
            param, false, 0, triggerFeeLimit,
            user001Address, user001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    devCpuLimitAfter = accountResource.getCpuLimit();
    devCpuUsageAfter = accountResource.getCpuUsed();
    devBalanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after trigger, dev devCpuLimitAfter is " + Long.toString(devCpuLimitAfter));
    logger.info("after trigger, dev devCpuUsageAfter is " + Long.toString(devCpuUsageAfter));
    logger.info("after trigger, dev devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitAfter = accountResource.getCpuLimit();
    long userCpuUsageAfter = accountResource.getCpuUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address,
        blockingStubFull).getBalance();

    logger.info("after trigger, user userCpuLimitAfter is "
        + Long.toString(userCpuLimitAfter));
    logger.info("after trigger, user userCpuUsageAfter is "
        + Long.toString(userCpuUsageAfter));
    logger.info("after trigger, user userBalanceAfter is " + Long.toString(userBalanceAfter));

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);
    boolean isSuccess = true;
    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      logger.info("transaction failed with message: " + infoById.get().getResMessage());
      isSuccess = false;
    }

    long fee = infoById.get().getFee();
    long cpuFee = infoById.get().getReceipt().getCpuFee();
    long cpuUsage = infoById.get().getReceipt().getCpuUsage();
    long originCpuUsage = infoById.get().getReceipt().getOriginCpuUsage();
    long cpuTotalUsage = infoById.get().getReceipt().getCpuUsageTotal();
    long netUsage = infoById.get().getReceipt().getNetUsage();
    long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee: " + fee);
    logger.info("cpuFee: " + cpuFee);
    logger.info("cpuUsage: " + cpuUsage);
    logger.info("originCpuUsage: " + originCpuUsage);
    logger.info("cpuTotalUsage: " + cpuTotalUsage);
    logger.info("netUsage: " + netUsage);
    logger.info("netFee: " + netFee);

    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    long consumeUserPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeUserPercent);

    long devExpectCost = cpuTotalUsage * (100 - consumeUserPercent) / 100;
    long userExpectCost = cpuTotalUsage - devExpectCost;
    final long totalCost = devExpectCost + userExpectCost;

    logger.info("devExpectCost: " + devExpectCost);
    logger.info("userExpectCost: " + userExpectCost);

    Assert.assertTrue(devCpuLimitAfter > 0);
    Assert.assertEquals(devBalanceBefore, devBalanceAfter);

    // dev original is the dev max expense A2B1
    Assert.assertEquals(getOriginalCpuLimit(contractAddress), devMax);

    // DEV is enough to pay
    Assert.assertEquals(originCpuUsage, devExpectCost);
    //    Assert.assertEquals(devCpuUsageAfter,devExpectCost + devCpuUsageBefore);
    // User Cpu is enough to pay");
    Assert.assertEquals(cpuUsage, userExpectCost);
    Assert.assertEquals(userBalanceBefore, userBalanceAfter);
    Assert.assertEquals(userCpuUsageAfter, userCpuUsageBefore);
    Assert.assertEquals(userBalanceBefore, userBalanceAfter);
    Assert.assertEquals(totalCost, cpuTotalUsage);

    if (expectRet) {
      Assert.assertTrue(isSuccess);
    } else {
      Assert.assertFalse(isSuccess);
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
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


