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

package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Key;
import org.gsc.protos.Protocol.Permission;
import org.gsc.protos.Protocol.Vote;

@Ignore
public class AccountWrapperTest {

  private static final String dbPath = "db_accountWrapper_test";
  private static final Manager dbManager;
  private static final GSCApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String ASSET_NAME = "gsc";
  private static final long TOTAL_SUPPLY = 10000L;
  private static final int GSC_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "gsc";
  private static final String URL = "https://gsc.network";


  static AccountWrapper accountWrapperTest;
  static AccountWrapper accountWrapper;

  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);

    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "a06a17a49648a8ad32055c06f60fa14ae46df91234";
  }


  @BeforeClass
  public static void init() {
    ByteString accountName = ByteString.copyFrom(AccountWrapperTest.randomBytes(16));
    ByteString address = ByteString.copyFrom(AccountWrapperTest.randomBytes(32));
    AccountType accountType = AccountType.forNumber(1);
    accountWrapperTest = new AccountWrapper(accountName, address, accountType);
    byte[] accountByte = accountWrapperTest.getData();
    accountWrapper = new AccountWrapper(accountByte);
    accountWrapperTest.setBalance(1111L);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void getDataTest() {
    //test AccountWrapper onstructed function
    Assert.assertEquals(accountWrapper.getInstance().getAccountName(),
        accountWrapperTest.getInstance().getAccountName());
    Assert.assertEquals(accountWrapper.getInstance().getType(),
        accountWrapperTest.getInstance().getType());
    Assert.assertEquals(1111, accountWrapperTest.getBalance());
  }

  @Test
  public void addVotesTest() {
    //test addVote and getVotesList function
    ByteString voteAddress = ByteString.copyFrom(AccountWrapperTest.randomBytes(32));
    long voteAdd = 10L;
    accountWrapperTest.addVotes(voteAddress, voteAdd);
    List<Vote> votesList = accountWrapperTest.getVotesList();
    for (Vote vote :
        votesList) {
      Assert.assertEquals(voteAddress, vote.getVoteAddress());
      Assert.assertEquals(voteAdd, vote.getVoteCount());
    }
  }

  @Test
  public void AssetAmountTest() {
    //test AssetAmount ,addAsset and reduceAssetAmount function

    String nameAdd = "TokenX";
    long amountAdd = 222L;
    boolean addBoolean = accountWrapperTest
        .addAssetAmount(nameAdd.getBytes(), amountAdd);

    Assert.assertTrue(addBoolean);

    Map<String, Long> assetMap = accountWrapperTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMap.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd, entry.getValue().longValue());
    }
    long amountReduce = 22L;

    boolean reduceBoolean = accountWrapperTest
        .reduceAssetAmount(ByteArray.fromString("TokenX"), amountReduce);
    Assert.assertTrue(reduceBoolean);

    Map<String, Long> assetMapAfter = accountWrapperTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMapAfter.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd - amountReduce, entry.getValue().longValue());
    }
    String key = nameAdd;
    long value = 11L;
    boolean addAsssetBoolean = accountWrapperTest.addAsset(key.getBytes(), value);
    Assert.assertFalse(addAsssetBoolean);

    String keyName = "TokenTest";
    long amountValue = 33L;
    boolean addAsssetTrue = accountWrapperTest.addAsset(keyName.getBytes(), amountValue);
    Assert.assertTrue(addAsssetTrue);
  }


  public static byte[] randomBytes(int length) {
    //generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    return result;
  }

  /**
   * SameTokenName close, test assert amountV2 function
   */
  @Test
  public void sameTokenNameCloseAssertAmountV2test() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    Contract.AssetIssueContract assetIssueContract =
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);

    Contract.AssetIssueContract assetIssueContract2 =
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString("abc")))
            .setId(Long.toString(id + 1))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueWrapper assetIssueWrapper2 = new AssetIssueWrapper(assetIssueContract2);
    dbManager.getAssetIssueStore().put(assetIssueWrapper2.createDbKey(), assetIssueWrapper2);

    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            10000);
    accountWrapper.addAsset(ByteArray.fromString(ASSET_NAME), 1000L);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

    accountWrapper.addAssetV2(ByteArray.fromString(String.valueOf(id)), 1000L);
    Assert.assertEquals(accountWrapper.getAssetMap().get(ASSET_NAME).longValue(), 1000L);
    Assert.assertEquals(accountWrapper.getAssetMapV2().get(String.valueOf(id)).longValue(),
        1000L);

    //assetBalanceEnoughV2
    Assert.assertTrue(accountWrapper.assetBalanceEnoughV2(ByteArray.fromString(ASSET_NAME),
        999, dbManager));
    Assert.assertFalse(accountWrapper.assetBalanceEnoughV2(ByteArray.fromString(ASSET_NAME),
        1001, dbManager));

    //reduceAssetAmountV2
    Assert.assertTrue(accountWrapper.reduceAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        999, dbManager));
    Assert.assertFalse(accountWrapper.reduceAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        0, dbManager));
    Assert.assertFalse(accountWrapper.reduceAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        1001, dbManager));
    Assert.assertFalse(accountWrapper.reduceAssetAmountV2(ByteArray.fromString("abc"),
        1001, dbManager));

    //addAssetAmountV2
    Assert.assertTrue(accountWrapper.addAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        500, dbManager));
    // 1000-999 +500
    Assert.assertEquals(accountWrapper.getAssetMap().get(ASSET_NAME).longValue(), 501L);
    Assert.assertTrue(accountWrapper.addAssetAmountV2(ByteArray.fromString("abc"),
        500, dbManager));
    Assert.assertEquals(accountWrapper.getAssetMap().get("abc").longValue(), 500L);
  }

  /**
   * SameTokenName open, test assert amountV2 function
   */
  @Test
  public void sameTokenNameOpenAssertAmountV2test() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    Contract.AssetIssueContract assetIssueContract =
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

    Contract.AssetIssueContract assetIssueContract2 =
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString("abc")))
            .setId(Long.toString(id + 1))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueWrapper assetIssueWrapper2 = new AssetIssueWrapper(assetIssueContract2);
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper2.createDbV2Key(), assetIssueWrapper2);

    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            10000);
    accountWrapper.addAssetV2(ByteArray.fromString(String.valueOf(id)), 1000L);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);
    Assert.assertEquals(accountWrapper.getAssetMapV2().get(String.valueOf(id)).longValue(),
        1000L);

    //assetBalanceEnoughV2
    Assert.assertTrue(accountWrapper.assetBalanceEnoughV2(ByteArray.fromString(String.valueOf(id)),
        999, dbManager));

    Assert.assertFalse(accountWrapper.assetBalanceEnoughV2(ByteArray.fromString(String.valueOf(id)),
        1001, dbManager));

    //reduceAssetAmountV2
    Assert.assertTrue(accountWrapper.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        999, dbManager));
    Assert.assertFalse(accountWrapper.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        0, dbManager));
    Assert.assertFalse(accountWrapper.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        1001, dbManager));
    // abc
    Assert.assertFalse(
        accountWrapper.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id + 1)),
            1001, dbManager));

    //addAssetAmountV2
    Assert.assertTrue(accountWrapper.addAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        500, dbManager));
    // 1000-999 +500
    Assert.assertEquals(accountWrapper.getAssetMapV2().get(String.valueOf(id)).longValue(), 501L);
    //abc
    Assert.assertTrue(accountWrapper.addAssetAmountV2(ByteArray.fromString(String.valueOf(id + 1)),
        500, dbManager));
    Assert
        .assertEquals(accountWrapper.getAssetMapV2().get(String.valueOf(id + 1)).longValue(), 500L);
  }

  @Test
  public void witnessPermissionTest() {
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            10000);

    Assert.assertTrue(
        Arrays.equals(ByteArray.fromHexString(OWNER_ADDRESS),
            accountWrapper.getWitnessPermissionAddress()));

    String witnessPermissionAddress =
        Wallet.getAddressPreFixString() + "cc6a17a49648a8ad32055c06f60fa14ae46df912cc";
    accountWrapper = new AccountWrapper(accountWrapper.getInstance().toBuilder().
        setWitnessPermission(Permission.newBuilder().addKeys(
            Key.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString(witnessPermissionAddress)))
                .build()).
            build()).build());

    Assert.assertTrue(
        Arrays.equals(ByteArray.fromHexString(witnessPermissionAddress),
            accountWrapper.getWitnessPermissionAddress()));
  }
}