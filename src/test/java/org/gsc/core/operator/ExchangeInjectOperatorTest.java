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

import static org.testng.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

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
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j

public class ExchangeInjectOperatorTest {

    private static GSCApplicationContext context;
    private static Manager dbManager;
    private static final String dbPath = "db_ExchangeInject_test";
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
        AccountWrapper accountWrapper =
                new AccountWrapper(
                        ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                        AccountType.Normal,
                        300_000_000L);
        AccountWrapper accountWrapper1 =
                new AccountWrapper(
                        ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
                        AccountType.Normal,
                        200_000_000_000L);

        dbManager.getAccountStore()
                .put(accountWrapper.getAddress().toByteArray(), accountWrapper);
        dbManager.getAccountStore()
                .put(accountWrapper1.getAddress().toByteArray(), accountWrapper1);

        dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
        dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
        dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    }

    private Any getContract(String address, long exchangeId, String tokenId, long quant) {
        return Any.pack(
                Contract.ExchangeInjectContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
                        .setExchangeId(exchangeId)
                        .setTokenId(ByteString.copyFrom(tokenId.getBytes()))
                        .setQuant(quant)
                        .build());
    }

    private void InitExchangeBeforeSameTokenNameActive() {
        //V1
        ExchangeWrapper exchangeWrapper =
                new ExchangeWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                        1,
                        1000000,
                        "abc".getBytes(),
                        "def".getBytes());
        exchangeWrapper.setBalance(100000000L, 200000000L);
        ExchangeWrapper exchangeWrapper1 =
                new ExchangeWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                        2,
                        1000000,
                        "_".getBytes(),
                        "def".getBytes());
        exchangeWrapper1.setBalance(1_000_000_000000L, 10_000_000L);
        dbManager.getExchangeStore()
                .put(exchangeWrapper.createDbKey(), exchangeWrapper);
        dbManager.getExchangeStore()
                .put(exchangeWrapper1.createDbKey(), exchangeWrapper1);
        //V2
        ExchangeWrapper exchangeWrapper2 =
                new ExchangeWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                        1,
                        1000000,
                        "1".getBytes(),
                        "2".getBytes());
        exchangeWrapper2.setBalance(100000000L, 200000000L);
        ExchangeWrapper exchangeWrapper3 =
                new ExchangeWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                        2,
                        1000000,
                        "_".getBytes(),
                        "2".getBytes());
        exchangeWrapper3.setBalance(1_000_000_000000L, 10_000_000L);
        dbManager.getExchangeV2Store()
                .put(exchangeWrapper2.createDbKey(), exchangeWrapper2);
        dbManager.getExchangeV2Store()
                .put(exchangeWrapper3.createDbKey(), exchangeWrapper3);
    }

    private void InitExchangeSameTokenNameActive() {

        ExchangeWrapper exchangeWrapper =
                new ExchangeWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                        1,
                        1000000,
                        "123".getBytes(),
                        "456".getBytes());
        exchangeWrapper.setBalance(100000000L, 200000000L);
        ExchangeWrapper exchangeWrapper1 =
                new ExchangeWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                        2,
                        1000000,
                        "_".getBytes(),
                        "456".getBytes());
        exchangeWrapper1.setBalance(1_000_000_000000L, 10_000_000L);

        dbManager.getExchangeV2Store()
                .put(exchangeWrapper.createDbKey(), exchangeWrapper);
        dbManager.getExchangeV2Store()
                .put(exchangeWrapper1.createDbKey(), exchangeWrapper1);
    }

    /**
     * SameTokenName close, first inject Exchange,result is success.
     */
    @Test
    public void SameTokenNameCloseSuccessExchangeInject() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        AssetIssueWrapper assetIssueWrapper =
                new AssetIssueWrapper(
                        AssetIssueContract.newBuilder()
                                .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                                .build());
        assetIssueWrapper.setId(String.valueOf(1L));
        dbManager.getAssetIssueStore()
                .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);
        AssetIssueWrapper assetIssueWrapper1 =
                new AssetIssueWrapper(
                        AssetIssueContract.newBuilder()
                                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                                .build());
        assetIssueWrapper1.setId(String.valueOf(2L));
        dbManager.getAssetIssueStore()
                .put(assetIssueWrapper1.getName().toByteArray(), assetIssueWrapper1);

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            Assert.assertEquals(ret.getExchangeInjectAnotherAmount(), secondTokenQuant);
            //V1
            ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
                    .get(ByteArray.fromLong(exchangeId));
            Assert.assertNotNull(exchangeWrapper);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
            Assert.assertEquals(exchangeId, exchangeWrapper.getID());
            Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
            Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(300000000L, exchangeWrapper.getFirstTokenBalance());
            Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
            Assert.assertEquals(600000000L, exchangeWrapper.getSecondTokenBalance());
            //V2
            ExchangeWrapper exchangeWrapper1 =
                    dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
            Assert.assertNotNull(exchangeWrapper1);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper1.getCreatorAddress());
            Assert.assertEquals(exchangeId, exchangeWrapper1.getID());
            Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
            Assert.assertEquals(300000000L, exchangeWrapper1.getFirstTokenBalance());
            Assert.assertEquals(600000000L, exchangeWrapper1.getSecondTokenBalance());

            accountWrapper = dbManager.getAccountStore().get(ownerAddress);
            Map<String, Long> assetMap = accountWrapper.getAssetMap();
            Assert.assertEquals(10000_000000L, accountWrapper.getBalance());
            Assert.assertEquals(0L, assetMap.get(firstTokenId).longValue());
            Assert.assertEquals(0L, assetMap.get(secondTokenId).longValue());

        } catch (ContractValidateException e) {
            logger.info(e.getMessage());
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } catch (ItemNotFoundException e) {
            Assert.assertFalse(e instanceof ItemNotFoundException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * Init close SameTokenName,after init data,open SameTokenName
     */
    @Test
    public void OldNotUpdateSuccessExchangeInject() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(
                AssetIssueContract.newBuilder()
                        .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                        .setId(String.valueOf(1L))
                        .build());
        dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
        dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

        AssetIssueWrapper assetIssueWrapper1 = new AssetIssueWrapper(
                AssetIssueContract.newBuilder()
                        .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                        .setId(String.valueOf(2L))
                        .build());
        dbManager.getAssetIssueStore().put(assetIssueWrapper1.createDbKey(), assetIssueWrapper1);
        dbManager.getAssetIssueV2Store().put(assetIssueWrapper1.createDbV2Key(), assetIssueWrapper1);

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAsset(firstTokenId.getBytes(), firstTokenQuant);
        accountWrapper.addAsset(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.addAssetV2(String.valueOf(1L).getBytes(), firstTokenQuant);
        accountWrapper.addAssetV2(String.valueOf(2L).getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, String.valueOf(1L), firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            Assert.assertEquals(ret.getExchangeInjectAnotherAmount(), secondTokenQuant);
            //V1
            ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
                    .get(ByteArray.fromLong(exchangeId));
            Assert.assertNotNull(exchangeWrapper);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
            Assert.assertEquals(exchangeId, exchangeWrapper.getID());
            Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
            Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));

            Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
            Assert.assertNotEquals(300000000L, exchangeWrapper.getFirstTokenBalance());
            Assert.assertNotEquals(600000000L, exchangeWrapper.getSecondTokenBalance());
            //V2
            ExchangeWrapper exchangeWrapper1 =
                    dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
            Assert.assertNotNull(exchangeWrapper1);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper1.getCreatorAddress());
            Assert.assertEquals(exchangeId, exchangeWrapper1.getID());
            Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
            Assert.assertEquals(300000000L, exchangeWrapper1.getFirstTokenBalance());
            Assert.assertEquals(600000000L, exchangeWrapper1.getSecondTokenBalance());

            accountWrapper = dbManager.getAccountStore().get(ownerAddress);
            Map<String, Long> assetMap = accountWrapper.getAssetMapV2();
            Assert.assertEquals(10000_000000L, accountWrapper.getBalance());
            Assert.assertEquals(0L, assetMap.get(String.valueOf(1)).longValue());
            Assert.assertEquals(0L, assetMap.get(String.valueOf(2)).longValue());

        } catch (ContractValidateException e) {
            logger.info(e.getMessage());
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } catch (ItemNotFoundException e) {
            Assert.assertFalse(e instanceof ItemNotFoundException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, first inject Exchange,result is success.
     */
    @Test
    public void SameTokenNameOpenSuccessExchangeInject() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        AssetIssueWrapper assetIssueWrapper =
                new AssetIssueWrapper(
                        AssetIssueContract.newBuilder()
                                .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                                .build());
        assetIssueWrapper.setId(String.valueOf(1L));
        dbManager.getAssetIssueStore()
                .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);
        AssetIssueWrapper assetIssueWrapper1 =
                new AssetIssueWrapper(
                        AssetIssueContract.newBuilder()
                                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                                .build());
        assetIssueWrapper1.setId(String.valueOf(2L));
        dbManager.getAssetIssueStore()
                .put(assetIssueWrapper1.getName().toByteArray(), assetIssueWrapper1);

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            long id = 1;
            // V1,Data is no longer update
            Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(id)));

            //V2
            ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
                    .get(ByteArray.fromLong(id));
            Assert.assertNotNull(exchangeWrapper);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
            Assert.assertEquals(id, exchangeWrapper.getID());
            Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
            Assert
                    .assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(300000000L, exchangeWrapper.getFirstTokenBalance());
            Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
            Assert.assertEquals(600000000L, exchangeWrapper.getSecondTokenBalance());

            accountWrapper = dbManager.getAccountStore().get(ownerAddress);
            Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
            Assert.assertEquals(10000_000000L, accountWrapper.getBalance());
            Assert.assertEquals(0L, assetV2Map.get(firstTokenId).longValue());
            Assert.assertEquals(0L, assetV2Map.get(secondTokenId).longValue());

        } catch (ContractValidateException e) {
            logger.info(e.getMessage());
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } catch (ItemNotFoundException e) {
            Assert.assertFalse(e instanceof ItemNotFoundException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, second inject Exchange,result is success.
     */
    @Test
    public void SameTokenNameCloseSuccessExchangeInject2() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 100_000_000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 4_000_000L;
        AssetIssueWrapper assetIssueWrapper =
                new AssetIssueWrapper(
                        AssetIssueContract.newBuilder()
                                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                                .build());
        assetIssueWrapper.setId(String.valueOf(2L));
        dbManager.getAssetIssueStore()
                .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            //V1
            ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
                    .get(ByteArray.fromLong(exchangeId));
            Assert.assertNotNull(exchangeWrapper);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
            Assert.assertEquals(exchangeId, exchangeWrapper.getID());
            Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
            Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(1_100_000_000000L, exchangeWrapper.getFirstTokenBalance());
            Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
            Assert.assertEquals(11_000_000L, exchangeWrapper.getSecondTokenBalance());
            //V2
            ExchangeWrapper exchangeWrapper1 = dbManager.getExchangeV2Store()
                    .get(ByteArray.fromLong(exchangeId));
            Assert.assertNotNull(exchangeWrapper1);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper1.getCreatorAddress());
            Assert.assertEquals(exchangeId, exchangeWrapper1.getID());
            Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
            Assert.assertEquals(1_100_000_000000L, exchangeWrapper1.getFirstTokenBalance());
            Assert.assertEquals(11_000_000L, exchangeWrapper1.getSecondTokenBalance());

            accountWrapper = dbManager.getAccountStore().get(ownerAddress);
            Map<String, Long> assetMap = accountWrapper.getAssetMap();
            Assert.assertEquals(0L, accountWrapper.getBalance());
            Assert.assertEquals(3_000_000L, assetMap.get(secondTokenId).longValue());

        } catch (ContractValidateException e) {
            logger.info(e.getMessage());
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } catch (ItemNotFoundException e) {
            Assert.assertFalse(e instanceof ItemNotFoundException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, second inject Exchange,result is success.
     */
    @Test
    public void SameTokenNameOpenSuccessExchangeInject2() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 100_000_000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 4_000_000L;
        AssetIssueWrapper assetIssueWrapper =
                new AssetIssueWrapper(
                        AssetIssueContract.newBuilder()
                                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                                .build());
        assetIssueWrapper.setId(String.valueOf(2L));
        dbManager.getAssetIssueV2Store()
                .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
            // V1,Data is no longer update
            Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(exchangeId)));

            //V2
            ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
                    .get(ByteArray.fromLong(exchangeId));
            Assert.assertNotNull(exchangeWrapper);
            Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
            Assert.assertEquals(exchangeId, exchangeWrapper.getID());
            Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
            Assert
                    .assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
            Assert.assertEquals(1_100_000_000000L, exchangeWrapper.getFirstTokenBalance());
            Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
            Assert.assertEquals(11_000_000L, exchangeWrapper.getSecondTokenBalance());

            accountWrapper = dbManager.getAccountStore().get(ownerAddress);
            Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
            Assert.assertEquals(0L, accountWrapper.getBalance());
            Assert.assertEquals(3_000_000L, assetV2Map.get(secondTokenId).longValue());

        } catch (ContractValidateException e) {
            logger.info(e.getMessage());
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } catch (ItemNotFoundException e) {
            Assert.assertFalse(e instanceof ItemNotFoundException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, use Invalid Address, result is failed, exception is "Invalid address".
     */
    @Test
    public void SameTokenNameCloseInvalidAddress() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_INVALID, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
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
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, use Invalid Address, result is failed, exception is "Invalid address".
     */
    @Test
    public void SameTokenNameOpenInvalidAddress() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_INVALID, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
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
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, use AccountStore not exists, result is failed, exception is "account not
     * exists".
     */
    @Test
    public void SameTokenNameCloseNoAccount() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_NOACCOUNT, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, use AccountStore not exists, result is failed, exception is "account not
     * exists".
     */
    @Test
    public void SameTokenNameOpenNoAccount() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_NOACCOUNT, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, Exchange not exists
     */
    @Test
    public void SameTokenNameCloseExchangeNotExist() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 3;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail("Exchange not exists");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Exchange[3] not exists",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, Exchange not exists
     */
    @Test
    public void SameTokenNameOpenExchangeNotExist() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 3;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail("Exchange not exists");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Exchange[3] not exists",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, account[" + readableOwnerAddress + "] is not creator
     */
    @Test
    public void SameTokenNameCloseAccountIsNotCreator() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_SECOND, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("account[01f80c548794500882809695a8a687866e76d4271a1abc]"
                            + " is not creator",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, account[" + readableOwnerAddress + "] is not creator
     */
    @Test
    public void SameTokenNameOpenAccountIsNotCreator() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_SECOND, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("account[01f80c548794500882809695a8a687866e76d4271a1abc]"
                            + " is not creator",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, token is not in exchange
     */
    @Test
    public void SameTokenNameCloseTokenIsNotInExchange() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "_";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("token id is not in exchange",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, token is not in exchange
     */
    @Test
    public void SameTokenNameOpenTokenIsNotInExchange() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "_";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("token id is not in exchange",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, Token balance in exchange is equal with 0, the exchange has been closed"
     */
    @Test
    public void SameTokenNameCloseTokenBalanceZero() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
                    .get(ByteArray.fromLong(exchangeId));
            exchangeWrapper.setBalance(0, 0);
            dbManager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);

            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Token balance in exchange is equal with 0,"
                            + "the exchange has been closed",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } catch (ItemNotFoundException e) {
            Assert.assertFalse(e instanceof ItemNotFoundException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, Token balance in exchange is equal with 0, the exchange has been closed"
     */
    @Test
    public void SameTokenNameOpenTokenBalanceZero() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
                    .get(ByteArray.fromLong(exchangeId));
            exchangeWrapper.setBalance(0, 0);
            dbManager.getExchangeV2Store().put(exchangeWrapper.createDbKey(), exchangeWrapper);

            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Token balance in exchange is equal with 0,"
                            + "the exchange has been closed",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } catch (ItemNotFoundException e) {
            Assert.assertFalse(e instanceof ItemNotFoundException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, injected token quant must greater than zero
     */
    @Test
    public void SameTokenNameCloseTokenQuantLessThanZero() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = -1L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(firstTokenId.getBytes(), 1000L);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("injected token quant must greater than zero",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, injected token quant must greater than zero
     */
    @Test
    public void SameTokenNameOpenTokenQuantLessThanZero() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = -1L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), 1000L, dbManager);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("injected token quant must greater than zero",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, "the calculated token quant  must be greater than 0"
     */
    @Test
    public void SameTokenNameCloseCalculatedTokenQuantLessThanZero() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 100L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("the calculated token quant  must be greater than 0",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, "the calculated token quant  must be greater than 0"
     */
    @Test
    public void SameTokenNameOpenCalculatedTokenQuantLessThanZero() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 100L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("the calculated token quant  must be greater than 0",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            ;
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, token balance must less than balanceLimit
     */
    @Test
    public void SameTokenNameCloseTokenBalanceGreaterThanBalanceLimit() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 1_00_000_000_000_000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("token balance must less than 100000000000000L",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, token balance must less than balanceLimit
     */
    @Test
    public void SameTokenNameOpenTokenBalanceGreaterThanBalanceLimit() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 1_00_000_000_000_000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;
        // 100000000_000_000L
        // 1000000000000 000
        // 100000000000  000

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(firstTokenQuant);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("token balance must less than 100000000000000L",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, balance is not enough
     */
    @Test
    public void SameTokenNameCloseBalanceNotEnough() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 100_000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(firstTokenQuant - 1);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, balance is not enough
     */
    @Test
    public void SameTokenNameOpenBalanceNotEnough() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 2;
        String firstTokenId = "_";
        long firstTokenQuant = 100_000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(firstTokenQuant - 1);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, first token balance is not enough
     */
    @Test
    public void SameTokenNameCloseTokenBalanceNotEnough() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant - 1);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("token balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, first token balance is not enough
     */
    @Test
    public void SameTokenNameOpenTokenBalanceNotEnough() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant - 1, dbManager);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("token balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, balance is not enough2
     */
    @Test
    public void SameTokenNameCloseBalanceNotEnough2() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 2;
        String secondTokenId = "def";
        long secondTokenQuant = 4000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(399_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, balance is not enough2
     */
    @Test
    public void SameTokenNameOpenBalanceNotEnough2() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 2;
        String secondTokenId = "456";
        long secondTokenQuant = 4000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(399_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName close, first token balance is not enough
     */
    @Test
    public void SameTokenNameCloseAnotherTokenBalanceNotEnough() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
        InitExchangeBeforeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "abc";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "def";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant - 1);
        accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("another token balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, first token balance is not enough
     */
    @Test
    public void SameTokenNameOpenAnotherTokenBalanceNotEnough() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        String firstTokenId = "123";
        long firstTokenQuant = 200000000L;
        String secondTokenId = "456";
        long secondTokenQuant = 400000000L;

        byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant - 1, dbManager);
        accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
        accountWrapper.setBalance(10000_000000L);
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();

        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("another token balance is not enough",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
        }
    }

    /**
     * SameTokenName open, invalid param "token id is not a valid number"
     */
    @Test
    public void SameTokenNameOpenInvalidParam() {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        InitExchangeSameTokenNameActive();
        long exchangeId = 1;
        ExchangeInjectOperator operator = new ExchangeInjectOperator(getContract(
                OWNER_ADDRESS_FIRST, exchangeId, "abc", 1000),
                dbManager);
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            operator.validate();
            operator.execute(ret);
            fail();
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("token id is not a valid number",
                    e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        } finally {
            dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
            dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
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