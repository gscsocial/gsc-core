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
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.witness.WitnessController;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.VoteWitnessContract;
import org.gsc.protos.Contract.VoteWitnessContract.Vote;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class VoteWitnessOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static WitnessController witnessController;
  private static final String dbPath = "db_VoteWitness_test";
  private static final String ACCOUNT_NAME = "account";
  private static final String OWNER_ADDRESS;
  private static final String WITNESS_NAME = "witness";
  private static final String WITNESS_ADDRESS;
  private static final String URL = "https://gsc.network";
  private static final String ADDRESS_INVALID = "aaaa";
  private static final String WITNESS_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_BALANCENOTSUFFICIENT;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    WITNESS_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aae";
    OWNER_ADDRESS_BALANCENOTSUFFICIENT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e06d4271a1ced";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    witnessController = dbManager.getWitnessController();
  }

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void createWrapper() {
    WitnessWrapper ownerWrapper =
        new WitnessWrapper(
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            10L,
            URL);
    AccountWrapper witnessAccountSecondWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(WITNESS_NAME),
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            AccountType.Normal,
            300L);
    AccountWrapper ownerAccountFirstWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            10_000_000_000_000L);

    dbManager.getAccountStore()
        .put(witnessAccountSecondWrapper.getAddress().toByteArray(), witnessAccountSecondWrapper);
    dbManager.getAccountStore()
        .put(ownerAccountFirstWrapper.getAddress().toByteArray(), ownerAccountFirstWrapper);
    dbManager.getWitnessStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
  }

  private Any getContract(String address, String voteaddress, Long value) {
    return Any.pack(
        VoteWitnessContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(address))
            .addVotes(Vote.newBuilder()
                .setVoteAddress(StringUtil.hexString2ByteString(voteaddress))
                .setVoteCount(value).build())
            .build());
  }

  private Any getRepeateContract(String address, String voteaddress, Long value, int times) {
    VoteWitnessContract.Builder builder = VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(StringUtil.hexString2ByteString(address));
    for (int i = 0; i < times; i++) {
      builder.addVotes(Vote.newBuilder()
          .setVoteAddress(StringUtil.hexString2ByteString(voteaddress))
          .setVoteCount(value).build());
    }
    return Any.pack(builder.build());
  }

  private Any getContract(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        Contract.FreezeBalanceContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  /**
   * voteWitness,result is success.
   */
  @Test
  public void voteWitness() {
    long frozenBalance = 1_000_000_000_000L;
    long duration = 5;
    FreezeBalanceOperator freezeBalanceoperator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      freezeBalanceoperator.validate();
      freezeBalanceoperator.execute(ret);
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(1,
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteCount());
      Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteAddress().toByteArray());
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10 + 1, witnessWrapper.getVoteCount());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Invalid ownerAddress voteWitness,result is failed,exception is "Invalid address".
   */
  @Test
  public void InvalidAddress() {
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(ADDRESS_INVALID, WITNESS_ADDRESS, 1L),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use AccountStore not exists witness Address VoteWitness,result is failed,exception is "account
   * not exists".
   */
  @Test
  public void noAccount() {
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS_NOACCOUNT, 1L),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Account[" + WITNESS_ADDRESS_NOACCOUNT + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertEquals(0, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList().size());
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + WITNESS_ADDRESS_NOACCOUNT + "] not exists", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use WitnessStore not exists Address VoteWitness,result is failed,exception is "Witness not
   * exists".
   */
  @Test
  public void noWitness() {
    AccountWrapper accountSecondWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(WITNESS_NAME),
            StringUtil.hexString2ByteString(WITNESS_ADDRESS_NOACCOUNT),
            AccountType.Normal,
            300L);
    dbManager.getAccountStore()
        .put(accountSecondWrapper.getAddress().toByteArray(), accountSecondWrapper);
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS_NOACCOUNT, 1L),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Witness[" + OWNER_ADDRESS_NOACCOUNT + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertEquals(0, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList().size());
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + WITNESS_ADDRESS_NOACCOUNT + "] not exists", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * invalideVoteAddress
   */
  @Test
  public void invalideVoteAddress() {
    AccountWrapper accountSecondWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(WITNESS_NAME),
            StringUtil.hexString2ByteString(WITNESS_ADDRESS_NOACCOUNT),
            AccountType.Normal,
            300L);
    dbManager.getAccountStore()
        .put(accountSecondWrapper.getAddress().toByteArray(), accountSecondWrapper);
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS, ADDRESS_INVALID, 1L),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(0, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList().size());
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid vote address!", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Every vote count must greater than 0.
   */
  @Test
  public void voteCountTest() {
    long frozenBalance = 1_000_000_000_000L;
    long duration = 5;
    FreezeBalanceOperator freezeBalanceoperator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    //0 votes
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, 0L), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      freezeBalanceoperator.validate();
      freezeBalanceoperator.execute(ret);
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("vote count must be greater than 0", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    //-1 votes
    operator = new VoteWitnessOperator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, -1L), dbManager);
    ret = new TransactionResultWrapper();
    try {
      freezeBalanceoperator.validate();
      freezeBalanceoperator.execute(ret);
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("vote count must be greater than 0", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * User can vote to 1 - 30 witnesses.
   */
  @Test
  public void voteCountsTest() {
    long frozenBalance = 1_000_000_000_000L;
    long duration = 5;
    FreezeBalanceOperator freezeBalanceoperator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    VoteWitnessOperator operator = new VoteWitnessOperator(
        getRepeateContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L, 0),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      freezeBalanceoperator.validate();
      freezeBalanceoperator.execute(ret);
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("VoteNumber must more than 0", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    operator = new VoteWitnessOperator(getRepeateContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L, 31),
        dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("VoteNumber more than maxVoteNumber 30", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Vote 1 witness one more times.
   */
  @Test
  public void vote1WitnssOneMoreTiems() {
    long frozenBalance = 1_000_000_000_000L;
    long duration = 5;
    FreezeBalanceOperator freezeBalanceoperator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    VoteWitnessOperator operator = new VoteWitnessOperator(
        getRepeateContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L, 30),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      freezeBalanceoperator.validate();
      freezeBalanceoperator.execute(ret);
      operator.validate();
      operator.execute(ret);

      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10 + 30, witnessWrapper.getVoteCount());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use AccountStore not exists ownerAddress VoteWitness,result is failed,exception is "account not
   * exists".
   */
  @Test
  public void noOwnerAccount() {
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS_NOACCOUNT, WITNESS_ADDRESS, 1L),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * witnessAccount not freeze Balance, result is failed ,exception is "The total number of votes
   * 1000000 is greater than 0.
   */
  @Test
  public void balanceNotSufficient() {
    AccountWrapper balanceNotSufficientWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("balanceNotSufficient"),
            StringUtil.hexString2ByteString(OWNER_ADDRESS_BALANCENOTSUFFICIENT),
            AccountType.Normal,
            500L);
    dbManager.getAccountStore()
        .put(balanceNotSufficientWrapper.getAddress().toByteArray(), balanceNotSufficientWrapper);
    VoteWitnessOperator operator =
        new VoteWitnessOperator(
            getContract(OWNER_ADDRESS_BALANCENOTSUFFICIENT, WITNESS_ADDRESS, 1L),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("The total number of votes[" + 1000000 + "] is greater than the gscPower["
          + balanceNotSufficientWrapper.getGSCPower() + "]");
    } catch (ContractValidateException e) {
      Assert.assertEquals(0, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS_BALANCENOTSUFFICIENT)).getVotesList().size());
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert
          .assertEquals("The total number of votes[" + 1000000 + "] is greater than the GscPower["
              + balanceNotSufficientWrapper.getGSCPower() + "]", e.getMessage());
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(10, witnessWrapper.getVoteCount());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Twice voteWitness,result is the last voteWitness.
   */
  @Test
  public void voteWitnessTwice() {
    long frozenBalance = 7_000_000_000_000L;
    long duration = 5;
    FreezeBalanceOperator freezeBalanceoperator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    VoteWitnessOperator operator =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L), dbManager);
    VoteWitnessOperator operatorTwice =
        new VoteWitnessOperator(getContract(OWNER_ADDRESS, WITNESS_ADDRESS, 3L), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      freezeBalanceoperator.validate();
      freezeBalanceoperator.execute(ret);
      operator.validate();
      operator.execute(ret);
      operatorTwice.validate();
      operatorTwice.execute(ret);
      Assert.assertEquals(3,
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteCount());
      Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
              .get(0).getVoteAddress().toByteArray());

      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      witnessController.updateWitness();
      WitnessWrapper witnessWrapper = witnessController
          .getWitnesseByAddress(StringUtil.hexString2ByteString(WITNESS_ADDRESS));
      Assert.assertEquals(13, witnessWrapper.getVoteCount());
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