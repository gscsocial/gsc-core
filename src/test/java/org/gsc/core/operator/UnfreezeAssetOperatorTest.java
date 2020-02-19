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
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UnfreezeAssetOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "db_unfreeze_asset_test";
  private static GSCApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long frozenBalance = 1_000_000_000L;
  private static final String assetName = "testCoin";
  private static final String assetID = "123456";

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
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
  public void createAccountWrapper() {
//    AccountWrapper ownerWrapper =
//        new AccountWrapper(
//            ByteString.copyFromUtf8("owner"),
//            StringUtil.hexString2ByteString(OWNER_ADDRESS),
//            AccountType.Normal,
//            initBalance);
//    ownerWrapper.setAssetIssuedName(assetName.getBytes());
//    dbManager.getAccountStore().put(ownerWrapper.createDbKey(), ownerWrapper);
  }

  @Before
  public void createAsset() {
//    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
//    builder.setName(ByteString.copyFromUtf8(assetName));
//    builder.setId(assetID);
//    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(builder.build());
//    dbManager.getAssetIssueStore().put(assetName.getBytes(),assetIssueWrapper);
  }

  private Any getContract(String ownerAddress) {
    return Any.pack(
        Contract.UnfreezeAssetContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .build());
  }


  private void createAssertBeforSameTokenNameActive() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);

    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    builder.setName(ByteString.copyFromUtf8(assetName));
    builder.setId(String.valueOf(tokenId));
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(builder.build());
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            initBalance);
    ownerWrapper.setAssetIssuedName(assetName.getBytes());
    ownerWrapper.setAssetIssuedID(assetIssueWrapper.createDbV2Key());
    dbManager.getAccountStore().put(ownerWrapper.createDbKey(), ownerWrapper);
  }

  private void createAssertSameTokenNameActive() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    builder.setName(ByteString.copyFromUtf8(assetName));
    builder.setId(String.valueOf(tokenId));
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(builder.build());
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            initBalance);
    ownerWrapper.setAssetIssuedName(assetName.getBytes());
    ownerWrapper.setAssetIssuedID(assetIssueWrapper.createDbV2Key());
    dbManager.getAccountStore().put(ownerWrapper.createDbKey(), ownerWrapper);
  }

  /**
   * SameTokenName close, Unfreeze assert success.
   */
  @Test
  public void SameTokenNameCloseUnfreezeAsset() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen0 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    Frozen newFrozen1 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance + 1)
        .setExpireTime(now + 600000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen0).addFrozenSupply(newFrozen1).build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      //V1
      Assert.assertEquals(owner.getAssetMap().get(assetName).longValue(), frozenBalance);
      //V2
      Assert.assertEquals(owner.getAssetMapV2().get(String.valueOf(tokenId)).longValue(),
          frozenBalance);
      Assert.assertEquals(owner.getFrozenSupplyCount(), 1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName active, Unfreeze assert success.
   */
  @Test
  public void SameTokenNameActiveUnfreezeAsset() {
    createAssertSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen0 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    Frozen newFrozen1 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance + 1)
        .setExpireTime(now + 600000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen0).addFrozenSupply(newFrozen1).build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      //V1 assert not exist
      Assert.assertNull(owner.getAssetMap().get(assetName));
      //V2
      Assert.assertEquals(owner.getAssetMapV2().get(String.valueOf(tokenId)).longValue(),
          frozenBalance);
      Assert.assertEquals(owner.getFrozenSupplyCount(), 1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * when init data, SameTokenName is close, then open SameTokenName, Unfreeze assert success.
   */
  @Test
  public void SameTokenNameActiveInitAndAcitveUnfreezeAsset() {
    createAssertBeforSameTokenNameActive();
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen0 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    Frozen newFrozen1 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance + 1)
        .setExpireTime(now + 600000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen0).addFrozenSupply(newFrozen1).build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      //V1 assert not exist
      Assert.assertNull(owner.getAssetMap().get(assetName));
      //V2
      Assert.assertEquals(owner.getAssetMapV2().get(String.valueOf(tokenId)).longValue(),
          frozenBalance);
      Assert.assertEquals(owner.getFrozenSupplyCount(), 1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    createAssertBeforSameTokenNameActive();
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS_INVALID),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAccount() {
    createAssertBeforSameTokenNameActive();
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ACCOUNT_INVALID),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notIssueAsset() {
    createAssertBeforSameTokenNameActive();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen).setAssetIssuedName(ByteString.EMPTY)
        .build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("this account did not issue any asset", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noFrozenSupply() {
    createAssertBeforSameTokenNameActive();
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozen supply balance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notTimeToUnfreeze() {
    createAssertBeforSameTokenNameActive();
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now + 60000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen).build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator operator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("It's not time to unfreeze asset supply", e.getMessage());
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