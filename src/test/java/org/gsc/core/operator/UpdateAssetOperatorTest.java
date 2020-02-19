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
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;

import java.io.File;
import java.util.Date;

import static junit.framework.TestCase.fail;

@Slf4j
public class UpdateAssetOperatorTest {

  private static GSCApplicationContext context;
  private static Application AppT;
  private static Manager dbManager;
  private static final String dbPath = "db_updateAsset_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
  private static final String SECOND_ACCOUNT_ADDRESS;
  private static final String OWNER_ADDRESS_NOTEXIST;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String NAME = "gsc-my";
  private static final long TOTAL_SUPPLY = 10000L;
  private static final String DESCRIPTION = "myCoin";
  private static final String URL = "gsc-my.com";

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "f6b1ed20174eb4de7202a34bf1975935c1a50432";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "bd2a8999ea35f80f73e9f77fa4f775f0e6e00fbe";
    SECOND_ACCOUNT_ADDRESS =
        Wallet.getAddressPreFixString() + "89b3256f723498acc2fa7d67fd456e93c738f31d";

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

  private Any getContract(
      String accountAddress, String description, String url, long newLimit, long newPublicLimit) {
    return Any.pack(
        Contract.UpdateAssetContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(accountAddress))
            .setDescription(ByteString.copyFromUtf8(description))
            .setUrl(ByteString.copyFromUtf8(url))
            .setNewLimit(newLimit)
            .setNewPublicLimit(newPublicLimit)
            .build());
  }

  private Contract.AssetIssueContract getAssetIssueContract() {
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);

    long nowTime = new Date().getTime();
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setId(String.valueOf(tokenId))
        .setGscNum(100)
        .setNum(10)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setOrder(0)
        .setDescription(ByteString.copyFromUtf8("assetTest"))
        .setUrl(ByteString.copyFromUtf8("gsc.test.com"))
        .build();
  }

  private void createAssertBeforSameTokenNameActive() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);

    // address in accountStore and the owner of contract
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);

    // add asset issue
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(getAssetIssueContract());
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

    accountWrapper.setAssetIssuedName(assetIssueWrapper.createDbKey());
    accountWrapper.setAssetIssuedID(assetIssueWrapper.getId().getBytes());

    accountWrapper.addAsset(assetIssueWrapper.createDbKey(), TOTAL_SUPPLY);
    accountWrapper.addAssetV2(assetIssueWrapper.createDbV2Key(), TOTAL_SUPPLY);

    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountWrapper);
  }

  private void createAssertSameTokenNameActive() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

    // address in accountStore and the owner of contract
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);

    // add asset issue
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(getAssetIssueContract());
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

    accountWrapper.setAssetIssuedName(assetIssueWrapper.createDbKey());
    accountWrapper.setAssetIssuedID(assetIssueWrapper.getId().getBytes());
    accountWrapper.addAssetV2(assetIssueWrapper.createDbV2Key(), TOTAL_SUPPLY);

    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountWrapper);
  }

  @Test
  public void successUpdateAssetBeforeSameTokenNameActive() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    TransactionResultWrapper ret = new TransactionResultWrapper();
    UpdateAssetOperator operator;
    operator =
        new UpdateAssetOperator(
            getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, 8000L), dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      //V1
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNotNull(assetIssueWrapper);
      Assert.assertEquals(
          DESCRIPTION, assetIssueWrapper.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueWrapper.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueWrapper.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueWrapper.getPublicFreeAssetNetLimit(), 8000L);
      //V2
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenId)));
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(
          DESCRIPTION, assetIssueWrapperV2.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueWrapperV2.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueWrapperV2.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueWrapperV2.getPublicFreeAssetNetLimit(), 8000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /**
   * Init close SameTokenName,after init data,open SameTokenName
   */
  @Test
  public void oldNotUpdataSuccessUpdateAsset() {
    createAssertBeforSameTokenNameActive();
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    TransactionResultWrapper ret = new TransactionResultWrapper();
    UpdateAssetOperator operator;
    operator = new UpdateAssetOperator(
        getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, 8000L), dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      //V1 old version exist but  not updata
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNotNull(assetIssueWrapper);
      Assert.assertNotEquals(
          DESCRIPTION, assetIssueWrapper.getInstance().getDescription().toStringUtf8());
      Assert.assertNotEquals(URL, assetIssueWrapper.getInstance().getUrl().toStringUtf8());
      Assert.assertNotEquals(assetIssueWrapper.getFreeAssetNetLimit(), 500L);
      Assert.assertNotEquals(assetIssueWrapper.getPublicFreeAssetNetLimit(), 8000L);
      //V2
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenId)));
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(
          DESCRIPTION, assetIssueWrapperV2.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueWrapperV2.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueWrapperV2.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueWrapperV2.getPublicFreeAssetNetLimit(), 8000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void successUpdateAssetAfterSameTokenNameActive() {
    createAssertSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    TransactionResultWrapper ret = new TransactionResultWrapper();
    UpdateAssetOperator operator;
    operator = new UpdateAssetOperator(getContract(OWNER_ADDRESS, DESCRIPTION, URL,
        500L, 8000L), dbManager);
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      //V1，Data is no longer update
      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNull(assetIssueWrapper);
      //V2
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenId)));
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(
          DESCRIPTION, assetIssueWrapperV2.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueWrapperV2.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueWrapperV2.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueWrapperV2.getPublicFreeAssetNetLimit(), 8000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void invalidAddress() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    UpdateAssetOperator operator =
        new UpdateAssetOperator(
            getContract(OWNER_ADDRESS_INVALID, DESCRIPTION, URL, 500L, 8000L), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Invalid ownerAddress");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid ownerAddress", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void noExistAccount() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    UpdateAssetOperator operator =
        new UpdateAssetOperator(
            getContract(OWNER_ADDRESS_NOTEXIST, DESCRIPTION, URL, 500L, 8000L), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Account has not existed");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account has not existed", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void noAsset() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    UpdateAssetOperator operator =
        new UpdateAssetOperator(
            getContract(SECOND_ACCOUNT_ADDRESS, DESCRIPTION, URL, 500L, 8000L), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Account has not issue any asset");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account has not issue any asset", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /*
   * empty url
   */
  @Test
  public void invalidAssetUrl() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    String localUrl = "";
    UpdateAssetOperator operator =
        new UpdateAssetOperator(
            getContract(OWNER_ADDRESS, DESCRIPTION, localUrl, 500L, 8000L), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /*
   * description is more than 200 character
   */
  @Test
  public void invalidAssetDescription() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    String localDescription =
        "abchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuv"
            + "wxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghij"
            + "klmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyz";

    UpdateAssetOperator operator =
        new UpdateAssetOperator(
            getContract(OWNER_ADDRESS, localDescription, URL, 500L, 8000L), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Invalid description");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid description", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /*
   * new limit is more than 69_120_000_000
   */
  @Test
  public void invalidNewLimit() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long localNewLimit = 69120000000L;
    UpdateAssetOperator operator =
        new UpdateAssetOperator(
            getContract(OWNER_ADDRESS, DESCRIPTION, URL, localNewLimit, 4000L), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Invalid FreeAssetNetLimit");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid FreeAssetNetLimit", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void invalidNewPublicLimit() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long localNewPublicLimit = -1L;
    UpdateAssetOperator operator =
        new UpdateAssetOperator(
            getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, localNewPublicLimit), dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);

      fail("Invalid PublicFreeAssetNetLimit");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid PublicFreeAssetNetLimit", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}
