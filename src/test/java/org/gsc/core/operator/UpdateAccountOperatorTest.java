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
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UpdateAccountOperatorTest {

    private static GSCApplicationContext context;
    private static Manager dbManager;
    private static final String dbPath = "db_updateaccount_test";
    private static final String ACCOUNT_NAME = "ownerTest";
    private static final String ACCOUNT_NAME_1 = "ownerTest1";
    private static final String OWNER_ADDRESS;
    private static final String OWNER_ADDRESS_1;
    private static final String OWNER_ADDRESS_INVALID = "aaaa";

    static {
        Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
        context = new GSCApplicationContext(DefaultConfig.class);
        OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
        OWNER_ADDRESS_1 = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
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
        AccountWrapper ownerWrapper =
                new AccountWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                        ByteString.EMPTY,
                        AccountType.Normal);
        dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
        dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_1));
        dbManager.getAccountIdIndexStore().delete(ACCOUNT_NAME.getBytes());
        dbManager.getAccountIdIndexStore().delete(ACCOUNT_NAME_1.getBytes());
    }

    private Any getContract(String name, String address) {
        return Any.pack(
                Contract.AccountUpdateContract.newBuilder()
                        .setAccountName(ByteString.copyFromUtf8(name))
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
                        .build());
    }

    private Any getContract(ByteString name, String address) {
        return Any.pack(
                Contract.AccountUpdateContract.newBuilder()
                        .setAccountName(name)
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
                        .build());
    }

    /**
     * Unit test.
     */
    /**
     * Update account when all right.
     */
    @Test
    public void rightUpdateAccount() {
        TransactionResultWrapper ret = new TransactionResultWrapper();
        UpdateAccountOperator operator = new UpdateAccountOperator(
                getContract(ACCOUNT_NAME, OWNER_ADDRESS), dbManager);
        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert.assertEquals(ACCOUNT_NAME, accountWrapper.getAccountName().toStringUtf8());
            Assert.assertTrue(true);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void invalidAddress() {
        TransactionResultWrapper ret = new TransactionResultWrapper();
        UpdateAccountOperator operator = new UpdateAccountOperator(
                getContract(ACCOUNT_NAME, OWNER_ADDRESS_INVALID), dbManager);
        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertFalse(true);
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid ownerAddress", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void noExitAccount() {
        TransactionResultWrapper ret = new TransactionResultWrapper();
        UpdateAccountOperator operator = new UpdateAccountOperator(
                getContract(ACCOUNT_NAME, OWNER_ADDRESS_1), dbManager);
        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertFalse(true);
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Account has not existed", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    //@Test
    public void nameAlreadyUsed() {
        TransactionResultWrapper ret = new TransactionResultWrapper();
        UpdateAccountOperator operator = new UpdateAccountOperator(
                getContract(ACCOUNT_NAME, OWNER_ADDRESS), dbManager);
        UpdateAccountOperator operator1 = new UpdateAccountOperator(
                getContract(ACCOUNT_NAME, OWNER_ADDRESS_1), dbManager);
        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert.assertEquals(ACCOUNT_NAME, accountWrapper.getAccountName().toStringUtf8());
            Assert.assertTrue(true);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }

        AccountWrapper ownerWrapper =
                new AccountWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_1)),
                        ByteString.EMPTY,
                        AccountType.Normal);
        dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);

        try {
            operator1.validate();
            operator1.execute(ret);
            Assert.assertFalse(true);
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("This name has existed", e.getMessage());
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert.assertEquals(ACCOUNT_NAME, accountWrapper.getAccountName().toStringUtf8());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    /*
     * Account name need 8 - 32 bytes.
     */
    public void invalidName() {
        dbManager.getDynamicPropertiesStore().saveAllowUpdateAccountName(1);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        //Just OK 32 bytes is OK
        try {
            UpdateAccountOperator operator = new UpdateAccountOperator(
                    getContract("testname0123456789abcdefghijgklm", OWNER_ADDRESS), dbManager);
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert.assertEquals("testname0123456789abcdefghijgklm",
                    accountWrapper.getAccountName().toStringUtf8());
            Assert.assertTrue(true);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
        //8 bytes is OK
        AccountWrapper accountWrapper = dbManager.getAccountStore()
                .get(ByteArray.fromHexString(OWNER_ADDRESS));
        accountWrapper.setAccountName(ByteString.EMPTY.toByteArray());
        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
        try {
            UpdateAccountOperator operator = new UpdateAccountOperator(
                    getContract("testname", OWNER_ADDRESS), dbManager);
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            accountWrapper = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(OWNER_ADDRESS));
            Assert.assertEquals("testname",
                    accountWrapper.getAccountName().toStringUtf8());
            Assert.assertTrue(true);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
        //Empty name
        try {
            UpdateAccountOperator operator = new UpdateAccountOperator(
                    getContract(ByteString.EMPTY, OWNER_ADDRESS), dbManager);
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid accountName", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
        //Too long name 33 bytes
        try {
            UpdateAccountOperator operator = new UpdateAccountOperator(
                    getContract("testname0123456789abcdefghijgklmo0123456789abcdefghijgk"
                            + "lmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo"
                            + "0123456789abcdefghijgklmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo"
                            + "0123456789abcdefghijgklmo0123456789abcdefghijgklmo", OWNER_ADDRESS), dbManager);
            operator.validate();
            operator.execute(ret);
            Assert.assertFalse(true);
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid accountName", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }

        //@Test
//        public void twiceUpdateAccount () {
//            TransactionResultWrapper ret = new TransactionResultWrapper();
//            UpdateAccountOperator operator = new UpdateAccountOperator(
//                    getContract(ACCOUNT_NAME, OWNER_ADDRESS), dbManager);
//            UpdateAccountOperator operator1 = new UpdateAccountOperator(
//                    getContract(ACCOUNT_NAME_1, OWNER_ADDRESS), dbManager);
//            try {
//                operator.validate();
//                operator.execute(ret);
//                Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
//                AccountWrapper accountWrapper = dbManager.getAccountStore()
//                        .get(ByteArray.fromHexString(OWNER_ADDRESS));
//                Assert.assertEquals(ACCOUNT_NAME, accountWrapper.getAccountName().toStringUtf8());
//                Assert.assertTrue(true);
//            } catch (ContractValidateException e) {
//                Assert.assertFalse(e instanceof ContractValidateException);
//            } catch (ContractExeException e) {
//                Assert.assertFalse(e instanceof ContractExeException);
//            }
//
//            try {
//                operator1.validate();
//                operator1.execute(ret);
//                Assert.assertFalse(true);
//            } catch (ContractValidateException e) {
//                Assert.assertTrue(e instanceof ContractValidateException);
//                Assert.assertEquals("This account name already exist", e.getMessage());
//                AccountWrapper accountWrapper = dbManager.getAccountStore()
//                        .get(ByteArray.fromHexString(OWNER_ADDRESS));
//                Assert.assertEquals(ACCOUNT_NAME, accountWrapper.getAccountName().toStringUtf8());
//            } catch (ContractExeException e) {
//                Assert.assertFalse(e instanceof ContractExeException);
//            }
//        }

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