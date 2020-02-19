/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 5 of the License, or
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
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.ResourceCode;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;
import org.spongycastle.util.encoders.Hex;

@Slf4j
public class FreezeBalanceOperatorTest {

    private static Manager dbManager;
    private static final String dbPath = "db_freeze_balance_test";
    private static GSCApplicationContext context;
    private static final String OWNER_ADDRESS;
    private static final String RECEIVER_ADDRESS;
    private static final String OWNER_ADDRESS_INVALID = "abcdef";
    private static final String OWNER_ACCOUNT_INVALID;
    private static final long initBalance = 10_000_000_000L;
    // 10000000000
    // 1000000000

    static {
        Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
        context = new GSCApplicationContext(DefaultConfig.class);
        OWNER_ADDRESS = Wallet.getAddressPreFixString() + "f740d0a396a324b15d68aa8ffeaf2057d937d745";
        RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "afe9656f4a2d804aea04afac52627aef6299780e";
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

        AccountWrapper accountWrapper1 =
                new AccountWrapper(
                        ByteString.copyFromUtf8("receiver"),
                        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
                        AccountType.Normal,
                        initBalance);
        dbManager.getAccountStore().put(accountWrapper1.getAddress().toByteArray(), accountWrapper1);
    }

    private Any getContractForNet(String ownerAddress, long frozenBalance, long duration) {
        return Any.pack(
                Contract.FreezeBalanceContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                        .setFrozenBalance(frozenBalance)
                        .setFrozenDuration(duration)
                        .build());
    }

    private Any getContractForCpu(String ownerAddress, long frozenBalance, long duration) {
        return Any.pack(
                Contract.FreezeBalanceContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                        .setFrozenBalance(frozenBalance)
                        .setFrozenDuration(duration)
                        .setResource(ResourceCode.CPU)
                        .build());
    }

    private Any getDelegatedContractForNet(String ownerAddress, String receiverAddress,
                                           long frozenBalance,
                                           long duration) {
        return Any.pack(
                Contract.FreezeBalanceContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
                        .setFrozenBalance(frozenBalance)
                        .setFrozenDuration(duration)
                        .build());
    }

    private Any getDelegatedContractForCpu(String ownerAddress, String receiverAddress,
                                           long frozenBalance,
                                           long duration) {
        return Any.pack(
                Contract.FreezeBalanceContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                        .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
                        .setFrozenBalance(frozenBalance)
                        .setFrozenDuration(duration)
                        .setResource(ResourceCode.CPU)
                        .build());
    }

    @Test
    public void testFreezeBalanceForNet() {
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            AccountWrapper owner =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

            Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
                    - ChainConstant.TRANSFER_FEE);
            Assert.assertEquals(owner.getFrozenBalance(), frozenBalance);
            Assert.assertEquals(frozenBalance, owner.getGSCPower());
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void testFreezeBalanceForCpu() {
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForCpu(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

            AccountWrapper owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
                    - ChainConstant.TRANSFER_FEE);
            Assert.assertEquals(0L, owner.getFrozenBalance());
            Assert.assertEquals(frozenBalance, owner.getCpuFrozenBalance());
            Assert.assertEquals(frozenBalance, owner.getGSCPower());
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }

    }


    @Test
    public void testFreezeDelegatedBalanceForNetWithContractAddress() {
        AccountWrapper accountWrapper =
                new AccountWrapper(
                        ByteString.copyFromUtf8("receiver"),
                        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
                        AccountType.Contract,
                        initBalance);
        dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

        dbManager.getDynamicPropertiesStore().saveAllowGvmConstantinople(1);

        dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getDelegatedContractForNet(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
        } catch (ContractValidateException e) {
            Assert.assertEquals(e.getMessage(), "Do not allow delegate resources to contract addresses");
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    //@Test
    public void testFreezeDelegatedBalanceForNet() {
        dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator actuator = new FreezeBalanceOperator(
                getDelegatedContractForNet(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

        try {
            actuator.validate();
            actuator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            AccountWrapper owner =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

            Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
                    - ChainConstant.TRANSFER_FEE);
            Assert.assertEquals(0L, owner.getFrozenBalance());
            Assert.assertEquals(frozenBalance, owner.getDelegatedFrozenBalanceForNet());
            Assert.assertEquals(frozenBalance, owner.getGSCPower());

            AccountWrapper receiver =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
            Assert.assertEquals(frozenBalance, receiver.getAcquiredDelegatedFrozenBalanceForNet());
            Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForCpu());
            Assert.assertEquals(0L, receiver.getGSCPower());

            DelegatedResourceWrapper delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
                    .get(DelegatedResourceWrapper
                            .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
                                    ByteArray.fromHexString(RECEIVER_ADDRESS)));

            Assert.assertEquals(frozenBalance, delegatedResourceCapsule.getFrozenBalanceForNet());
            long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
            Assert.assertEquals(totalNetWeightBefore + frozenBalance / 1000_000L, totalNetWeightAfter);

            //check DelegatedResourceAccountIndex
            DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexCapsuleOwner = dbManager
                    .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert
                    .assertEquals(0, delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
            Assert.assertEquals(1, delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());
            Assert.assertEquals(true,
                    delegatedResourceAccountIndexCapsuleOwner.getToAccountsList()
                            .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

            DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexCapsuleReceiver = dbManager
                    .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
            Assert
                    .assertEquals(0, delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
            Assert
                    .assertEquals(1,
                            delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());
            Assert.assertEquals(true,
                    delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList()
                            .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void testFreezeDelegatedBalanceForCpuSameNameTokenActive() {
        dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator actuator = new FreezeBalanceOperator(
                getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();
        try {
            actuator.validate();
            actuator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            AccountWrapper owner =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

            Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
                    - ChainConstant.TRANSFER_FEE);
            Assert.assertEquals(0L, owner.getFrozenBalance());
            Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForNet());
            Assert.assertEquals(frozenBalance, owner.getDelegatedFrozenBalanceForCpu());
            Assert.assertEquals(frozenBalance, owner.getGSCPower());

            AccountWrapper receiver =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
            Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForNet());
            Assert.assertEquals(frozenBalance, receiver.getAcquiredDelegatedFrozenBalanceForCpu());
            Assert.assertEquals(0L, receiver.getGSCPower());

            DelegatedResourceWrapper delegatedResourceWrapper = dbManager.getDelegatedResourceStore()
                    .get(DelegatedResourceWrapper
                            .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
                                    ByteArray.fromHexString(RECEIVER_ADDRESS)));

            System.out.println(Hex.toHexString(delegatedResourceWrapper.getFrom().toByteArray()));
            System.out.println(Hex.toHexString(delegatedResourceWrapper.getTo().toByteArray()));
            System.out.println(delegatedResourceWrapper.getFrozenBalanceForNet());
            System.out.println(delegatedResourceWrapper.getFrozenBalanceForCpu());
            System.out.println(delegatedResourceWrapper.getExpireTimeForCpu(dbManager));
            System.out.println(delegatedResourceWrapper.getExpireTimeForNet());
//            System.out.println("getData  " + Hex.toHexString(delegatedResourceWrapper.getData()));
//            System.out.println("getInstance  " + delegatedResourceWrapper.getInstance());

            Assert.assertEquals(0L, delegatedResourceWrapper.getFrozenBalanceForNet());
            Assert.assertEquals(frozenBalance, delegatedResourceWrapper.getFrozenBalanceForCpu());

            long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();
            Assert.assertEquals(totalEnergyWeightBefore + frozenBalance / 1000_000L,
                    totalEnergyWeightAfter);

            //check DelegatedResourceAccountIndex
            DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexCapsuleOwner = dbManager
                    .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert
                    .assertEquals(0, delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
            Assert.assertEquals(1, delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());
            Assert.assertEquals(true,
                    delegatedResourceAccountIndexCapsuleOwner.getToAccountsList()
                            .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

            DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexCapsuleReceiver = dbManager
                    .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
            Assert
                    .assertEquals(0, delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
            Assert
                    .assertEquals(1,
                            delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());
            Assert.assertEquals(true,
                    delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList()
                            .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));

        } catch (ContractValidateException e) {
            System.out.println(e.getMessage());
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void testFreezeDelegatedBalanceForCpuSameNameTokenClose() {
        dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(0);
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        long totalCpuWeightBefore = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();
        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            AccountWrapper owner = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
                    - ChainConstant.TRANSFER_FEE);
            Assert.assertEquals(0L, owner.getFrozenBalance());
            Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForNet());
            Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForCpu());
            Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForCpu());

            AccountWrapper receiver =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
            Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForNet());
            Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForCpu());
            Assert.assertEquals(0L, receiver.getGSCPower());

            long totalCpuWeightAfter = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();
            Assert.assertEquals(totalCpuWeightBefore + frozenBalance / 1000_000L,
                    totalCpuWeightAfter);

        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void freezeLessThanZero() {
        long frozenBalance = -1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            fail("cannot run here.");

        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("frozenBalance must be positive", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void freezeMoreThanBalance() {
        long frozenBalance = 11_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            fail("cannot run here.");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("frozenBalance must be less than accountBalance", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void invalidOwnerAddress() {
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS_INVALID, frozenBalance, duration), dbManager);
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
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ACCOUNT_INVALID, frozenBalance, duration), dbManager);
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
    public void durationLessThanMin() {
        long frozenBalance = 1_000_000_000L;
        long duration = 2;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            fail("cannot run here.");

        } catch (ContractValidateException e) {
            long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
            long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("frozenDuration must be less than " + maxFrozenTime + " days "
                            + "and more than " + minFrozenTime + " days"
                    , e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void durationMoreThanMax() {
        long frozenBalance = 1_000_000_000L;
        long duration = 4;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            fail("cannot run here.");
        } catch (ContractValidateException e) {
            long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
            long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("frozenDuration must be less than " + maxFrozenTime + " days "
                            + "and more than " + minFrozenTime + " days"
                    , e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void lessThan1GscTest() {
        long frozenBalance = 1;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            fail("cannot run here.");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("frozenBalance must be more than 1GSC", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void frozenNumTest() {
        AccountWrapper account = dbManager.getAccountStore()
                .get(ByteArray.fromHexString(OWNER_ADDRESS));
        account.setFrozen(1_000L, 1_000_000_000L);
        account.setFrozen(1_000_000L, 1_000_000_000L);
        dbManager.getAccountStore().put(account.getAddress().toByteArray(), account);

        long frozenBalance = 20_000_000L;
        long duration = 5L;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            fail("cannot run here.");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("frozenCount must be 0 or 1", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    //@Test
    public void moreThanFrozenNumber() {
        long frozenBalance = 1_000_000_000L;
        long duration = 5;
        FreezeBalanceOperator operator = new FreezeBalanceOperator(
                getContractForNet(OWNER_ADDRESS, frozenBalance, duration), dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
        try {
            operator.validate();
            operator.execute(ret);
            fail("cannot run here.");
        } catch (ContractValidateException e) {
            long maxFrozenNumber = ChainConstant.MAX_FROZEN_NUMBER;
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("max frozen number is: " + maxFrozenNumber, e.getMessage());

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
