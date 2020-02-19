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

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
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
public class BuyStorageOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "db_buy_storage_test1";
  private static GSCApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000_000_000L;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "f740d0a396a324b15d68aa8ffeaf2057d937d745";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "89b3256f723498acc2fa7d67fd456e93c738f31d";
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
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

    dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(
        128L * 1024 * 1024 * 1024);
    dbManager.getDynamicPropertiesStore().saveTotalStoragePool(100_000_000_000000L);
    dbManager.getDynamicPropertiesStore().saveTotalStorageTax(0);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);
  }

  private Any getContract(String ownerAddress, long quant) {
    return Any.pack(
        Contract.BuyStorageContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setQuant(quant)
            .build());
  }

  @Test
  public void testBuyStorage() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long quant = 2_000_000_000_000L; // 2 million gsc
    BuyStorageOperator operator = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(2694881440L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 2694881440L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testBuyStorage2() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long quant = 1_000_000_000_000L; // 1 million gsc

    BuyStorageOperator operator = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    BuyStorageOperator operator2 = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultWrapper ret2 = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(1360781717L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 1360781717L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());

      operator2.validate();
      operator2.execute(ret2);
      Assert.assertEquals(ret2.getInstance().getRet(), code.SUCESS);

      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - 2 * quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(2694881439L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 2694881439L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + 2 * quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void buyLessThanZero() {
    long quant = -1_000_000_000L;
    BuyStorageOperator operator = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("quantity must be positive", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void buyLessThan1Gsc() {
    long quant = 200_000L;
    BuyStorageOperator operator = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("quantity must be larger than 1GSC", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void buyLessThan1Byte() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long quant = 9_000_000_000_000_000L; // 9 billion gsc

    BuyStorageOperator operator = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    BuyStorageOperator operator2 = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, 1_000_000), dbManager);
    TransactionResultWrapper ret2 = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(135928635301L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 135928635301L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());

      operator2.validate();
      operator2.execute(ret2);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("storage_bytes must be larger than 1,current storage_bytes[0]",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void buyMoreThanBalance() {
    long quant = 11_000_000_000_000_000L;
    BuyStorageOperator operator = new BuyStorageOperator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("quantity must be less than accountBalance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long quant = 1_000_000_000L;
    BuyStorageOperator operator = new BuyStorageOperator(
        getContract(OWNER_ADDRESS_INVALID, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);

      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void invalidOwnerAccount() {
    long quant = 1_000_000_000L;
    BuyStorageOperator actuator = new BuyStorageOperator(
            getContract(OWNER_ACCOUNT_INVALID, quant), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
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
