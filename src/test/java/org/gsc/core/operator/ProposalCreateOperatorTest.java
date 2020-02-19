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
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ProposalWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j

public class ProposalCreateOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_ProposalCreate_test";
  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String URL = "https://gsc.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_BALANCENOTSUFFIENT;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
    OWNER_ADDRESS_BALANCENOTSUFFIENT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e06d4271a1ced";
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
  }

  private Any getContract(String address, HashMap<Long, Long> paras) {
    return Any.pack(
        Contract.ProposalCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .putAllParameters(paras)
            .build());
  }

  /**
   * first createProposal,result is success.
   */
  @Test
  public void successProposalCreate() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 1000000L);
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
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

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 10000L);
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_INVALID, paras), dbManager);
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
   * use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 10000L);
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_NOACCOUNT, paras), dbManager);
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
   * use WitnessStore not exists Address,result is failed,exception is "witness not exists".
   */
  @Test
  public void noWitness() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 10000L);
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_SECOND, paras), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("witness[+OWNER_ADDRESS_NOWITNESS+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + OWNER_ADDRESS_SECOND + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use invalid parameter, result is failed, exception is "Bad chain parameter id".
   */
  @Test
  public void invalidPara() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(31L, 10000L);
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Bad chain parameter id");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Bad chain parameter id",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    paras = new HashMap<>();
    paras.put(3L, 1 + 100_000_000_000_000_000L);
    operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      fail("Bad chain parameter value,valid range is [0,100_000_000_000_000_000L]");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Bad chain parameter value,valid range is [0,100_000_000_000_000_000L]",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    paras = new HashMap<>();
    paras.put(10L, -1L);
    operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(-1);
    try {
      operator.validate();
      fail("This proposal has been executed before and is only allowed to be executed once");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "This proposal has been executed before and is only allowed to be executed once",
          e.getMessage());
    }

    paras.put(10L, -1L);
    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0);
    operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0);
    try {
      operator.validate();
      fail("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1",
          e.getMessage());
    }
  }

  /**
   * parameter size = 0 , result is failed, exception is "This proposal has no parameter.".
   */
  @Test
  public void emptyProposal() {
    HashMap<Long, Long> paras = new HashMap<>();
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("This proposal has no parameter");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This proposal has no parameter.",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void InvalidParaValue() {
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(10L, 1000L);
    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /*
   * two same proposal can work
   */
  @Test
  public void duplicateProposalCreateSame() {
    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0L);

    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 23 * 3600 * 1000L);
    paras.put(1L, 8_888_000_000L);
    paras.put(2L, 200_000L);
    paras.put(3L, 20L);
    paras.put(4L, 2048_000_000L);
    paras.put(5L, 64_000_000L);
    paras.put(6L, 64_000_000L);
    paras.put(7L, 64_000_000L);
    paras.put(8L, 64_000_000L);
    paras.put(9L, 1L);
    paras.put(10L, 1L);
    paras.put(11L, 64L);
    paras.put(12L, 64L);
    paras.put(13L, 64L);

    ProposalCreateOperator operator =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
    ProposalCreateOperator operatorSecond =
        new ProposalCreateOperator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);

    dbManager.getDynamicPropertiesStore().saveLatestProposalNum(0L);
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 0);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      operatorSecond.validate();
      operatorSecond.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 2L);
      ProposalWrapper proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(2L));
      Assert.assertNotNull(proposalWrapper);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
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