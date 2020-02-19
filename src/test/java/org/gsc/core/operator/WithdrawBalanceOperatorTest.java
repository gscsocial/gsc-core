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
import lombok.extern.slf4j.Slf4j;
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
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.config.args.Witness;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class WithdrawBalanceOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "db_withdraw_balance_test";
  private static GSCApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long allowance = 32_000_000L;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //    Args.setParam(new String[]{"--db-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
  }
  /**
   * create temp Wrapper test need.
   */
  @Before
  public void createAccountWrapper() {
    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerWrapper.createDbKey(), ownerWrapper);
  }

  private Any getContract(String ownerAddress) {
    return Any.pack(
        Contract.WithdrawBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .build());
  }

  @Test
  public void testWithdrawBalance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);
    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    Assert.assertEquals(accountWrapper.getAllowance(), allowance);
    Assert.assertEquals(accountWrapper.getLatestWithdrawTime(), 0);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://baidu.com");
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator operator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance + allowance);
      Assert.assertEquals(owner.getAllowance(), 0);
      Assert.assertNotEquals(owner.getLatestWithdrawTime(), 0);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void invalidOwnerAddress() {
    WithdrawBalanceOperator operator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS_INVALID), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);

      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void invalidOwnerAccount() {
    WithdrawBalanceOperator operator = new WithdrawBalanceOperator(
        getContract(OWNER_ACCOUNT_INVALID), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notWitness() {
//    long now = System.currentTimeMillis();
//    AccountWrapper accountWrapper = dbManager.getAccountStore()
//        .get(ByteArray.fromHexString(OWNER_ADDRESS));
//    accountWrapper.setFrozen(1_000_000_000L, now);
//    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    WithdrawBalanceOperator operator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS + "] is not a witnessAccount",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noAllowance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    Assert.assertEquals(accountWrapper.getAllowance(), 0);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://baidu.com");
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator operator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("witnessAccount does not have any allowance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void isGR() {
    Witness w = Args.getInstance().getGenesisBlock().getWitnesses().get(0);
    byte[] address = w.getAddress();
    AccountWrapper grWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("gr"),
            ByteString.copyFrom(address),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(grWrapper.createDbKey(), grWrapper);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    Assert.assertEquals(accountWrapper.getAllowance(), allowance);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://google.com");

    dbManager.getAccountStore().put(address, accountWrapper);
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator operator = new WithdrawBalanceOperator(
        getContract(ByteArray.toHexString(address)), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertTrue(dbManager.getWitnessStore().has(address));

    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      String readableOwnerAddress = StringUtil.createReadableString(address);
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + readableOwnerAddress
          + "] is a guard representative and is not allowed to withdraw Balance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notTimeToWithdraw() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);
    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    accountWrapper.setLatestWithdrawTime(now);
    Assert.assertEquals(accountWrapper.getAllowance(), allowance);
    Assert.assertEquals(accountWrapper.getLatestWithdrawTime(), now);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://baidu.com");

    dbManager.getAccountStore().put(address, accountWrapper);
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator operator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("The last withdraw time is "
          + now + ",less than 24 hours", e.getMessage());
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

