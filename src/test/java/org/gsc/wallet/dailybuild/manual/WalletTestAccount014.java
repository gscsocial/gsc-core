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

import com.google.protobuf.ByteString;
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
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;

@Slf4j
public class WalletTestAccount014 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private ManagedChannel channelSoliInFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubSoliInFull = null;

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");


  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(1);


  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account014Address = ecKey1.getAddress();
  String account014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] account014SecondAddress = ecKey2.getAddress();
  String account014SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
    PublicMethed.printAddress(testKey002);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext(true)
        .build();
    blockingStubSoliInFull = WalletConfirmedGrpc.newBlockingStub(channelSoliInFull);
  }

  @Test(enabled = true, description = "Query freeNetUsage in 50061")
  public void fullAndSoliMerged1ForFreeNetUsage() {
    //Create account014
    ecKey1 = new ECKey(Utils.getRandom());
    account014Address = ecKey1.getAddress();
    account014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    ecKey2 = new ECKey(Utils.getRandom());
    account014SecondAddress = ecKey2.getAddress();
    account014SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.printAddress(account014Key);
    PublicMethed.printAddress(account014SecondKey);
    Assert.assertTrue(PublicMethed.sendcoin(account014Address, 1000000000L, fromAddress,
        testKey002, blockingStubFull));

    //Test freeNetUsage in fullnode and confirmednode.
    Assert.assertTrue(PublicMethed.sendcoin(account014SecondAddress, 5000000L,
        account014Address, account014Key,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(account014SecondAddress, 5000000L,
        account014Address, account014Key,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account account014 = PublicMethed.queryAccount(account014Address, blockingStubFull);
    final long freeNetUsageInFullnode = account014.getFreeNetUsage();
    final long createTimeInFullnode = account014.getCreateTime();
    final long lastOperationTimeInFullnode = account014.getLatestOprationTime();
    final long lastCustomeFreeTimeInFullnode = account014.getLatestConsumeFreeTime();
    PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
    account014 = PublicMethed.queryAccount(account014Address, blockingStubSoliInFull);
    final long freeNetUsageInSoliInFull = account014.getFreeNetUsage();
    final long createTimeInSoliInFull = account014.getCreateTime();
    final long lastOperationTimeInSoliInFull = account014.getLatestOprationTime();
    final long lastCustomeFreeTimeInSoliInFull = account014.getLatestConsumeFreeTime();
    //PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull,blockingStubConfirmed);
    account014 = PublicMethed.queryAccount(account014Address, blockingStubConfirmed);
    final long freeNetUsageInConfirmed = account014.getFreeNetUsage();
    final long createTimeInConfirmed = account014.getCreateTime();
    final long lastOperationTimeInConfirmed = account014.getLatestOprationTime();
    final long lastCustomeFreeTimeInConfirmed = account014.getLatestConsumeFreeTime();
    Assert.assertTrue(freeNetUsageInSoliInFull > 0 && freeNetUsageInConfirmed > 0
        && freeNetUsageInFullnode > 0);
    Assert.assertTrue(freeNetUsageInFullnode <= freeNetUsageInSoliInFull + 5
        && freeNetUsageInFullnode >= freeNetUsageInSoliInFull - 5);
    Assert.assertTrue(freeNetUsageInFullnode <= freeNetUsageInConfirmed + 5
        && freeNetUsageInFullnode >= freeNetUsageInConfirmed - 5);
    Assert.assertTrue(createTimeInFullnode == createTimeInConfirmed && createTimeInFullnode
        == createTimeInSoliInFull);
    Assert.assertTrue(createTimeInSoliInFull != 0);
    Assert.assertTrue(lastOperationTimeInFullnode == lastOperationTimeInConfirmed
        && lastOperationTimeInFullnode == lastOperationTimeInSoliInFull);
    Assert.assertTrue(lastOperationTimeInSoliInFull != 0);
    Assert.assertTrue(lastCustomeFreeTimeInFullnode == lastCustomeFreeTimeInConfirmed
        && lastCustomeFreeTimeInFullnode == lastCustomeFreeTimeInSoliInFull);
    Assert.assertTrue(lastCustomeFreeTimeInSoliInFull != 0);
  }

  @Test(enabled = true, description = "Query net usage in 50061")
  public void fullAndSoliMerged2ForNetUsage() {

    Assert.assertTrue(PublicMethed.freezeBalance(account014Address, 1000000L, 5,
        account014Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(account014SecondAddress, 1000000L,
        account014Address, account014Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(account014Address, 1000000,
        5, 1, account014Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014Address, 1000000,
        5, 0, ByteString.copyFrom(
            account014SecondAddress), account014Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014Address, 1000000,
        5, 1, ByteString.copyFrom(
            account014SecondAddress), account014Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014SecondAddress, 1000000,
        5, 0, ByteString.copyFrom(
            account014Address), account014SecondKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014SecondAddress, 1000000,
        5, 1, ByteString.copyFrom(
            account014Address), account014SecondKey, blockingStubFull));

    PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
    Account account014 = PublicMethed.queryAccount(account014Address, blockingStubFull);
    final long lastCustomeTimeInFullnode = account014.getLatestConsumeTime();
    final long netUsageInFullnode = account014.getNetUsage();
    final long acquiredForNetInFullnode = account014
        .getAcquiredDelegatedFrozenBalanceForNet();
    final long delegatedNetInFullnode = account014.getDelegatedFrozenBalanceForNet();
    final long acquiredForCpuInFullnode = account014
        .getAccountResource().getAcquiredDelegatedFrozenBalanceForCpu();
    final long delegatedForCpuInFullnode = account014
        .getAccountResource().getDelegatedFrozenBalanceForCpu();
    logger.info("delegatedForCpuInFullnode " + delegatedForCpuInFullnode);
    PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
    account014 = PublicMethed.queryAccount(account014Address, blockingStubSoliInFull);
    final long lastCustomeTimeInSoliInFull = account014.getLatestConsumeTime();
    logger.info("freeNetUsageInSoliInFull " + lastCustomeTimeInSoliInFull);
    final long netUsageInSoliInFull = account014.getNetUsage();
    final long acquiredForNetInSoliInFull = account014
        .getAcquiredDelegatedFrozenBalanceForNet();
    final long delegatedNetInSoliInFull = account014.getDelegatedFrozenBalanceForNet();
    final long acquiredForCpuInSoliInFull = account014
        .getAccountResource().getAcquiredDelegatedFrozenBalanceForCpu();
    final long delegatedForCpuInSoliInFull = account014
        .getAccountResource().getDelegatedFrozenBalanceForCpu();
    logger.info("delegatedForCpuInSoliInFull " + delegatedForCpuInSoliInFull);
    //PublicMethed.waitConfirmedNodeSynFullNodeData(blockingStubFull,blockingStubConfirmed);
    account014 = PublicMethed.queryAccount(account014Address, blockingStubConfirmed);
    final long netUsageInConfirmed = account014.getNetUsage();
    final long lastCustomeTimeInConfirmed = account014.getLatestConsumeTime();
    final long acquiredForNetInConfirmed = account014
        .getAcquiredDelegatedFrozenBalanceForNet();
    final long delegatedNetInConfirmed = account014.getDelegatedFrozenBalanceForNet();
    final long acquiredForCpuInConfirmed = account014.getAccountResource()
        .getAcquiredDelegatedFrozenBalanceForCpu();
    final long delegatedForCpuInConfirmed = account014.getAccountResource()
        .getDelegatedFrozenBalanceForCpu();

    logger.info("delegatedForCpuInConfirmed " + delegatedForCpuInConfirmed);
    Assert.assertTrue(netUsageInSoliInFull > 0 && netUsageInConfirmed > 0
        && netUsageInFullnode > 0);
    Assert.assertTrue(netUsageInFullnode <= netUsageInSoliInFull + 5
        && netUsageInFullnode >= netUsageInSoliInFull - 5);
    Assert.assertTrue(netUsageInFullnode <= netUsageInConfirmed + 5
        && netUsageInFullnode >= netUsageInConfirmed - 5);
    Assert.assertTrue(acquiredForNetInFullnode == acquiredForNetInSoliInFull
        && acquiredForNetInFullnode == acquiredForNetInConfirmed);
    Assert.assertTrue(delegatedNetInFullnode == delegatedNetInSoliInFull
        && delegatedNetInFullnode == delegatedNetInConfirmed);
    Assert.assertTrue(acquiredForCpuInFullnode == acquiredForCpuInSoliInFull
        && acquiredForCpuInFullnode == acquiredForCpuInConfirmed);
    Assert.assertTrue(delegatedForCpuInFullnode == delegatedForCpuInSoliInFull
        && delegatedForCpuInFullnode == delegatedForCpuInConfirmed);
    Assert.assertTrue(acquiredForNetInSoliInFull == 1000000
        && delegatedNetInSoliInFull == 1000000 && acquiredForCpuInSoliInFull == 1000000
        && delegatedForCpuInSoliInFull == 1000000);
    logger.info("lastCustomeTimeInSoliInFull " + lastCustomeTimeInSoliInFull);
    Assert.assertTrue(lastCustomeTimeInFullnode == lastCustomeTimeInConfirmed
        && lastCustomeTimeInFullnode == lastCustomeTimeInSoliInFull);
    logger.info("lastCustomeTimeInSoliInFull " + lastCustomeTimeInSoliInFull);
    Assert.assertTrue(lastCustomeTimeInSoliInFull != 0);

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