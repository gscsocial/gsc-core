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

public class ExchangeTransactionOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_ExchangeTransaction_test";
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
            10000_000_000L);
    AccountWrapper accountWrapper1 =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            20000_000_000L);

    dbManager.getAccountStore()
        .put(accountWrapper.getAddress().toByteArray(), accountWrapper);
    dbManager.getAccountStore()
        .put(accountWrapper1.getAddress().toByteArray(), accountWrapper1);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }

  private Any getContract(String address, long exchangeId, String tokenId,
      long quant, long expected) {
    return Any.pack(
        Contract.ExchangeTransactionContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setExchangeId(exchangeId)
            .setTokenId(ByteString.copyFrom(tokenId.getBytes()))
            .setQuant(quant)
            .setExpected(expected)
            .build());
  }

  private void InitExchangeBeforeSameTokenNameActive() {
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(
        AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom("abc".getBytes()))
            .setId(String.valueOf(1L))
            .build());
    AssetIssueWrapper assetIssueWrapper1 = new AssetIssueWrapper(
        AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom("def".getBytes()))
            .setId(String.valueOf(2L))
            .build());
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
    dbManager.getAssetIssueStore().put(assetIssueWrapper1.createDbKey(), assetIssueWrapper1);
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper1.createDbV2Key(), assetIssueWrapper1);

    //V1
    ExchangeWrapper exchangeWrapper =
        new ExchangeWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            1,
            1000000,
            "_".getBytes(),
            "abc".getBytes());
    exchangeWrapper.setBalance(1_000_000_000_000L, 10_000_000L); // 1M gsc == 10M abc
    ExchangeWrapper exchangeWrapper1 =
        new ExchangeWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            2,
            1000000,
            "abc".getBytes(),
            "def".getBytes());
    exchangeWrapper1.setBalance(100000000L, 200000000L);
    dbManager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);
    dbManager.getExchangeStore().put(exchangeWrapper1.createDbKey(), exchangeWrapper1);
    //V2
    ExchangeWrapper exchangeWrapper2 =
        new ExchangeWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            1,
            1000000,
            "_".getBytes(),
            "1".getBytes());
    exchangeWrapper2.setBalance(1_000_000_000_000L, 10_000_000L); // 1M gsc == 10M abc
    ExchangeWrapper exchangeWrapper3 =
        new ExchangeWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            2,
            1000000,
            "1".getBytes(),   //abc's Id
            "2".getBytes()); //def's Id
    exchangeWrapper3.setBalance(100000000L, 200000000L);
    dbManager.getExchangeV2Store().put(exchangeWrapper2.createDbKey(), exchangeWrapper2);
    dbManager.getExchangeV2Store().put(exchangeWrapper3.createDbKey(), exchangeWrapper3);
  }

  private void InitExchangeSameTokenNameActive() {
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(
        AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom("123".getBytes()))
            .setId(String.valueOf(1L))
            .build());
    AssetIssueWrapper assetIssueWrapper1 =
        new AssetIssueWrapper(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom("456".getBytes()))
                .setId(String.valueOf(2))
                .build());
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(),
        assetIssueWrapper);
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper1.createDbV2Key(),
        assetIssueWrapper1);

    ExchangeWrapper exchangeWrapper =
        new ExchangeWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            1,
            1000000,
            "_".getBytes(),
            "123".getBytes());
    exchangeWrapper.setBalance(1_000_000_000_000L, 10_000_000L); // 1M gsc == 10M abc
    ExchangeWrapper exchangeWrapper1 =
        new ExchangeWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            2,
            1000000,
            "123".getBytes(),
            "456".getBytes());
    exchangeWrapper1.setBalance(100000000L, 200000000L);
    dbManager.getExchangeV2Store().put(exchangeWrapper.createDbKey(), exchangeWrapper);
    dbManager.getExchangeV2Store().put(exchangeWrapper1.createDbKey(), exchangeWrapper1);
  }

  /**
   * SameTokenName close,first transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeTransaction() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000_000L; // use 100 gsc to buy abc

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get("def"));

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
      long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(1_000_000_000_000L, firstTokenBalance);
      Assert.assertEquals("abc", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(10_000_000L, secondTokenBalance);

      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      //V1
      exchangeWrapper = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals("abc", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(9999001L, exchangeWrapper.getSecondTokenBalance());
      //V2
      ExchangeWrapper exchangeWrapper1 =
          dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper1);
      Assert.assertEquals(exchangeId, exchangeWrapper1.getID());
      Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
      Assert.assertEquals(firstTokenBalance + quant, exchangeWrapper1.getFirstTokenBalance());
      Assert.assertEquals(9999001L, exchangeWrapper1.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountWrapper.getAssetMap();
      Assert.assertEquals(20000_000000L - quant, accountWrapper.getBalance());
      Assert.assertEquals(999L, assetMap.get("abc").longValue());

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
  public void oldNotUpdateSuccessExchangeTransaction() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000_000L; // use 100 gsc to buy abc

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get("def"));

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

    try {
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
      long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(1_000_000_000_000L, firstTokenBalance);
      Assert.assertEquals("abc", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(10_000_000L, secondTokenBalance);

      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      //V1 not update
      exchangeWrapper = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals("abc", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertNotEquals(firstTokenBalance + quant, exchangeWrapper.getFirstTokenBalance());
      Assert.assertNotEquals(9999001L, exchangeWrapper.getSecondTokenBalance());
      //V2
      ExchangeWrapper exchangeWrapper1 =
          dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper1);
      Assert.assertEquals(exchangeId, exchangeWrapper1.getID());
      Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
      Assert.assertEquals(firstTokenBalance + quant, exchangeWrapper1.getFirstTokenBalance());
      Assert.assertEquals(9999001L, exchangeWrapper1.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountWrapper.getAssetMapV2();
      Assert.assertEquals(20000_000000L - quant, accountWrapper.getBalance());
      Assert.assertEquals(999L, assetMap.get("1").longValue());

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
   * SameTokenName open,first transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeTransaction() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000_000L; // use 100 gsc to buy abc

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get("456"));

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
      long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(1_000_000_000_000L, firstTokenBalance);
      Assert.assertEquals("123", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(10_000_000L, secondTokenBalance);

      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(exchangeId)));

      //V2
      exchangeWrapper = dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals("123", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(9999001L, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      assetV2Map = accountWrapper.getAssetMapV2();
      Assert.assertEquals(20000_000000L - quant, accountWrapper.getBalance());
      Assert.assertEquals(999L, assetV2Map.get("123").longValue());

      Assert.assertEquals(999L, ret.getExchangeReceivedAmount());

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
   * SameTokenName close,second transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeTransaction2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
      long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(100000000L, firstTokenBalance);
      Assert.assertEquals("def", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(200000000L, secondTokenBalance);

      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      //V1
      exchangeWrapper = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals("def", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(199998001L, exchangeWrapper.getSecondTokenBalance());
      //V2
      ExchangeWrapper exchangeWrapper1 =
          dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper1);
      Assert.assertEquals(exchangeId, exchangeWrapper1.getID());
      Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
//      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeWrapper1.getFirstTokenId()));
//      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper1.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeWrapper1.getFirstTokenBalance());
//      Assert.assertEquals("def", ByteArray.toStr(exchangeWrapper1.getSecondTokenId()));
      Assert.assertEquals(199998001L, exchangeWrapper1.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountWrapper.getAssetMap();
      Assert.assertEquals(9000L, assetMap.get("abc").longValue());
      Assert.assertEquals(1999L, assetMap.get("def").longValue());

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
   * SameTokenName open,second transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeTransaction2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
      long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(100000000L, firstTokenBalance);
      Assert.assertEquals("456", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(200000000L, secondTokenBalance);

      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(exchangeId)));

      //V2
      exchangeWrapper = dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(exchangeId, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals("456", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(199998001L, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      assetV2Map = accountWrapper.getAssetMapV2();
      Assert.assertEquals(9000L, assetV2Map.get("123").longValue());
      Assert.assertEquals(1999L, assetV2Map.get("456").longValue());

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
   * SameTokenName close,use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void SameTokenNameCloseInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_INVALID, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void SameTokenNameOpenInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_INVALID, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,use AccountStore not exists, result is failed, exception is "account not
   * exists".
   */
  @Test
  public void SameTokenNameCloseNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_NOACCOUNT, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,use AccountStore not exists, result is failed, exception is "account not
   * exists".
   */
  @Test
  public void SameTokenNameOpenNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_NOACCOUNT, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,Exchange not exists
   */
  @Test
  public void SameTokenNameCloseExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 3;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,Exchange not exists
   */
  @Test
  public void SameTokenNameOpenExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 3;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,token is not in exchange
   */
  @Test
  public void SameTokenNameCloseTokenIsNotInExchange() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "ddd";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token is not in exchange",
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
   * SameTokenName open,token is not in exchange
   */
  @Test
  public void SameTokenNameOpenTokenIsNotInExchange() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "999";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token is not in exchange",
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
   * SameTokenName close,Token balance in exchange is equal with 0, the exchange has been closed"
   */
  @Test
  public void SameTokenNameCloseTokenBalanceZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,Token balance in exchange is equal with 0, the exchange has been closed"
   */
  @Test
  public void SameTokenNameOpenTokenBalanceZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close,token quant must greater than zero
   */
  @Test
  public void SameTokenNameCloseTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = -1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token quant must greater than zero",
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
   * SameTokenName open,token quant must greater than zero
   */
  @Test
  public void SameTokenNameOpenTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = -1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token quant must greater than zero",
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
   * SameTokenName close,token balance must less than balanceLimit
   */
  @Test
  public void SameTokenNameCloseTokenBalanceGreaterThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_00_000_000_000_001L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,token balance must less than balanceLimit
   */
  @Test
  public void SameTokenNameOpenTokenBalanceGreaterThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_00_000_000_000_001L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,balance is not enough
   */
  @Test
  public void SameTokenNameCloseBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000000L;
    String buyTokenId = "abc";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    accountWrapper.setBalance(quant - 1);
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,balance is not enough
   */
  @Test
  public void SameTokenNameOpenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000000L;
    String buyTokenId = "123";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    accountWrapper.setBalance(quant - 1);
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,token balance is not enough
   */
  @Test
  public void SameTokenNameOpenTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), quant - 1, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,token required must greater than expected
   */
  @Test
  public void SameTokenNameCloseTokenRequiredNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), quant);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    long expected = 0;
    try {
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      expected = exchangeWrapper.transaction(tokenId.getBytes(), quant);
    } catch (ItemNotFoundException e) {
      fail();
    }

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, expected + 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token required must greater than expected",
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
   * SameTokenName open,token required must greater than expected
   */
  @Test
  public void SameTokenNameOpenTokenRequiredNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(tokenId.getBytes(), quant, dbManager);
    Map<String, Long> assetV2Map = accountWrapper.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    long expected = 0;
    try {
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
          .get(ByteArray.fromLong(exchangeId));
      expected = exchangeWrapper.transaction(tokenId.getBytes(), quant);
    } catch (ItemNotFoundException e) {
      fail();
    }

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, expected + 1),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token required must greater than expected",
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

  @Test
  public void SameTokenNameCloseTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(tokenId.getBytes(), quant - 1);
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    Assert.assertEquals(20000_000000L, accountWrapper.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,invalid param "token id is not a valid number" "token expected must greater
   * than zero"
   */
  @Test
  public void SameTokenNameOpenInvalidParam() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    long quant = 100_000_000L; // use 100 gsc to buy abc
    TransactionResultWrapper ret = new TransactionResultWrapper();

    //token id is not a valid number
    ExchangeTransactionOperator operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, "abc", quant, 1),
        dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token id is not a valid number",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //token expected must greater than zero
    operator = new ExchangeTransactionOperator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, "_", quant, 0),
        dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token expected must greater than zero",
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