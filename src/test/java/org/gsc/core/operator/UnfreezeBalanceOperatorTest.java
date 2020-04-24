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
import java.util.ArrayList;
import java.util.List;
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
import org.gsc.core.wrapper.DelegatedResourceAccountIndexWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;
import org.gsc.protos.Protocol.Vote;

@Slf4j
public class UnfreezeBalanceOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "db_unfreeze_balance_test";
  private static GSCApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long frozenBalance = 1_000_000_000L;
  private static final long smallTatalResource = 100L;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
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

    AccountWrapper receiverWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(receiverWrapper.getAddress().toByteArray(), receiverWrapper);
  }

  private Any getContractForNet(String ownerAddress) {
    return Any.pack(
        Contract.UnfreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .build());
  }

  private Any getContractForCpu(String ownerAddress) {
    return Any.pack(
        Contract.UnfreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setResource(org.gsc.protos.Contract.ResourceCode.CPU)
            .build());
  }

  private Any getDelegatedContractForNet(String ownerAddress, String receiverAddress) {
    return Any.pack(
        Contract.UnfreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
            .build());
  }

  private Any getDelegatedContractForCpu(String ownerAddress, String receiverAddress) {
    return Any.pack(
        Contract.UnfreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
            .setResource(org.gsc.protos.Contract.ResourceCode.CPU)
            .build());
  }

  private Any getContract(String ownerAddress, Contract.ResourceCode resourceCode) {
    return Any.pack(
        Contract.UnfreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setResource(resourceCode)
            .build());
  }


  @Test
  public void testUnfreezeBalanceForNet() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(frozenBalance, now);
    Assert.assertEquals(accountWrapper.getFrozenBalance(), frozenBalance);
    Assert.assertEquals(accountWrapper.getGSCPower(), frozenBalance);

    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getContractForNet(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getFrozenBalance(), 0);
      Assert.assertEquals(owner.getGSCPower(), 0L);

      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore, totalNetWeightAfter + frozenBalance / 1000_000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForCpu() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozenForCpu(frozenBalance, now);
    Assert.assertEquals(accountWrapper.getAllFrozenBalanceForCpu(), frozenBalance);
    Assert.assertEquals(accountWrapper.getGSCPower(), frozenBalance);

    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getContractForCpu(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    long totalCpuWeightBefore = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getCpuFrozenBalance(), 0);
      Assert.assertEquals(owner.getGSCPower(), 0L);
      long totalCpuWeightAfter = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();
      Assert.assertEquals(totalCpuWeightBefore,
          totalCpuWeightAfter + frozenBalance / 1000_000L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForNet() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setDelegatedFrozenBalanceForNet(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getGSCPower());

    AccountWrapper receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForNet(frozenBalance);
    Assert.assertEquals(0L, receiver.getGSCPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    //init DelegatedResourceWrapper
    DelegatedResourceWrapper delegatedResourceWrapper = new DelegatedResourceWrapper(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceWrapper.setFrozenBalanceForNet(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceWrapper
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceWrapper);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndex = new DelegatedResourceAccountIndexWrapper(
          owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndex = new DelegatedResourceAccountIndexWrapper(
          receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(RECEIVER_ADDRESS), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getDelegatedContractForNet(OWNER_ADDRESS, RECEIVER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      AccountWrapper receiverResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getGSCPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForNet());
      Assert.assertEquals(0L, receiverResult.getAllFrozenBalanceForNet());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapperOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexWrapperOwner.getFromAccountsList().size());
      Assert.assertEquals(0, delegatedResourceAccountIndexWrapperOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapperReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexWrapperReceiver.getToAccountsList().size());
      Assert
          .assertEquals(0,
              delegatedResourceAccountIndexWrapperReceiver.getFromAccountsList().size());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForNetWithDeletedReceiver() {

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setDelegatedFrozenBalanceForNet(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getGSCPower());

    AccountWrapper receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForNet(frozenBalance);
    Assert.assertEquals(0L, receiver.getGSCPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    //init DelegatedResourceWrapper
    DelegatedResourceWrapper delegatedResourceWrapper = new DelegatedResourceWrapper(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceWrapper.setFrozenBalanceForNet(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceWrapper
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceWrapper);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndex = new DelegatedResourceAccountIndexWrapper(
          owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndex = new DelegatedResourceAccountIndexWrapper(
          receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(RECEIVER_ADDRESS), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getDelegatedContractForNet(OWNER_ADDRESS, RECEIVER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();



    dbManager.getDynamicPropertiesStore().saveAllowGvmConstantinople(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());
    try {
      operator.validate();
      operator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),"Receiver Account[01f80cabd4b9367799eaa3197fecb144eb71de1e049150] not exists");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowGvmConstantinople(1);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getGSCPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForNet());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapperOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexWrapperOwner.getFromAccountsList().size());
      Assert.assertEquals(0, delegatedResourceAccountIndexWrapperOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapperReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexWrapperReceiver.getToAccountsList().size());
      Assert
          .assertEquals(0,
              delegatedResourceAccountIndexWrapperReceiver.getFromAccountsList().size());

    } catch (ContractValidateException e) {
      logger.error("",e);
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * when SameTokenName close,delegate balance frozen, unfreoze show error
   */
  @Test
  public void testUnfreezeDelegatedBalanceForNetSameTokenNameClose() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(0);

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setDelegatedFrozenBalanceForNet(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getGSCPower());

    AccountWrapper receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForNet(frozenBalance);
    Assert.assertEquals(0L, receiver.getGSCPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    //init DelegatedResourceWrapper
    DelegatedResourceWrapper delegatedResourceWrapper = new DelegatedResourceWrapper(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceWrapper.setFrozenBalanceForNet(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceWrapper
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceWrapper);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndex = new DelegatedResourceAccountIndexWrapper(
          owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(OWNER_ADDRESS), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndex = new DelegatedResourceAccountIndexWrapper(
          receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(ByteArray.fromHexString(RECEIVER_ADDRESS), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getDelegatedContractForNet(OWNER_ADDRESS, RECEIVER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozenBalance(NET)", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpu() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.addDelegatedFrozenBalanceForCpu(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getGSCPower());

    AccountWrapper receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForCpu(frozenBalance);
    Assert.assertEquals(0L, receiver.getGSCPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(),receiver);

    DelegatedResourceWrapper delegatedResourceWrapper = new DelegatedResourceWrapper(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceWrapper.setFrozenBalanceForCpu(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceWrapper
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceWrapper);

    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      AccountWrapper receiverResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getGSCPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForCpu());
      Assert.assertEquals(0L, receiverResult.getAllFrozenBalanceForCpu());
    } catch (ContractValidateException e) {
      logger.error("",e);
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpuWithDeletedReceiver() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.addDelegatedFrozenBalanceForCpu(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getGSCPower());

    AccountWrapper receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForCpu(frozenBalance);
    Assert.assertEquals(0L, receiver.getGSCPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    DelegatedResourceWrapper delegatedResourceWrapper = new DelegatedResourceWrapper(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceWrapper.setFrozenBalanceForCpu(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceWrapper
        .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceWrapper);

    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();


    dbManager.getDynamicPropertiesStore().saveAllowGvmConstantinople(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());

    try {
      operator.validate();
      operator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),"Receiver Account[01f80cabd4b9367799eaa3197fecb144eb71de1e049150] not exists");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowGvmConstantinople(1);

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getGSCPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForCpu());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getContractForNet(OWNER_ADDRESS_INVALID), dbManager);
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
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getContractForNet(OWNER_ACCOUNT_INVALID), dbManager);
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
  public void noFrozenBalance() {
    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getContractForNet(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozenBalance(NET)", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notTimeToUnfreeze() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(1_000_000_000L, now + 60000);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getContractForNet(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("It's not time to unfreeze(NET).", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testClearVotes() {
    byte[] ownerAddressBytes = ByteArray.fromHexString(OWNER_ADDRESS);
    ByteString ownerAddress = ByteString.copyFrom(ownerAddressBytes);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddressBytes);
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator operator = new UnfreezeBalanceOperator(
        getContractForNet(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    dbManager.getVotesStore().reset();
    Assert.assertNull(dbManager.getVotesStore().get(ownerAddressBytes));
    try {
      operator.validate();
      operator.execute(ret);
      VotesWrapper votesWrapper = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesWrapper);
      Assert.assertEquals(0, votesWrapper.getNewVotes().size());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    List<Vote> oldVotes = new ArrayList<Vote>();
    VotesWrapper votesWrapper = new VotesWrapper(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        oldVotes);
    votesWrapper.addNewVotes(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        100);
    dbManager.getVotesStore().put(ByteArray.fromHexString(OWNER_ADDRESS), votesWrapper);
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    try {
      operator.validate();
      operator.execute(ret);
      votesWrapper = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesWrapper);
      Assert.assertEquals(0, votesWrapper.getNewVotes().size());
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

