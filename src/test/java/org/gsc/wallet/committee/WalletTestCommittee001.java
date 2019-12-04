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
import java.util.Optional;
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
import org.gsc.api.GrpcAPI.EmptyMessage;
import org.gsc.api.GrpcAPI.PaginatedMessage;
import org.gsc.api.GrpcAPI.ProposalList;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.core.Wallet;


@Slf4j
public class WalletTestCommittee001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
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

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
  }


  @Test
  public void testListProposals() {
    //List proposals
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer beforeProposalCount = listProposals.get().getProposalsCount();

    //CreateProposal
    final long now = System.currentTimeMillis();
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 1000000L);
    PublicMethed.createProposal(witness001Address, witnessKey001, proposalMap, blockingStubFull);

    //List proposals
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    Integer afterProposalCount = listProposals.get().getProposalsCount();
    Assert.assertTrue(beforeProposalCount + 1 == afterProposalCount);
    logger.info(Long.toString(listProposals.get().getProposals(0).getCreateTime()));
    logger.info(Long.toString(now));
    //Assert.assertTrue(listProposals.get().getProposals(0).getCreateTime() >= now);
    Assert.assertTrue(listProposals.get().getProposals(0).getParametersMap().equals(proposalMap));

    //getProposalListPaginated
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(0);
    pageMessageBuilder.setLimit(1);
    ProposalList paginatedProposalList = blockingStubFull
        .getPaginatedProposalList(pageMessageBuilder.build());
    Assert.assertTrue(paginatedProposalList.getProposalsCount() >= 1);
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


