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
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.config.VMConfig;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter.ForkBlockVersionConsts;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.GSCException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;


@Slf4j
@Ignore
public class UpdateCpuLimitContractOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_updateCpuLimitContractOperator_test";
  private static String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
  private static String SECOND_ACCOUNT_ADDRESS;
  private static String OWNER_ADDRESS_NOTEXIST;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String SMART_CONTRACT_NAME = "smart_contarct";
  private static final String CONTRACT_ADDRESS = "111111";
  private static final String NO_EXIST_CONTRACT_ADDRESS = "2222222";
  private static final long SOURCE_CPU_LIMIT = 10L;
  private static final long TARGET_CPU_LIMIT = 30L;
  private static final long INVALID_CPU_LIMIT = -200L;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    OWNER_ADDRESS =
        Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    SECOND_ACCOUNT_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d427122222";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
//    dbManager.getDynamicPropertiesStore()
//        .statsByVersion(ForkBlockVersionConsts.CPU_LIMIT, stats);
//    VMConfig.initVmHardFork();
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

    // smartContract in contractStore
    Protocol.SmartContract.Builder builder = Protocol.SmartContract.newBuilder();
    builder.setName(SMART_CONTRACT_NAME);
    builder.setOriginAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    builder.setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_ADDRESS)));
    builder.setOriginCpuLimit(SOURCE_CPU_LIMIT);
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

  /**
   * Release resources.
   */
  private Any getContract(String accountAddress, String contractAddress, long originCpuLimit) {
    return Any.pack(
        Contract.UpdateCpuLimitContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(accountAddress))
            .setContractAddress(StringUtil.hexString2ByteString(contractAddress))
            .setOriginCpuLimit(originCpuLimit).build());
  }

  @Test
  public void successUpdateCpuLimitContract() throws InvalidProtocolBufferException {
    UpdateCpuLimitContractOperator operator =
        new UpdateCpuLimitContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, TARGET_CPU_LIMIT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      // assert result state and cpu_limit
      Assert.assertEquals(OWNER_ADDRESS,
          ByteArray.toHexString(operator.getOwnerAddress().toByteArray()));
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS))
              .getOriginCpuLimit(), TARGET_CPU_LIMIT);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void invalidAddress() {
    UpdateCpuLimitContractOperator operator =
        new UpdateCpuLimitContractOperator(
            getContract(OWNER_ADDRESS_INVALID, CONTRACT_ADDRESS, TARGET_CPU_LIMIT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Invalid address");
    } catch (GSCException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    }
  }

  @Test
  public void noExistAccount() {
    UpdateCpuLimitContractOperator operator =
        new UpdateCpuLimitContractOperator(
            getContract(OWNER_ADDRESS_NOTEXIST, CONTRACT_ADDRESS, TARGET_CPU_LIMIT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Account[" + OWNER_ADDRESS_NOTEXIST + "] not exists");
    } catch (GSCException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOTEXIST + "] not exists", e.getMessage());
    }
  }

  @Test
  public void invalidResourceCpuLimit() {
    UpdateCpuLimitContractOperator operator =
        new UpdateCpuLimitContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, INVALID_CPU_LIMIT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("origin cpu limit less than 0");
    } catch (GSCException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("origin cpu limit must > 0", e.getMessage());
    }
  }

  @Test
  public void noExistContract() {
    UpdateCpuLimitContractOperator operator =
        new UpdateCpuLimitContractOperator(
            getContract(OWNER_ADDRESS, NO_EXIST_CONTRACT_ADDRESS, TARGET_CPU_LIMIT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Contract not exists");
    } catch (GSCException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Contract not exists", e.getMessage());
    }
  }

  @Test
  public void callerNotContractOwner() {
    UpdateCpuLimitContractOperator operator =
        new UpdateCpuLimitContractOperator(
            getContract(SECOND_ACCOUNT_ADDRESS, CONTRACT_ADDRESS, TARGET_CPU_LIMIT), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Account[" + SECOND_ACCOUNT_ADDRESS + "] is not the owner of the contract");
    } catch (GSCException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Account[" + SECOND_ACCOUNT_ADDRESS + "] is not the owner of the contract",
          e.getMessage());
    }
  }

  @Test
  public void twiceUpdateCpuLimitContract() throws InvalidProtocolBufferException {
    UpdateCpuLimitContractOperator operator =
        new UpdateCpuLimitContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, TARGET_CPU_LIMIT), dbManager);

    UpdateCpuLimitContractOperator secondoperator =
        new UpdateCpuLimitContractOperator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, 90L), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      // first
      operator.validate();
      operator.execute(ret);

      Assert.assertEquals(OWNER_ADDRESS,
          ByteArray.toHexString(operator.getOwnerAddress().toByteArray()));
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS))
              .getOriginCpuLimit(), TARGET_CPU_LIMIT);

      // second
      secondoperator.validate();
      secondoperator.execute(ret);

      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS))
              .getOriginCpuLimit(), 90L);
    } catch (ContractValidateException | ContractExeException e) {
      Assert.fail(e.getMessage());
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
//    VMConfig.setCPU_LIMIT_HARD_FORK(false);
  }

}
