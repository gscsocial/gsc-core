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

package org.gsc.core.operator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Proposal.State;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j

public class ProposalDeleteOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_ProposalApprove_test";
  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String URL = "https://gsc.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void initTest() {
    WitnessWrapper ownerWitnessFirstWrapper =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            10_000_000L,
            URL);
    AccountWrapper ownerAccountFirstWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            300_000_000L);
    AccountWrapper ownerAccountSecondWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstWrapper.getAddress().toByteArray(), ownerAccountFirstWrapper);
    dbManager.getAccountStore()
        .put(ownerAccountSecondWrapper.getAddress().toByteArray(), ownerAccountSecondWrapper);

    dbManager.getWitnessStore().put(ownerWitnessFirstWrapper.getAddress().toByteArray(),
        ownerWitnessFirstWrapper);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().saveLatestProposalNum(0);

    long id = 1;
    dbManager.getProposalStore().delete(ByteArray.fromLong(1));
    dbManager.getProposalStore().delete(ByteArray.fromLong(2));
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 3 * 27 * 1000L);
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      ProposalWrapper proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalWrapper);
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 1);
      Assert.assertEquals(proposalWrapper.getApprovals().size(), 0);
      Assert.assertEquals(proposalWrapper.getCreateTime(), 1000000);
      Assert.assertEquals(proposalWrapper.getExpirationTime(),
          261200000); // 2000000 + 3 * 4 * 21600000
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  private Any getContract(String address, HashMap<Long, Long> paras) {
    return Any.pack(
        Contract.ProposalCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .putAllParameters(paras)
            .build());
  }

  private Any getContract(String address, long id) {
    return Any.pack(
        Contract.ProposalDeleteContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setProposalId(id)
            .build());
  }

  /**
   * first deleteProposal, result is success.
   */
  @Test
  public void successDeleteApprove() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalDeleteOperator operator = new ProposalDeleteOperator(
        getContract(OWNER_ADDRESS_FIRST, id), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    ProposalWrapper proposalWrapper;
    try {
      proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalWrapper.getState(), State.PENDING);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      try {
        proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      } catch (ItemNotFoundException e) {
        Assert.assertFalse(e instanceof ItemNotFoundException);
        return;
      }
      Assert.assertEquals(proposalWrapper.getState(), State.CANCELED);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalDeleteOperator operator = new ProposalDeleteOperator(
        getContract(OWNER_ADDRESS_INVALID, id), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Account not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalDeleteOperator operator = new ProposalDeleteOperator(
        getContract(OWNER_ADDRESS_NOACCOUNT, id), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal is not proposed by witness, result is failed,exception is "witness not exists".
   */
  @Test
  public void notProposed() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalDeleteOperator operator = new ProposalDeleteOperator(
        getContract(OWNER_ADDRESS_SECOND, id), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("witness[+OWNER_ADDRESS_NOWITNESS+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] " + "is not proposed by "
              + StringUtil.createReadableString(
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND))),
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Proposal not exists, result is failed, exception is "Proposal not exists".
   */
  @Test
  public void noProposal() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 2;

    ProposalDeleteOperator operator = new ProposalDeleteOperator(
        getContract(OWNER_ADDRESS_FIRST, id), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Proposal[" + id + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal expired, result is failed, exception is "Proposal expired".
   */
  @Test
  public void proposalExpired() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(261200100);
    long id = 1;

    ProposalDeleteOperator operator = new ProposalDeleteOperator(
        getContract(OWNER_ADDRESS_FIRST, id), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Proposal[" + id + "] expired");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] expired",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal canceled, result is failed, exception is "Proposal expired".
   */
  @Test
  public void proposalCanceled() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(100100);
    long id = 1;

    ProposalDeleteOperator operator = new ProposalDeleteOperator(
        getContract(OWNER_ADDRESS_FIRST, id), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    ProposalWrapper proposalWrapper;
    try {
      proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      proposalWrapper.setState(State.CANCELED);
      dbManager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalWrapper.getApprovals().size(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail("Proposal[" + id + "] canceled");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] canceled",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}