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

package org.gsc.wallet.committee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
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
import org.gsc.core.Wallet;


@Slf4j
public class WalletTestCommittee002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  //Witness 47.93.9.236
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  //Witness 47.93.33.201
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  //Witness 123.56.10.6
  private final String witnessKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  //Wtiness 39.107.80.135
  private final String witnessKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  //Witness 47.93.184.2
  private final String witnessKey005 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");

  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

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

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
  }


  @Test(enabled = true)
  public void testCreateProposalMaintenanceTimeInterval() {
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
    Assert.assertTrue(PublicMethed.sendcoin(witness001Address, 10000000L,
        toAddress, testKey003, blockingStubFull));

    //0:MAINTENANCE_TIME_INTERVAL,[3*27s,24h]
    //Minimum interval
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 81000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum interval
    proposalMap.put(0L, 86400000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum -1 interval, create failed.
    proposalMap.put(0L, 80000L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(0L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003, proposalMap,
        blockingStubFull));
    
    //Maximum + 1 interval
    proposalMap.put(0L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testCreateProposalAccountUpgradeCost() {
    //1:ACCOUNT_UPGRADE_COST,[0,100 000 000 000 000 000]//drop
    //Minimum AccountUpgradeCost
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(1L, 0L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum AccountUpgradeCost
    proposalMap.put(1L, 100000000000000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 AccountUpgradeCost
    proposalMap.put(1L, -1L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 AccountUpgradeCost
    proposalMap.put(1L, 100000000000000001L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(1L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));
  }

  @Test(enabled = true)
  public void testCreateProposalCreateAccountFee() {
    //2:CREATE_ACCOUNT_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum CreateAccountFee
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(2L, 0L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum CreateAccountFee
    proposalMap.put(2L, 100000000000000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 CreateAccountFee
    proposalMap.put(2L, -1L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 CreateAccountFee
    proposalMap.put(2L, 100000000000000001L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(2L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testTransactionFee() {
    //3:TRANSACTION_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum TransactionFee
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(3L, 0L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum TransactionFee
    proposalMap.put(3L, 100000000000000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 TransactionFee
    proposalMap.put(3L, -1L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 TransactionFee
    proposalMap.put(3L, 100000000000000001L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(3L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testAssetIssueFee() {
    //4:ASSET_ISSUE_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum AssetIssueFee
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(4L, 0L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Duplicat proposals
    proposalMap.put(4L, 0L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum AssetIssueFee
    proposalMap.put(4L, 100000000000000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 AssetIssueFee
    proposalMap.put(4L, -1L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 AssetIssueFee
    proposalMap.put(4L, 100000000000000001L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(4L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testWitnessPayPerBlock() {
    //5:WITNESS_PAY_PER_BLOCK,[0,100 000 000 000 000 000]//drop
    //Minimum WitnessPayPerBlock
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(5L, 0L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum WitnessPayPerBlock
    proposalMap.put(5L, 100000000000000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessPayPerBlock
    proposalMap.put(5L, -1L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessPayPerBlock
    proposalMap.put(5L, 100000000000000001L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(5L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testWitnessStandbyAllowance() {
    //6:WITNESS_STANDBY_ALLOWANCE,[0,100 000 000 000 000 000]//drop
    //Minimum WitnessStandbyAllowance
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(6L, 0L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum WitnessStandbyAllowance
    proposalMap.put(6L, 100000000000000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessStandbyAllowance
    proposalMap.put(6L, -1L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessStandbyAllowance
    proposalMap.put(6L, 100000000000000001L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(6L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testInvalidProposals() {
    // The index isn't from 0-9
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(10L, 60L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
            proposalMap, blockingStubFull));

    //The index is -1
    proposalMap.put(-1L, 6L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
            proposalMap, blockingStubFull));
  }

  @Test(enabled = true)
  public void testCreateNewAccountFeeInSystemControl() {

    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(7L, 1L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum WitnessStandbyAllowance
    proposalMap.put(7L, 100000000000000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessStandbyAllowance
    proposalMap.put(6L, -1L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessStandbyAllowance
    proposalMap.put(6L, 100000000000000001L);
    Assert.assertFalse(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(6L, 86400000L);
    Assert.assertFalse(PublicMethed.createProposal(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelConfirmed != null) {
      channelConfirmed.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


