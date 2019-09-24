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

package org.gsc.core.witness;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gsc.core.wrapper.ProposalWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.testng.collections.Lists;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.DynamicPropertiesStore;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.Proposal;
import org.gsc.protos.Protocol.Proposal.State;

public class ProposalControllerTest {

  private static Manager dbManager = new Manager();
  private static GSCApplicationContext context;
  private static String dbPath = "db_proposal_controller_test";
  private static ProposalController proposalController;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    proposalController = ProposalController
        .createInstance(dbManager);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testSetDynamicParameters() {

    ProposalWrapper proposalWrapper = new ProposalWrapper(
        Proposal.newBuilder().build());
    Map<Long, Long> parameters = new HashMap<>();
    DynamicPropertiesStore dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    long accountUpgradeCostDefault = dynamicPropertiesStore.getAccountUpgradeCost();
    long createAccountFeeDefault = dynamicPropertiesStore.getCreateAccountFee();
    long transactionFeeDefault = dynamicPropertiesStore.getTransactionFee();
    parameters.put(1L, accountUpgradeCostDefault + 1);
    parameters.put(2L, createAccountFeeDefault + 1);
    parameters.put(3L, transactionFeeDefault + 1);
    proposalWrapper.setParameters(parameters);

    proposalController.setDynamicParameters(proposalWrapper);
    Assert.assertEquals(accountUpgradeCostDefault + 1,
        dynamicPropertiesStore.getAccountUpgradeCost());
    Assert.assertEquals(createAccountFeeDefault + 1, dynamicPropertiesStore.getCreateAccountFee());
    Assert.assertEquals(transactionFeeDefault + 1, dynamicPropertiesStore.getTransactionFee());

  }

  @Test
  public void testProcessProposal() {
    ProposalWrapper proposalWrapper = new ProposalWrapper(
        Proposal.newBuilder().build());
    proposalWrapper.setState(State.PENDING);
    proposalWrapper.setID(1);

    byte[] key = proposalWrapper.createDbKey();
    dbManager.getProposalStore().put(key, proposalWrapper);

    proposalController.processProposal(proposalWrapper);

    try {
      proposalWrapper = dbManager.getProposalStore().get(key);
    } catch (Exception ex) {
    }
    Assert.assertEquals(State.DISAPPROVED, proposalWrapper.getState());

    proposalWrapper.setState(State.PENDING);
    dbManager.getProposalStore().put(key, proposalWrapper);
    for (int i = 0; i < 17; i++) {
      proposalWrapper.addApproval(ByteString.copyFrom(new byte[i]));
    }

    proposalController.processProposal(proposalWrapper);

    try {
      proposalWrapper = dbManager.getProposalStore().get(key);
    } catch (Exception ex) {
    }
    Assert.assertEquals(State.DISAPPROVED, proposalWrapper.getState());

    List<ByteString> activeWitnesses = Lists.newArrayList();
    String prefix = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1a";
    for (int i = 0; i < 27; i++) {
      activeWitnesses
          .add(ByteString.copyFrom(ByteArray.fromHexString(prefix + (i >= 10 ? i : "0" + i))));
    }
    for (int i = 0; i < 18; i++) {
      proposalWrapper.addApproval(
          ByteString.copyFrom(ByteArray.fromHexString(prefix + (i >= 10 ? i : "0" + i))));
    }
    dbManager.getWitnessScheduleStore().saveActiveWitnesses(activeWitnesses);
    proposalWrapper.setState(State.PENDING);
    dbManager.getProposalStore().put(key, proposalWrapper);
    proposalController.processProposal(proposalWrapper);

    try {
      proposalWrapper = dbManager.getProposalStore().get(key);
    } catch (Exception ex) {
    }
    Assert.assertEquals(State.APPROVED, proposalWrapper.getState());
  }


  @Test
  public void testProcessProposals() {
    ProposalWrapper proposalWrapper1 = new ProposalWrapper(
        Proposal.newBuilder().build());
    proposalWrapper1.setState(State.APPROVED);
    proposalWrapper1.setID(1);

    ProposalWrapper proposalWrapper2 = new ProposalWrapper(
        Proposal.newBuilder().build());
    proposalWrapper2.setState(State.DISAPPROVED);
    proposalWrapper2.setID(2);

    ProposalWrapper proposalWrapper3 = new ProposalWrapper(
        Proposal.newBuilder().build());
    proposalWrapper3.setState(State.PENDING);
    proposalWrapper3.setID(3);
    proposalWrapper3.setExpirationTime(10000L);

    ProposalWrapper proposalWrapper4 = new ProposalWrapper(
        Proposal.newBuilder().build());
    proposalWrapper4.setState(State.CANCELED);
    proposalWrapper4.setID(4);
    proposalWrapper4.setExpirationTime(11000L);

    ProposalWrapper proposalWrapper5 = new ProposalWrapper(
        Proposal.newBuilder().build());
    proposalWrapper5.setState(State.PENDING);
    proposalWrapper5.setID(5);
    proposalWrapper5.setExpirationTime(12000L);

    dbManager.getDynamicPropertiesStore().saveLatestProposalNum(5);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(10000L);
    dbManager.getProposalStore().put(proposalWrapper1.createDbKey(), proposalWrapper1);
    dbManager.getProposalStore().put(proposalWrapper2.createDbKey(), proposalWrapper2);
    dbManager.getProposalStore().put(proposalWrapper3.createDbKey(), proposalWrapper3);
    dbManager.getProposalStore().put(proposalWrapper4.createDbKey(), proposalWrapper4);
    dbManager.getProposalStore().put(proposalWrapper5.createDbKey(), proposalWrapper5);

    proposalController.processProposals();

    try {
      proposalWrapper3 = dbManager.getProposalStore().get(proposalWrapper3.createDbKey());
    } catch (Exception ex) {
    }
    Assert.assertEquals(State.DISAPPROVED, proposalWrapper3.getState());

  }

  @Test
  public void testHasMostApprovals() {
    ProposalWrapper proposalWrapper = new ProposalWrapper(
        Proposal.newBuilder().build());
    proposalWrapper.setState(State.APPROVED);
    proposalWrapper.setID(1);

    List<ByteString> activeWitnesses = Lists.newArrayList();
    for (int i = 0; i < 27; i++) {
      activeWitnesses.add(ByteString.copyFrom(new byte[]{(byte) i}));
    }
    for (int i = 0; i < 18; i++) {
      proposalWrapper.addApproval(ByteString.copyFrom(new byte[]{(byte) i}));
    }

    Assert.assertEquals(true, proposalWrapper.hasMostApprovals(activeWitnesses));

    proposalWrapper.clearApproval();
    for (int i = 1; i < 18; i++) {
      proposalWrapper.addApproval(ByteString.copyFrom(new byte[]{(byte) i}));
    }

    activeWitnesses.clear();
    for (int i = 0; i < 5; i++) {
      activeWitnesses.add(ByteString.copyFrom(new byte[]{(byte) i}));
    }
    proposalWrapper.clearApproval();
    for (int i = 0; i < 3; i++) {
      proposalWrapper.addApproval(ByteString.copyFrom(new byte[]{(byte) i}));
    }
    Assert.assertEquals(true, proposalWrapper.hasMostApprovals(activeWitnesses));


  }


}
