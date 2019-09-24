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

package org.gsc.core;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.NetProcessor;
import org.gsc.db.Manager;
import org.gsc.db.TransactionTrace;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.TooBigTransactionResultException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.AccountType;

@Slf4j
public class NetProcessorTest {

  private static Manager dbManager;
  private static final String dbPath = "db_net_test";
  private static GSCApplicationContext context;
  private static final String ASSET_NAME;
  private static final String ASSET_NAME_V2;
  private static final String OWNER_ADDRESS;
  private static final String ASSET_ADDRESS;
  private static final String ASSET_ADDRESS_V2;
  private static final String TO_ADDRESS;
  private static final long TOTAL_SUPPLY = 10000000000000L;
  private static final int GSC_NUM = 2;
  private static final int NUM = 2147483647;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "GSC";
  private static final String URL = "https://gsc.network";
  private static long START_TIME;
  private static long END_TIME;


  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    ASSET_NAME = "test_token";
    ASSET_NAME_V2 = "2";
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "f740d0a396a324b15d68aa8ffeaf2057d937d745";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "89b3256f723498acc2fa7d67fd456e93c738f31d";
    ASSET_ADDRESS = Wallet.getAddressPreFixString() + "afe9656f4a2d804aea04afac52627aef6299780e";
    ASSET_ADDRESS_V2 = Wallet.getAddressPreFixString() + "826fcb1d10252c8eb4975035ac2b3d6953e5bb15";
    START_TIME = DateTime.now().minusDays(1).getMillis();
    END_TIME = DateTime.now().getMillis();
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * Release resources.
   */
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

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void createWrapper() {
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(getAssetIssueContract());
    assetIssueWrapper.setId("1");
    dbManager

        .getAssetIssueStore()
        .put(
            ByteArray.fromString(ASSET_NAME),
            assetIssueWrapper);
    dbManager
        .getAssetIssueV2Store()
        .put(
            ByteArray.fromString("1"),
            assetIssueWrapper);

    AssetIssueWrapper assetIssueWrapperV2 = new AssetIssueWrapper(getAssetIssueV2Contract());
    dbManager
        .getAssetIssueV2Store()
        .put(
            ByteArray.fromString(ASSET_NAME_V2),
            assetIssueWrapperV2);

    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            0L);
    ownerWrapper.addAsset(ASSET_NAME.getBytes(), 100L);
    ownerWrapper.addAsset(ASSET_NAME_V2.getBytes(), 100L);

    AccountWrapper toAccountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            0L);

    AccountWrapper assetWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("asset"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)),
            AccountType.AssetIssue,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());

    AccountWrapper assetWrapper2 =
        new AccountWrapper(
            ByteString.copyFromUtf8("asset2"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS_V2)),
            AccountType.AssetIssue,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());

    dbManager.getAccountStore().reset();
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
    dbManager.getAccountStore().put(toAccountWrapper.getAddress().toByteArray(), toAccountWrapper);
    dbManager.getAccountStore().put(assetWrapper.getAddress().toByteArray(), assetWrapper);
    dbManager.getAccountStore().put(assetWrapper2.getAddress().toByteArray(), assetWrapper2);

  }

  private TransferAssetContract getTransferAssetContract() {
    return Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private TransferAssetContract getTransferAssetV2Contract() {
    return Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME_V2)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private AssetIssueContract getAssetIssueContract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME))
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

  private AssetIssueContract getAssetIssueV2Contract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS_V2)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME_V2))
        .setId(ASSET_NAME_V2)
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp, String assetName) {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setGscNum(GSC_NUM)
            .setNum(NUM)
            .setStartTime(startTimestmp)
            .setEndTime(endTimestmp)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
      dbManager.getAssetIssueV2Store()
          .put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);
      accountWrapper.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    } else {
      dbManager.getAssetIssueStore()
          .put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
      accountWrapper.addAsset(assetName.getBytes(), TOTAL_SUPPLY);
    }
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);
  }


  //@Test
  public void testCreateNewAccount() throws Exception {
    NetProcessor processor = new NetProcessor(dbManager);
    TransferAssetContract transferAssetContract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(transferAssetContract);

    String NOT_EXISTS_ADDRESS =
        Wallet.getAddressPreFixString() + "008794500882809695a8a687866e76d4271a1abc";
    transferAssetContract = transferAssetContract.toBuilder()
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(NOT_EXISTS_ADDRESS))).build();

    org.gsc.protos.Protocol.Transaction.Contract contract = org.gsc.protos.Protocol.Transaction.Contract
        .newBuilder()
        .setType(Protocol.Transaction.Contract.ContractType.TransferAssetContract).setParameter(
            Any.pack(transferAssetContract)).build();

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(10_000_000L, 0L);

    Assert.assertEquals(true, processor.contractCreateNewAccount(contract));
    long bytes = trx.getSerializedSize();
    processor.consumeNetForCreateNewAccount(accountWrapper, bytes, 1526647838000L);

    AccountWrapper ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    Assert.assertEquals(122L, ownerWrapperNew.getNetUsage());

  }


  @Test
  public void testFree() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566867038000L);
    Contract.TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper ownerWrapper = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
     dbManager.consumeNet(trx, trace);

     AccountWrapper ownerWrapperNew = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
     Assert.assertEquals(126L + (dbManager.getDynamicPropertiesStore().supportVM()
             ? Constant.MAX_RESULT_SIZE_IN_TX: 0), ownerWrapperNew.getFreeNetUsage());
    Assert.assertEquals(317812L, ownerWrapperNew.getLatestConsumeFreeTime());//slot
    Assert.assertEquals(1566867038000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(
        126L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0),
        dbManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(317812L, dbManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566910238000L); // + 12h

    dbManager.consumeNet(trx, trace);
    ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(63L + 126 + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerWrapperNew.getFreeNetUsage());
    Assert.assertEquals(332212L,
        ownerWrapperNew.getLatestConsumeFreeTime()); // 508882612L + 28800L/2
    Assert.assertEquals(1566910238000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(63L + 126L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        dbManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(332212L, dbManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());
  }


  @Test
  public void testConsumeAssetAccount() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566867038000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only assetAccount has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));
    accountWrapper.setFrozen(10_000_000L, 0L);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeNet(trx, trace);

    AccountWrapper ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountWrapper assetWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(317812L, assetWrapperNew.getLatestConsumeTime());
    Assert.assertEquals(1566867038000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(317812L, ownerWrapperNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(
        126L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0),
        ownerWrapperNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(
        126L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0),
        ownerWrapperNew.getFreeAssetNetUsageV2("1"));
    Assert.assertEquals(
        126L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0),
        assetWrapperNew.getNetUsage());

    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566910238000L); // + 12h

    dbManager.consumeNet(trx, trace);

    ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    assetWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(332212L, assetWrapperNew.getLatestConsumeTime());
    Assert.assertEquals(1566910238000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(332212L, ownerWrapperNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(63L + 126L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerWrapperNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(63L + 126L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerWrapperNew.getFreeAssetNetUsageV2("1"));
    Assert.assertEquals(63L + 126L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        assetWrapperNew.getNetUsage());
    Assert.assertEquals(0L, ret.getFee());

  }

  @Test
  public void testConsumeAssetAccountV2() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566867038000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only assetAccount has frozen balance

    TransferAssetContract contract = getTransferAssetV2Contract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    // issuer freeze balance for net
    AccountWrapper issuerWrapperV2 = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));
    issuerWrapperV2.setFrozen(10_000_000L, 0L);
    dbManager.getAccountStore().put(issuerWrapperV2.getAddress().toByteArray(), issuerWrapperV2);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeNet(trx, trace);

    AccountWrapper ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountWrapper issuerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));

    Assert.assertEquals(317812L, issuerWrapperNew.getLatestConsumeTime());
    Assert.assertEquals(1566867038000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(317812L,
        ownerWrapperNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(
        117L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        issuerWrapperNew.getNetUsage());
    Assert.assertEquals(
        117L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerWrapperNew.getFreeAssetNetUsageV2(ASSET_NAME_V2));

    Assert.assertEquals(317812L, ownerWrapperNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566910238000L); // + 12h

    dbManager.consumeNet(trx, trace);

    ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    issuerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));

    Assert.assertEquals(332212L, issuerWrapperNew.getLatestConsumeTime());
    Assert.assertEquals(1566910238000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(332212L,
        ownerWrapperNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(58L + 117L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerWrapperNew.getFreeAssetNetUsageV2(ASSET_NAME_V2));
    Assert.assertEquals(58L + 117L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        issuerWrapperNew.getNetUsage());
    Assert.assertEquals(0L, ret.getFee());

  }

  @Test
  public void testConsumeOwner() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566867038000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(10_000_000L, 0L);

    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeNet(trx, trace);

    AccountWrapper ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    AccountWrapper assetWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(
        126 + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0),
        ownerWrapperNew.getNetUsage());
    Assert.assertEquals(1566867038000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(317812L, ownerWrapperNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566910238000L); // + 12h

    dbManager.consumeNet(trx, trace);

    ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(63L + 126L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerWrapperNew.getNetUsage());
    Assert.assertEquals(1566910238000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(332212L, ownerWrapperNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());

  }


  @Test
  public void testUsingFee() throws Exception {

    Args.getInstance().getGenesisBlock().getAssets().forEach(account -> {
      AccountWrapper wrapper =
          new AccountWrapper(
              ByteString.copyFromUtf8(""),
              ByteString.copyFrom(account.getAddress()),
              AccountType.AssetIssue,
              100L);
      dbManager.getAccountStore().put(account.getAddress(), wrapper);
    });

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1566867038000L);
    dbManager.getDynamicPropertiesStore().saveFreeNetLimit(0L);

    TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper ownerWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerWrapper.setBalance(10_000_000L);

    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeNet(trx, trace);

    AccountWrapper ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long transactionFee =
        (126L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0)) * dbManager
            .getDynamicPropertiesStore().getTransactionFee();
    Assert.assertEquals(transactionFee,
        dbManager.getDynamicPropertiesStore().getTotalTransactionCost());
    Assert.assertEquals(
        10_000_000L - transactionFee,
        ownerWrapperNew.getBalance());
    Assert.assertEquals(transactionFee, trace.getReceipt().getNetFee());

    dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
    dbManager.consumeNet(trx, trace);

  }

  /**
   * sameTokenName close, consume success assetIssueWrapper.getOwnerAddress() !=
   * fromAccount.getAddress()) contract.getType() = TransferAssetContract
   */
  @Test
  public void sameTokenNameCloseConsumeSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
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
            .setPublicFreeAssetNetLimit(2000)
            .setFreeAssetNetLimit(2000)
            .build();
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    // V1
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
    // V2
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerWrapper.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerWrapper.setFrozenForNet(2_000_000L, expireTime);
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);

    AccountWrapper toAddressWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressWrapper.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressWrapper.setFrozenForNet(2_000_000L, expireTime2);
    dbManager.getAccountStore().put(toAddressWrapper.getAddress().toByteArray(), toAddressWrapper);

    TransferAssetContract contract = Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();

    TransactionWrapper trx = new TransactionWrapper(contract);
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize() +
        Constant.MAX_RESULT_SIZE_IN_TX;

    NetProcessor processor = new NetProcessor(dbManager);

    try {
      processor.consume(trx, trace);
      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      //V1
      AssetIssueWrapper assetIssueWrapperV1 =
          dbManager.getAssetIssueStore().get(assetIssueWrapper.createDbKey());
      Assert.assertNotNull(assetIssueWrapperV1);
      Assert.assertEquals(assetIssueWrapperV1.getPublicFreeAssetNetUsage(), byteSize);

      AccountWrapper fromAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsage(ASSET_NAME), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);

      AccountWrapper ownerAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertNotNull(ownerAccount);
      Assert.assertEquals(ownerAccount.getNetUsage(), byteSize);

      //V2
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(assetIssueWrapper.createDbV2Key());
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(assetIssueWrapperV2.getPublicFreeAssetNetUsage(), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsage(ASSET_NAME), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
      dbManager.getAssetIssueStore().delete(assetIssueWrapper.createDbKey());
      dbManager.getAssetIssueV2Store().delete(assetIssueWrapper.createDbV2Key());
    }
  }

  /**
   * sameTokenName open, consume success assetIssueWrapper.getOwnerAddress() !=
   * fromAccount.getAddress()) contract.getType() = TransferAssetContract
   */
  @Test
  public void sameTokenNameOpenConsumeSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
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
            .setPublicFreeAssetNetLimit(2000)
            .setFreeAssetNetLimit(2000)
            .build();
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    // V2
    dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerWrapper.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerWrapper.setFrozenForNet(2_000_000L, expireTime);
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);

    AccountWrapper toAddressWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressWrapper.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressWrapper.setFrozenForNet(2_000_000L, expireTime2);
    dbManager.getAccountStore().put(toAddressWrapper.getAddress().toByteArray(), toAddressWrapper);

    TransferAssetContract contract = Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(String.valueOf(id))))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();

    TransactionWrapper trx = new TransactionWrapper(contract);
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize() +
        Constant.MAX_RESULT_SIZE_IN_TX;

    NetProcessor processor = new NetProcessor(dbManager);

    try {
      processor.consume(trx, trace);
      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      AccountWrapper ownerAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertNotNull(ownerAccount);
      Assert.assertEquals(ownerAccount.getNetUsage(), byteSize);

      //V2
      AssetIssueWrapper assetIssueWrapperV2 =
          dbManager.getAssetIssueV2Store().get(assetIssueWrapper.createDbV2Key());
      Assert.assertNotNull(assetIssueWrapperV2);
      Assert.assertEquals(assetIssueWrapperV2.getPublicFreeAssetNetUsage(), byteSize);

      AccountWrapper fromAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
      dbManager.getAssetIssueStore().delete(assetIssueWrapper.createDbKey());
      dbManager.getAssetIssueV2Store().delete(assetIssueWrapper.createDbV2Key());
    }
  }

  /**
   * sameTokenName close, consume success contract.getType() = TransferContract toAddressAccount
   * isn't exist.
   */
  @Test
  public void sameTokenNameCloseTransferToAccountNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerWrapper.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerWrapper.setFrozenForNet(2_000_000L, expireTime);
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);

    AccountWrapper toAddressWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressWrapper.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressWrapper.setFrozenForNet(2_000_000L, expireTime2);
    dbManager.getAccountStore().delete(toAddressWrapper.getAddress().toByteArray());

    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();

    TransactionWrapper trx = new TransactionWrapper(contract, dbManager.getAccountStore());
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize() +
        Constant.MAX_RESULT_SIZE_IN_TX;

    NetProcessor processor = new NetProcessor(dbManager);

    try {
      processor.consume(trx, trace);

      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      AccountWrapper fromAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getNetUsage(), byteSize);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
    }
  }
}
