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
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionResultWrapper;
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
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class TransferOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "db_transfer_test";
  private static GSCApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final long AMOUNT = 100;
  private static final long OWNER_BALANCE = 9999999;
  private static final long TO_BALANCE = 100001;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String TO_ADDRESS_INVALID = "bbb";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final String OWNER_NO_BALANCE;
  private static final String To_ACCOUNT_INVALID;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    OWNER_NO_BALANCE = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3433";
    To_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3422";
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
  public void createWrapper() {
    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            OWNER_BALANCE);
    AccountWrapper toAccountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            TO_BALANCE);
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
    dbManager.getAccountStore().put(toAccountWrapper.getAddress().toByteArray(), toAccountWrapper);
  }

  private Any getContract(long count) {
    long nowTime = new Date().getTime();
    return Any.pack(
        Contract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(count)
            .build());
  }

  private Any getContract(long count, String owneraddress, String toaddress) {
    long nowTime = new Date().getTime();
    return Any.pack(
        Contract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owneraddress)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toaddress)))
            .setAmount(count)
            .build());
  }

  @Test
  public void rightTransfer() {
    TransferOperator operator = new TransferOperator(getContract(AMOUNT), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - AMOUNT - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + AMOUNT);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void perfectTransfer() {
    TransferOperator operator = new TransferOperator(
        getContract(OWNER_BALANCE - ChainConstant.TRANSFER_FEE), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 0);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + OWNER_BALANCE);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void moreTransfer() {
    TransferOperator operator = new TransferOperator(getContract(OWNER_BALANCE + 1), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      logger.info(e.getMessage());
      Assert.assertTrue(
          "Validate TransferContract error, balance is not sufficient.".equals(e.getMessage()));
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void iniviateOwnerAddress() {
    TransferOperator operator = new TransferOperator(
        getContract(10000L, OWNER_ADDRESS_INVALID, TO_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Invalid ownerAddress");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid ownerAddress", e.getMessage());
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void iniviateToAddress() {
    TransferOperator operator = new TransferOperator(
        getContract(10000L, OWNER_ADDRESS, TO_ADDRESS_INVALID), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Invalid toAddress");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid toAddress", e.getMessage());
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void iniviateTrx() {
    TransferOperator operator = new TransferOperator(
        getContract(100L, OWNER_ADDRESS, OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Cannot transfer gsc to yourself.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cannot transfer gsc to yourself.", e.getMessage());
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void noExitOwnerAccount() {
    TransferOperator operator = new TransferOperator(
        getContract(100L, OWNER_ACCOUNT_INVALID, TO_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Validate TransferContract error, no OwnerAccount.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Validate TransferContract error, no OwnerAccount.", e.getMessage());
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  /**
   * If to account not exit, create it.
   */
  public void noExitToAccount() {
    TransferOperator operator = new TransferOperator(
        getContract(1_000_000L, OWNER_ADDRESS, To_ACCOUNT_INVALID), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      AccountWrapper noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(To_ACCOUNT_INVALID));
      Assert.assertTrue(null == noExitAccount);
      operator.validate();
      operator.execute(ret);
      noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(To_ACCOUNT_INVALID));
      Assert.assertFalse(null == noExitAccount);    //Had created.
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1_000_000L);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(To_ACCOUNT_INVALID));
      Assert.assertEquals(noExitAccount.getBalance(), 1_000_000L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(To_ACCOUNT_INVALID));
    }
  }

  @Test
  public void zeroAmountTest() {
    TransferOperator operator = new TransferOperator(getContract(0), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0.".equals(e.getMessage()));
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void negativeAmountTest() {
    TransferOperator operator = new TransferOperator(getContract(-AMOUNT), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0.".equals(e.getMessage()));
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void addOverflowTest() {
    // First, increase the to balance. Else can't complete this test case.
    AccountWrapper toAccount = dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
    toAccount.setBalance(Long.MAX_VALUE);
    dbManager.getAccountStore().put(ByteArray.fromHexString(TO_ADDRESS), toAccount);
    TransferOperator operator = new TransferOperator(getContract(1), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("long overflow").equals(e.getMessage()));
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      toAccount = dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), Long.MAX_VALUE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void insufficientFee() {
    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_NO_BALANCE)),
            AccountType.Normal,
            -10000L);
    AccountWrapper toAccountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(To_ACCOUNT_INVALID)),
            AccountType.Normal,
            100L);
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
    dbManager.getAccountStore().put(toAccountWrapper.getAddress().toByteArray(), toAccountWrapper);

    TransferOperator operator = new TransferOperator(
        getContract(AMOUNT, OWNER_NO_BALANCE, To_ACCOUNT_INVALID), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Validate TransferContract error, insufficient fee.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Validate TransferContract error, balance is not sufficient.",
          e.getMessage());
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(To_ACCOUNT_INVALID));
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
