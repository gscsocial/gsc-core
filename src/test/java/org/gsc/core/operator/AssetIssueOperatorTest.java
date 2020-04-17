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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
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
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AssetIssueContract.FrozenSupply;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class AssetIssueOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_assetIssue_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_SECOND;
  private static final String NAME = "gsc-my";
  private static final long TOTAL_SUPPLY = 10000L;
  private static final int GSC_NUM = 10000;
  private static final int NUM = 100000;
  private static final String DESCRIPTION = "myCoin";
  private static final String URL = "gsc-my.com";
  private static final String ASSET_NAME_SECOND = "asset_name2";
  private static long now = 0;
  private static long startTime = 0;
  private static long endTime = 0;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
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
  public void createWrapper() {
    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    AccountWrapper ownerSecondWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("ownerSecond"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
    dbManager.getAccountStore().put(
        ownerSecondWrapper.getAddress().toByteArray(), ownerSecondWrapper);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(24 * 3600 * 1000);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);

    now = dbManager.getHeadBlockTimeStamp();
    startTime = now + 48 * 3600 * 1000;
    endTime = now + 72 * 3600 * 1000;
  }

  @After
  public void removeWrapper() {
    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);
    dbManager.getAccountStore().delete(address);
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

  private Any getContract() {
    long nowTime = new Date().getTime();
    return Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setPrecision(6)
            .build());
  }

  @Test
  public void SameTokenNameCloseAssetIssueSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    AssetIssueOperator operator = new AssetIssueOperator(getContract(), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      // check V1
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNotNull(assetIssueWrapper);
      Assert.assertEquals(6, assetIssueWrapper.getPrecision());
      Assert.assertEquals(NUM, assetIssueWrapper.getNum());
      Assert.assertEquals(GSC_NUM, assetIssueWrapper.getGscNum());
      Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(), TOTAL_SUPPLY);
      // check V2
      long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenIdNum)));
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(0, assetIssueWrapperV2.getPrecision());
      Assert.assertEquals(NUM, assetIssueWrapperV2.getNum());
      Assert.assertEquals(GSC_NUM, assetIssueWrapperV2.getGscNum());
      Assert.assertEquals(owner.getAssetMapV2().get(String.valueOf(tokenIdNum)).longValue(),
          TOTAL_SUPPLY);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  public void oldNotUpdateAssetIssueSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    AssetIssueOperator operator = new AssetIssueOperator(getContract(), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      // V1,Data is no longer update
      Assert.assertFalse(
          dbManager.getAssetIssueStore().has(ByteString.copyFromUtf8(NAME).toByteArray()));
      // check V2
      long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenIdNum)));
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(6, assetIssueWrapperV2.getPrecision());
      Assert.assertEquals(NUM, assetIssueWrapperV2.getNum());
      Assert.assertEquals(GSC_NUM, assetIssueWrapperV2.getGscNum());
      Assert.assertEquals(owner.getAssetMapV2().get(String.valueOf(tokenIdNum)).longValue(),
          TOTAL_SUPPLY);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  public void SameTokenNameOpenAssetIssueSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    AssetIssueOperator operator = new AssetIssueOperator(getContract(), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      // V1,Data is no longer update
      Assert.assertFalse(
          dbManager.getAssetIssueStore().has(ByteString.copyFromUtf8(NAME).toByteArray()));
      //V2
      long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();
      byte[] assertKey = ByteArray.fromString(String.valueOf(tokenIdNum));
      AssetIssueWrapper assetIssueWrapperV2 = dbManager.getAssetIssueV2Store().get(assertKey);
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(6, assetIssueWrapperV2.getPrecision());
      Assert.assertEquals(NUM, assetIssueWrapperV2.getNum());
      Assert.assertEquals(GSC_NUM, assetIssueWrapperV2.getGscNum());
      Assert.assertEquals(owner.getAssetMapV2().get(String.valueOf(tokenIdNum)).longValue(),
          TOTAL_SUPPLY);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  /**
   Total supply must greater than zero.Else can't asset issue and balance do not change.
   */
  public void negativeTotalSupplyTest() {
    long nowTime = new Date().getTime();
    Any contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(-TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("TotalSupply must greater than 0!".equals(e.getMessage()));
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  /**
   Total supply must greater than zero.Else can't asset issue and balance do not change.
   */
  public void zeroTotalSupplyTest() {
    long nowTime = new Date().getTime();
    Any contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(0)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("TotalSupply must greater than 0!".equals(e.getMessage()));
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  /*
    gsc num must greater than zero.Else can't asset issue and balance do not change.
   */
  public void negativeGscNumTest() {
    long nowTime = new Date().getTime();
    Any contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(-GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("GscNum must greater than 0!".equals(e.getMessage()));
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  /*
    gsc num must greater than zero.Else can't asset issue and balance do not change.
   */
  public void zeroGscNumTest() {
    long nowTime = new Date().getTime();
    Any contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(0)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("GscNum must greater than 0!".equals(e.getMessage()));
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  /*
    Num must greater than zero.Else can't asset issue and balance do not change.
   */
  public void negativeNumTest() {
    long nowTime = new Date().getTime();
    Any contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(-NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Num must greater than 0!".equals(e.getMessage()));
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  /*
    gsc num must greater than zero.Else can't asset issue and balance do not change.
   */
  public void zeroNumTest() {
    long nowTime = new Date().getTime();
    Any contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(0)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Num must greater than 0!".equals(e.getMessage()));
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  /*
   * Asset name length must between 1 to 32 and can not contain space and other unreadable character, and can not contain chinese characters.
   */
  public void assetNameTest() {
    long nowTime = new Date().getTime();

    //Empty name, throw exception
    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.EMPTY)
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid assetName", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //Too long name, throw exception. Max long is 32.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8("testname0123456789abcdefghijgklmo"))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid assetName", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //Contain space, throw exception. Every character need readable .
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8("t e"))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid assetName", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //Contain chinese character, throw exception.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFrom(ByteArray.fromHexString("E6B58BE8AF95")))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid assetName", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    // 32 byte readable character just ok.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8("testname0123456789abcdefghijgklm"))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get("testname0123456789abcdefghijgklm".getBytes());
      Assert.assertNotNull(assetIssueWrapper);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get("testname0123456789abcdefghijgklm").longValue(),
          TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    createWrapper();
    // 1 byte readable character ok.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8("0"))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get("0".getBytes());
      Assert.assertNotNull(assetIssueWrapper);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get("0").longValue(), TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /*
   * Url length must between 1 to 256.
   */
  @Test
  public void urlTest() {
    long nowTime = new Date().getTime();

    //Empty url, throw exception
    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.EMPTY)
        .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    String url256Bytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    //Too long url, throw exception. Max long is 256.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(url256Bytes + "0"))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    // 256 byte readable character just ok.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(url256Bytes))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(NAME.getBytes());
      Assert.assertNotNull(assetIssueWrapper);
      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    createWrapper();
    // 1 byte url.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8("0"))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(NAME.getBytes());
      Assert.assertNotNull(assetIssueWrapper);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    createWrapper();
    // 1 byte space ok.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
        .setUrl(ByteString.copyFromUtf8(" "))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(NAME.getBytes());
      Assert.assertNotNull(assetIssueWrapper);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /*
   * Description length must less than 200.
   */
  @Test
  public void descriptionTest() {
    long nowTime = new Date().getTime();

    String description200Bytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef01234567";
    //Too long description, throw exception. Max long is 200.
    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(description200Bytes + "0"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid description", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    // 200 bytes character just ok.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(description200Bytes))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(NAME.getBytes());
      Assert.assertNotNull(assetIssueWrapper);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    createWrapper();
    // Empty description is ok.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.EMPTY)
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(NAME.getBytes());
      Assert.assertNotNull(assetIssueWrapper);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    createWrapper();
    // 1 byte space ok.
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setDescription(ByteString.copyFromUtf8(" "))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());

    operator = new AssetIssueOperator(contract, dbManager);
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(NAME.getBytes());
      Assert.assertNotNull(assetIssueWrapper);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /*
   * Test FrozenSupply, 1. frozen_amount must greater than zero.
   */
  @Test
  public void frozenTest() {
    //frozen_amount = 0 throw exception.
    FrozenSupply frozenSupply = FrozenSupply.newBuilder().setFrozenDays(1).setFrozenAmount(0)
        .build();
    long nowTime = new Date().getTime();
    Any contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .addFrozenSupply(frozenSupply)
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Frozen supply must be greater than 0!", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //frozen_amount < 0 throw exception.
    frozenSupply = FrozenSupply.newBuilder().setFrozenDays(1).setFrozenAmount(-1)
        .build();
    contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .addFrozenSupply(frozenSupply)
            .build());

    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Frozen supply must be greater than 0!", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    long minFrozenSupplyTime = dbManager.getDynamicPropertiesStore().getMinFrozenSupplyTime();
    long maxFrozenSupplyTime = dbManager.getDynamicPropertiesStore().getMaxFrozenSupplyTime();

    //FrozenDays = 0 throw exception.
    frozenSupply = FrozenSupply.newBuilder().setFrozenDays(0).setFrozenAmount(1)
        .build();
    nowTime = new Date().getTime();
    contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .addFrozenSupply(frozenSupply)
            .build());

    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "frozenDuration must be less than " + maxFrozenSupplyTime + " days " + "and more than "
              + minFrozenSupplyTime + " days", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //FrozenDays < 0 throw exception.
    frozenSupply = FrozenSupply.newBuilder().setFrozenDays(-1).setFrozenAmount(1)
        .build();
    contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .addFrozenSupply(frozenSupply)
            .build());

    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "frozenDuration must be less than " + maxFrozenSupplyTime + " days " + "and more than "
              + minFrozenSupplyTime + " days", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //FrozenDays >  maxFrozenSupplyTime throw exception.
    frozenSupply = FrozenSupply.newBuilder().setFrozenDays(maxFrozenSupplyTime + 1)
        .setFrozenAmount(1)
        .build();
    contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .addFrozenSupply(frozenSupply)
            .build());

    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "frozenDuration must be less than " + maxFrozenSupplyTime + " days " + "and more than "
              + minFrozenSupplyTime + " days", e.getMessage());
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteArray.fromString(NAME));
      Assert.assertEquals(owner.getBalance(),
          dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance);
      Assert.assertNull(assetIssueWrapper);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //frozen_amount = 1 and  frozenDays = 1 is OK
    frozenSupply = FrozenSupply.newBuilder().setFrozenDays(1).setFrozenAmount(1)
        .build();
    contract = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .addFrozenSupply(frozenSupply)
            .build());

    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      operator.validate();
      operator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  public void issueTimeTest() {
    //empty start time will throw exception
    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Start time should be not empty", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //empty end time will throw exception
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("End time should be not empty", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //startTime == now, throw exception
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(now)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Start time should be greater than HeadBlockTime", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //startTime < now, throw exception
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(now - 1)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Start time should be greater than HeadBlockTime", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //endTime == startTime, throw exception
    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setEndTime(startTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("End time should be greater than start time", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(endTime)
        .setEndTime(startTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("End time should be greater than start time", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      AccountWrapper account = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      Assert.assertEquals(account.getAssetIssuedName().toStringUtf8(), NAME);
      Assert.assertEquals(account.getAssetMap().size(), 1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  public void assetIssueNameTest() {
    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME_SECOND))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    operator = new AssetIssueOperator(contract, dbManager);
    ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("An account can only issue one asset", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(ASSET_NAME_SECOND));
    }
  }

  @Test
  public void assetIssueGSCNameTest() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8("GSC"))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .build());
    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("assetName can't be gsc", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(ASSET_NAME_SECOND));
    }
  }

  @Test
  public void frozenListSizeTest() {
    this.dbManager.getDynamicPropertiesStore().saveMaxFrozenSupplyNumber(3);
    List<FrozenSupply> frozenList = new ArrayList();
    for (int i = 0; i < this.dbManager.getDynamicPropertiesStore().getMaxFrozenSupplyNumber() + 2;
        i++) {
      frozenList.add(FrozenSupply.newBuilder()
          .setFrozenAmount(10)
          .setFrozenDays(3)
          .build());
    }

    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .addAllFrozenSupply(frozenList)
        .build());
    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Frozen supply list length is too long", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  public void frozenSupplyMoreThanTotalSupplyTest() {
    this.dbManager.getDynamicPropertiesStore().saveMaxFrozenSupplyNumber(3);
    List<FrozenSupply> frozenList = new ArrayList();
    frozenList.add(FrozenSupply.newBuilder()
        .setFrozenAmount(TOTAL_SUPPLY + 1)
        .setFrozenDays(3)
        .build());
    Any contract = Any.pack(Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setGscNum(GSC_NUM).setNum(NUM)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setDescription(ByteString.copyFromUtf8("description"))
        .setUrl(ByteString.copyFromUtf8(URL))
        .addAllFrozenSupply(frozenList)
        .build());
    AssetIssueOperator operator = new AssetIssueOperator(contract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Frozen supply cannot exceed total supply", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  public void SameTokenNameCloseInvalidOwnerAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    long nowTime = new Date().getTime();
    Any any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("12312315345345")))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(any, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid ownerAddress", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /**
   * SameTokenName open, check invalid precision
   */
  @Test
  public void SameTokenNameCloseInvalidPrecision() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    long nowTime = new Date().getTime();
    Any any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setPrecision(7)
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(any, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
//    dbManager.getDynamicPropertiesStore()
//        .statsByVersion(Parameter.ForkBlockVersionConsts.CPU_LIMIT, stats);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("precision cannot exceed 6", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /**
   * SameTokenName close, Invalid abbreviation for token
   */
  @Test
  public void SameTokenNameCloseInvalidAddr() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    long nowTime = new Date().getTime();
    Any any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setAbbr(ByteString.copyFrom(ByteArray.fromHexString(
                "a0299f3db80a24123b20a254b89ce639d59132f157f13")))
            .setPrecision(4)
            .build());

    AssetIssueOperator operator = new AssetIssueOperator(any, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
//    dbManager.getDynamicPropertiesStore()
//        .statsByVersion(Parameter.ForkBlockVersionConsts.CPU_LIMIT, stats);

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid abbreviation for token", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /**
   * repeat issue assert name,
   */
  @Test
  public void IssueSameTokenNameAssert() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String ownerAddress = "a08beaa1a8e2d45367af7bae7c49009876a4fa4301";

    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    Contract.AssetIssueContract assetIssueContract =
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setName(ByteString.copyFrom(ByteArray.fromString(NAME)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(1)
            .setEndTime(100)
            .setVoteScore(2)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);

    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)),
            ByteString.copyFromUtf8("owner11"),
            AccountType.AssetIssue);
    accountWrapper.addAsset(NAME.getBytes(), 1000L);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

    AssetIssueOperator operator = new AssetIssueOperator(getContract(), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    // SameTokenName not active, same assert name, should failure
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Token exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // SameTokenName active, same assert name,should success
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenIdNum)));
      Assert.assertNotNull(assetIssueWrapperV2);

      Assert.assertEquals(owner.getBalance(), 0L);
      Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      Assert.assertEquals(owner.getAssetMapV2().get(String.valueOf(tokenIdNum)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /**
   * SameTokenName close, check invalid param "PublicFreeAssetNetUsage must be 0!" "Invalid
   * FreeAssetNetLimit" "Invalid PublicFreeAssetNetLimit" "Account not exists" "No enough balance
   * for fee!"
   */
  @Test
  public void SameTokenNameCloseInvalidparam() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    long nowTime = new Date().getTime();
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    Any any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setPrecision(3)
            .setPublicFreeAssetNetUsage(100)
            .build());
    AssetIssueOperator operator = new AssetIssueOperator(any, dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("PublicFreeAssetNetUsage must be 0!", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //Invalid FreeAssetNetLimit
    any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setPrecision(3)
            .setFreeAssetNetLimit(-10)
            .build());
    operator = new AssetIssueOperator(any, dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid FreeAssetNetLimit", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setPrecision(3)
            .setPublicFreeAssetNetLimit(-10)
            .build());
    operator = new AssetIssueOperator(any, dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid PublicFreeAssetNetLimit", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /**
   * SameTokenName close, account not good "Account not exists" "No enough balance for fee!"
   */
  @Test
  public void SameTokenNameCloseInvalidAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    long nowTime = new Date().getTime();
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
//    dbManager.getDynamicPropertiesStore()
//        .statsByVersion(Parameter.ForkBlockVersionConsts.CPU_LIMIT, stats);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    // No enough balance for fee!
    Any any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setPrecision(3)
            .build());
    AssetIssueOperator operator = new AssetIssueOperator(any, dbManager);
    AccountWrapper owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(1000);
    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No enough balance for fee!", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

    //Account not exists
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
    any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .setPrecision(3)
            .build());
    operator = new AssetIssueOperator(any, dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }

  }

}