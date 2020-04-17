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

public class ExchangeCreateOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_ExchangeCreate_test";
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

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void initTest() {
    AccountWrapper ownerAccountFirstWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            300_000_000L);
    AccountWrapper ownerAccountSecondWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstWrapper.getAddress().toByteArray(), ownerAccountFirstWrapper);
    dbManager.getAccountStore()
        .put(ownerAccountSecondWrapper.getAddress().toByteArray(), ownerAccountSecondWrapper);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().saveLatestExchangeNum(0);

  }

  private Any getContract(String address, String firstTokenId, long firstTokenBalance,
      String secondTokenId, long secondTokenBalance) {
    return Any.pack(
        Contract.ExchangeCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setFirstTokenId(ByteString.copyFrom(firstTokenId.getBytes()))
            .setFirstTokenBalance(firstTokenBalance)
            .setSecondTokenId(ByteString.copyFrom(secondTokenId.getBytes()))
            .setSecondTokenBalance(secondTokenBalance)
            .build());
  }

  /**
   * SameTokenName close,first createExchange,result is success.
   */
  @Test
  public void sameTokenNameCloseSuccessExchangeCreate() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    AssetIssueWrapper assetIssueWrapper =
        new AssetIssueWrapper(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                .build());
    assetIssueWrapper.setId(String.valueOf(1L));

    AssetIssueWrapper assetIssueWrapper1 =
        new AssetIssueWrapper(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                .build());
    assetIssueWrapper1.setId(String.valueOf(2L));

    dbManager.getAssetIssueStore()
        .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);
    dbManager.getAssetIssueStore()
        .put(assetIssueWrapper1.getName().toByteArray(), assetIssueWrapper1);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);

      Assert.assertEquals(ret.getInstance().getExchangeId(), 1L);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);
      // check old(V1) version
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountWrapper.getAssetMap();
      Assert.assertEquals(10000_000000L - 1000_000000L, accountWrapper.getBalance());
      Assert.assertEquals(0L, assetMap.get(firstTokenId).longValue());
      Assert.assertEquals(0L, assetMap.get(secondTokenId).longValue());

      // check V2 version
      ExchangeWrapper exchangeWrapper1 = dbManager.getExchangeV2Store()
          .get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper1);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper1.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper1.getID());
      Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
      // convert
      firstTokenId = dbManager.getAssetIssueStore().get(firstTokenId.getBytes()).getId();
      secondTokenId = dbManager.getAssetIssueStore().get(secondTokenId.getBytes()).getId();
      Assert
          .assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper1.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper1.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper1.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper1.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper1.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountWrapper.getAssetMapV2();
      Assert.assertEquals(10000_000000L - 1000_000000L, accountWrapper.getBalance());
      Assert.assertEquals(0L, getAssetV2Map.get(firstTokenId).longValue());
      Assert.assertEquals(0L, getAssetV2Map.get(secondTokenId).longValue());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * Init close SameTokenName,after init data,open SameTokenName
   */
  @Test
  public void oldNotUpdateSuccessExchangeCreate2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(
        AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom(secondTokenId.getBytes()))
            .build());
    assetIssueWrapper.setId(String.valueOf(1L));
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(200_000_000_000000L);
    accountWrapper.addAsset(secondTokenId.getBytes(), 200_000_000L);
    accountWrapper.addAssetV2(String.valueOf(1L).getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, String.valueOf(1L),
        secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(id)));
      // check V2 version
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
          .get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      secondTokenId = dbManager.getAssetIssueStore().get(secondTokenId.getBytes()).getId();
      Assert
          .assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountWrapper.getAssetMapV2();
      Assert.assertEquals(200_000_000_000000L - 1000_000000L - firstTokenBalance,
          accountWrapper.getBalance());
      Assert.assertEquals(100_000_000L, getAssetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName open,first createExchange,result is success.
   */
  @Test
  public void sameTokenNameOpenSuccessExchangeCreate() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100000000L;

    AssetIssueWrapper assetIssueWrapper =
        new AssetIssueWrapper(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                .build());
    assetIssueWrapper.setId(String.valueOf(1L));

    AssetIssueWrapper assetIssueWrapper1 =
        new AssetIssueWrapper(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                .build());
    assetIssueWrapper1.setId(String.valueOf(2L));

    dbManager.getAssetIssueStore()
        .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);
    dbManager.getAssetIssueStore()
        .put(assetIssueWrapper1.getName().toByteArray(), assetIssueWrapper1);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmountV2(firstTokenId.getBytes(), firstTokenBalance, dbManager);
    accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), secondTokenBalance, dbManager);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);

      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(id)));

      // check V2 version
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
          .get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());

      Assert
          .assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountWrapper.getAssetMapV2();
      Assert.assertEquals(10000_000000L - 1000_000000L, accountWrapper.getBalance());
      Assert.assertEquals(0L, getAssetV2Map.get(firstTokenId).longValue());
      Assert.assertEquals(0L, getAssetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName open,second create Exchange, result is success.
   */
  @Test
  public void sameTokenNameOpenSuccessExchangeCreate2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    AssetIssueWrapper assetIssueWrapper =
        new AssetIssueWrapper(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                .build());
    assetIssueWrapper.setId(String.valueOf(1L));
    dbManager.getAssetIssueStore()
        .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(200_000_000_000000L);
    accountWrapper.addAssetAmountV2(secondTokenId.getBytes(), 200_000_000L, dbManager);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(id)));
      // check V2 version
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeV2Store()
          .get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert
          .assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountWrapper.getAssetMapV2();
      Assert.assertEquals(200_000_000_000000L - 1000_000000L - firstTokenBalance,
          accountWrapper.getBalance());
      Assert.assertEquals(100_000_000L, getAssetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }


  /**
   * SameTokenName open,first createExchange,result is failure.
   */
  @Test
  public void sameTokenNameOpenExchangeCreateFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token id is not a valid number", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open,second create Exchange, result is failure.
   */
  @Test
  public void sameTokenNameOpenExchangeCreateFailure2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(200_000_000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("second token id is not a valid number", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName close, use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void sameTokenNameCloseInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_INVALID, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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


  /**
   * SameTokenName open, use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void sameTokenNameOpenInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_INVALID, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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

  /**
   * SameTokenName close, use AccountStore not exists, result is failed, exception is "account not
   * exists".
   */
  @Test
  public void sameTokenNameCloseNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_NOACCOUNT, firstTokenId, firstTokenBalance, secondTokenId,
        secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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
    }
  }

  /**
   * SameTokenName open, use AccountStore not exists, result is failed, exception is "account not
   * exists".
   */
  @Test
  public void sameTokenNameOpenNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_NOACCOUNT, firstTokenId, firstTokenBalance, secondTokenId,
        secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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
    }
  }

  /**
   * SameTokenName close,No enough balance
   */
  @Test
  public void sameTokenNameCloseNoEnoughBalance() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(100000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No enough balance for exchange create fee!",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,No enough balance
   */
  @Test
  public void sameTokenNameOpenNoEnoughBalance() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "123";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "345";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(100000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No enough balance for exchange create fee!",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,exchange same tokens
   */
  @Test
  public void sameTokenNameCloseSameTokens() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("cannot exchange same tokens",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open,exchange same tokens
   */
  @Test
  public void sameTokenNameOpenSameTokens() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,token balance less than zero
   */
  @Test
  public void sameTokenNameCloseLessToken() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 0L;
    String secondTokenId = "def";
    long secondTokenBalance = 0L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), 1000);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 1000);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,token balance less than zero
   */
  @Test
  public void sameTokenNameOpenLessToken() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 0L;
    String secondTokenId = "456";
    long secondTokenBalance = 0L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), 1000);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 1000);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,token balance must less than balanceLimit
   */
  @Test
  public void sameTokenNameCloseMoreThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 1_00_000_000_000_001L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
    }
  }


  /**
   * SameTokenName open,token balance must less than balanceLimit
   */
  @Test
  public void sameTokenNameOpenMoreThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 1_00_000_000_000_001L;
    String secondTokenId = "456";
    long secondTokenBalance = 100000000L;
    // 10 00 000 000  000 000
    // 1  00 000 000  000 001
    // 1  00 000 000  000 000

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
    }
  }

  /**
   * SameTokenName close,balance is not enough
   */
  @Test
  public void sameTokenNameCloseBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(firstTokenBalance + 1000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
    }
  }


  /**
   * SameTokenName open,balance is not enough
   */
  @Test
  public void sameTokenNameOpenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(firstTokenBalance + 1000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
    }
  }

  /**
   * SameTokenName close,second create Exchange, result is success.
   */
  @Test
  public void sameTokenNameCloseSuccessExchangeCreate2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    AssetIssueWrapper assetIssueWrapper =
            new AssetIssueWrapper(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                            .build());
    assetIssueWrapper.setId(String.valueOf(1L));
    dbManager.getAssetIssueStore()
            .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(200_000_000_000000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);
      // check old version
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountWrapper.getAssetMap();
      Assert.assertEquals(200_000_000_000000L - 1000_000000L - firstTokenBalance,
              accountWrapper.getBalance());
      Assert.assertEquals(100_000_000L, assetMap.get(secondTokenId).longValue());

      // check V2 version
      ExchangeWrapper exchangeWrapper1 = dbManager.getExchangeV2Store()
              .get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper1);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper1.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper1.getID());
      Assert.assertEquals(1000000, exchangeWrapper1.getCreateTime());
      secondTokenId = dbManager.getAssetIssueStore().get(secondTokenId.getBytes()).getId();
      Assert
              .assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper1.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper1.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper1.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper1.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper1.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountWrapper.getAssetMapV2();
      Assert.assertEquals(200_000_000_000000L - 1000_000000L - firstTokenBalance,
              accountWrapper.getBalance());
      Assert.assertEquals(100_000_000L, getAssetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName close,first token balance is not enough
   */
  @Test
  public void sameTokenNameCloseFirstTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance - 1000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,first token balance is not enough
   */
  @Test
  public void sameTokenNameOpenFirstTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance - 1000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,balance is not enough
   */
  @Test
  public void sameTokenNameCloseBalanceNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000L;
    String secondTokenId = "_";
    long secondTokenBalance = 100_000_000_000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(secondTokenBalance + 1000L);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
    }
  }


  /**
   * SameTokenName open,balance is not enough
   */
  @Test
  public void sameTokenNameOpenBalanceNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000L;
    String secondTokenId = "_";
    long secondTokenBalance = 100_000_000_000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(secondTokenBalance + 1000L);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,first token balance is not enough
   */
  @Test
  public void sameTokenNameCloseSecondTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 90_000_000L);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("second token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,first token balance is not enough
   */
  @Test
  public void sameTokenNameOpenSecondTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 90_000_000L);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,not gsc,ont token is ok, but the second one is not exist.
   */
  @Test
  public void sameTokenNameCloseSecondTokenNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("second token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,not gsc,ont token is ok, but the second one is not exist.
   */
  @Test
  public void sameTokenNameOpenSecondTokenNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator operator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      operator.validate();
      operator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
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