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
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;


@Slf4j
public class UpdateSettingContractOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_updatesettingcontract_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
  private static final String SECOND_ACCOUNT_ADDRESS;
  private static final String OWNER_ADDRESS_NOTEXIST;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String SMART_CONTRACT_NAME = "smart_contarct";
  private static final String CONTRACT_ADDRESS = "111111";
  private static final String NO_EXIST_CONTRACT_ADDRESS = "2222222";
  private static final long SOURCE_PERCENT = 10L;
  private static final long TARGET_PERCENT = 30L;
  private static final long INVALID_PERCENT = 200L;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    SECOND_ACCOUNT_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d427122222";
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
  public void createWrapper() {
    // address in accountStore and the owner of contract
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountWrapper);

    // smartContarct in contractStore
    Protocol.SmartContract.Builder builder = Protocol.SmartContract.newBuilder();
    builder.setName(SMART_CONTRACT_NAME);
    builder.setOriginAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    builder.setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_ADDRESS)));
    builder.setConsumeUserResourcePercent(SOURCE_PERCENT);
    dbManager.getContractStore().put(
        ByteArray.fromHexString(CONTRACT_ADDRESS),
        new ContractWrapper(builder.build()));

    // address in accountStore not the owner of contract
    AccountWrapper secondAccount =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS), secondAccount);

    // address does not exist in accountStore
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST));
  }

  private Any getContract(String accountAddress, String contractAddress, long percent) {
    return Any.pack(
        Contract.UpdateSettingContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(accountAddress))
            .setContractAddress(StringUtil.hexString2ByteString(contractAddress))
            .setConsumeUserResourcePercent(percent).build());
  }

  @Test
  public void successUpdateSettingContract() {
    UpdateSettingContractOperator operator =
        new UpdateSettingContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      // assert result state and consume_user_resource_percent
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS)).
              getConsumeUserResourcePercent(),
          TARGET_PERCENT);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidAddress() {
    UpdateSettingContractOperator operator =
        new UpdateSettingContractOperator(
            getContract(OWNER_ADDRESS_INVALID, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

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

  @Test
  public void noExistAccount() {
    UpdateSettingContractOperator operator =
        new UpdateSettingContractOperator(
            getContract(OWNER_ADDRESS_NOTEXIST, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Account[" + OWNER_ADDRESS_NOTEXIST + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOTEXIST + "] not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidResourcePercent() {
    UpdateSettingContractOperator operator =
        new UpdateSettingContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, INVALID_PERCENT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("percent not in [0, 100]");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("percent not in [0, 100]", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noExistContract() {
    UpdateSettingContractOperator operator =
        new UpdateSettingContractOperator(
            getContract(OWNER_ADDRESS, NO_EXIST_CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Contract not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Contract not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void callerNotContractOwner() {
    UpdateSettingContractOperator operator =
        new UpdateSettingContractOperator(
            getContract(SECOND_ACCOUNT_ADDRESS, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Account[" + SECOND_ACCOUNT_ADDRESS + "] is not the owner of the contract");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Account[" + SECOND_ACCOUNT_ADDRESS + "] is not the owner of the contract",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void twiceUpdateSettingContract() {
    UpdateSettingContractOperator operator =
        new UpdateSettingContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    UpdateSettingContractOperator secondoperator =
        new UpdateSettingContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, 90L), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      // first
      operator.validate();
      operator.execute(ret);

      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS)).
              getConsumeUserResourcePercent(),
          TARGET_PERCENT);

      // second
      secondoperator.validate();
      secondoperator.execute(ret);

      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS)).
              getConsumeUserResourcePercent(),
          90L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
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
