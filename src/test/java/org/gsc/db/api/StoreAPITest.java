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

package org.gsc.db.api;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;

import org.gsc.core.wrapper.*;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.NonUniqueObjectException;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.BlockHeader;
import org.gsc.protos.Protocol.BlockHeader.raw;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Witness;


public class StoreAPITest {

  public static final String ACCOUNT_ADDRESS_ONE = "121212a9cf";
  public static final String ACCOUNT_ADDRESS_TWO = "232323a9cf";
  public static final String ACCOUNT_ADDRESS_THREE = "343434a9cf";
  public static final String ACCOUNT_ADDRESS_FOUR = "454545a9cf";
  public static final String ACCOUNT_NAME_ONE = "account12";
  public static final String ACCOUNT_NAME_TWO = "account23";
  public static final long BLOCK_NUM_ONE = 10;
  public static final long BLOCK_NUM_TWO = 11;
  public static final long BLOCK_NUM_THREE = 12;
  public static final long BLOCK_TIMESTAMP_ONE = DateTime.now().minusDays(2).getMillis();
  public static final long BLOCK_TIMESTAMP_TWO = DateTime.now().minusDays(1).getMillis();
  public static final long BLOCK_TIMESTAMP_THREE = DateTime.now().getMillis();
  public static final long BLOCK_WITNESS_ONE = 12;
  public static final long BLOCK_WITNESS_TWO = 13;
  public static final long BLOCK_WITNESS_THREE = 14;
  public static final long TRANSACTION_TIMESTAMP_ONE = DateTime.now().minusDays(2).getMillis();
  public static final long TRANSACTION_TIMESTAMP_TWO = DateTime.now().minusDays(1).getMillis();
  public static final long TRANSACTION_TIMESTAMP_THREE = DateTime.now().getMillis();
  public static final String WITNESS_PUB_K_ONE = "989898a9cf";
  public static final String WITNESS_PUB_K_TWO = "878787a9cf";
  public static final String WITNESS_PUB_K_THREE = "767676a9cf";
  public static final String WITNESS_PUB_K_FOUR = "656565a9cf";
  public static final String WITNESS_URL_ONE = "www.gsc.cn";
  public static final String WITNESS_URL_TWO = "www.gsc-super.cn";
  public static final String WITNESS_URL_THREE = "www.gsc-plus.cn";
  public static final String WITNESS_URL_FOUR = "www.gsc-hk.cn";
  public static final long WITNESS_COUNT_ONE = 100;
  public static final long WITNESS_COUNT_TWO = 200;
  public static final long WITNESS_COUNT_THREE = 300;
  public static final long WITNESS_COUNT_FOUR = 400;
  public static final String ASSETISSUE_NAME_ONE = "www.gsc.cn";
  public static final String ASSETISSUE_NAME_TWO = "www.gsc-super.cn";
  public static final String ASSETISSUE_NAME_THREE = "www.gsc-plus.cn";
  public static final String ASSETISSUE_NAME_FOUR = "www.gsc-hk.cn";
  public static final long ASSETISSUE_START_ONE = DateTime.now().minusDays(2).getMillis();
  public static final long ASSETISSUE_END_ONE = DateTime.now().minusDays(1).getMillis();
  public static final long ASSETISSUE_START_TWO = DateTime.now().minusDays(1).getMillis();
  public static final long ASSETISSUE_END_TWO = DateTime.now().plusDays(1).getMillis();
  public static final long ASSETISSUE_START_THREE = DateTime.now().getMillis();
  public static final long ASSETISSUE_END_THREE = DateTime.now().plusDays(1).getMillis();
  public static final long ASSETISSUE_START_FOUR = DateTime.now().plusDays(1).getMillis();
  public static final long ASSETISSUE_END_FOUR = DateTime.now().plusDays(2).getMillis();
  public static Account account1;
  public static Account account2;
  public static Block block1;
  public static Block block2;
  public static Block block3;
  public static Witness witness1;
  public static Witness witness2;
  public static Witness witness3;
  public static Witness witness4;
  public static Transaction transaction1;
  public static Transaction transaction2;
  public static Transaction transaction3;
  public static Transaction transaction4;
  public static Transaction transaction5;
  public static Transaction transaction6;
  public static AssetIssueContract assetIssue1;
  public static AssetIssueContract assetIssue2;
  public static AssetIssueContract assetIssue3;
  public static AssetIssueContract assetIssue4;
  private static Manager dbManager;
  private static StoreAPI storeAPI;
  private static GSCApplicationContext context;
  private static String dbPath = "db_StoreAPI_test";
  private static Application AppT;


  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, "config-test-index.conf");
    Args.getInstance().setConfirmedNode(true);
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
  }

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    initAccount();
    initTransaction();
    initWitness();
    initAssetIssue();
    initBlock();
    storeAPI = context.getBean(StoreAPI.class);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  /**
   * initAssetIssue.
   */
  private static void initAssetIssue() {
    assetIssue1 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_ONE, ACCOUNT_ADDRESS_ONE, ASSETISSUE_START_ONE, ASSETISSUE_END_ONE);
    addAssetIssueToStore(assetIssue1);
    assetIssue2 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_TWO, ACCOUNT_ADDRESS_TWO, ASSETISSUE_START_TWO, ASSETISSUE_END_TWO);
    addAssetIssueToStore(assetIssue2);
    assetIssue3 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_THREE,
            ACCOUNT_ADDRESS_THREE,
            ASSETISSUE_START_THREE,
            ASSETISSUE_END_THREE);
    addAssetIssueToStore(assetIssue3);
    assetIssue4 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_FOUR, ACCOUNT_ADDRESS_ONE, ASSETISSUE_START_FOUR, ASSETISSUE_END_FOUR);
    addAssetIssueToStore(assetIssue4);
    dbManager.getAssetIssueStore().delete(ASSETISSUE_NAME_FOUR.getBytes());
    dbManager.getAssetIssueStore().delete(ASSETISSUE_NAME_THREE.getBytes());


  }

  private static void addAssetIssueToStore(AssetIssueContract assetIssueContract) {
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    dbManager
        .getAssetIssueStore()
        .put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
  }

  private static void addAssetIssueToStoreV2(AssetIssueContract assetIssueContract, byte[] id) {
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
    dbManager
        .getAssetIssueV2Store()
        .put(id, assetIssueWrapper);
  }

  private static AssetIssueContract getBuildAssetIssueContract(
      String name, String address, long start, long end) {
    return AssetIssueContract.newBuilder()
        .setName(ByteString.copyFrom(name.getBytes()))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setStartTime(start)
        .setEndTime(end)
        .build();
  }

  private static void addTransactionToStore(Transaction transaction) {
    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
    dbManager
            .getTransactionStore()
            .put(transactionWrapper.getTransactionId().getBytes(), transactionWrapper);
  }

  /**
   * initTransaction.
   */
  private static void initTransaction() {
    transaction1 =
        getBuildTransaction(
            getBuildTransferContract(ACCOUNT_ADDRESS_ONE, ACCOUNT_ADDRESS_TWO),
            TRANSACTION_TIMESTAMP_ONE);
    addTransactionToStore(transaction1);
    transaction2 =
        getBuildTransaction(
            getBuildTransferContract(ACCOUNT_ADDRESS_TWO, ACCOUNT_ADDRESS_THREE),
            TRANSACTION_TIMESTAMP_TWO);
    addTransactionToStore(transaction2);
  }

  private static Transaction getBuildTransaction(
      TransferContract transferContract, long transactionTimestamp) {
    return Transaction.newBuilder()
        .setRawData(
            Transaction.raw
                .newBuilder()
                .setTimestamp(transactionTimestamp)
                .addContract(
                    Contract.newBuilder()
                        .setType(ContractType.TransferContract)
                        .setParameter(Any.pack(transferContract))
                        .build())
                .build())
        .build();
  }

  private static TransferContract getBuildTransferContract(String ownerAddress, String toAddress) {
    return TransferContract.newBuilder()
        .setAmount(10)
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress)))
        .build();
  }

  /**
   * initWitness.
   */
  private static void initWitness() {
    witness1 =
        getBuildWitness(
            true, ACCOUNT_ADDRESS_ONE, WITNESS_PUB_K_ONE, WITNESS_URL_ONE, WITNESS_COUNT_ONE);
    addWitnessToStore(witness1);
    witness2 =
        getBuildWitness(
            true, ACCOUNT_ADDRESS_TWO, WITNESS_PUB_K_TWO, WITNESS_URL_TWO, WITNESS_COUNT_ONE);
    addWitnessToStore(witness2);
    witness3 =
        getBuildWitness(
            true, ACCOUNT_ADDRESS_THREE, WITNESS_PUB_K_THREE, WITNESS_URL_THREE, WITNESS_COUNT_ONE);
    addWitnessToStore(witness3);
    witness4 =
        getBuildWitness(
            false, ACCOUNT_ADDRESS_FOUR, WITNESS_PUB_K_FOUR, WITNESS_URL_FOUR, WITNESS_COUNT_ONE);
    addWitnessToStore(witness4);
  }

  private static void addWitnessToStore(Witness transaction) {
    WitnessWrapper witnessWrapper = new WitnessWrapper(transaction);
    dbManager.getWitnessStore().put(witnessWrapper.getAddress().toByteArray(), witnessWrapper);
  }

  private static Witness getBuildWitness(
      boolean job, String address, String pubKey, String url, long count) {
    return Witness.newBuilder()
        .setIsJobs(job)
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setPubKey(ByteString.copyFrom(ByteArray.fromHexString(pubKey)))
        .setUrl(url)
        .setVoteCount(count)
        .build();
  }

  /**
   * initBlock.
   */
  private static void initBlock() {
    block1 =
        getBuildBlock(
            BLOCK_TIMESTAMP_ONE,
            BLOCK_NUM_ONE,
            BLOCK_WITNESS_ONE,
            ACCOUNT_ADDRESS_ONE,
            transaction1,
            transaction2);
    addBlockToStore(block1);
  }

  private static void addBlockToStore(Block block) {
    BlockWrapper blockWrapper = new BlockWrapper(block);
    dbManager.getBlockStore().put(blockWrapper.getBlockId().getBytes(), blockWrapper);
  }

  private static Block getBuildBlock(
      long timestamp,
      long num,
      long witnessId,
      String witnessAddress,
      Transaction transaction,
      Transaction transactionNext) {
    return Block.newBuilder()
        .setBlockHeader(
            BlockHeader.newBuilder()
                .setRawData(
                    raw.newBuilder()
                        .setTimestamp(timestamp)
                        .setNumber(num)
                        .setWitnessId(witnessId)
                        .setWitnessAddress(
                            ByteString.copyFrom(ByteArray.fromHexString(witnessAddress)))
                        .build())
                .build())
        .addTransactions(transaction)
        .addTransactions(transactionNext)
        .build();
  }

  /**
   * initAccount.
   */
  private static void initAccount() {
    account1 = getBuildAccount(ACCOUNT_ADDRESS_ONE, ACCOUNT_NAME_ONE);
    addAccountToStore(account1);
    account2 = getBuildAccount(ACCOUNT_ADDRESS_TWO, ACCOUNT_NAME_TWO);
    addAccountToStore(account2);
  }

  private static Account getBuildAccount(String address, String name) {
    return Account.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setAccountName(ByteString.copyFrom(name.getBytes()))
        .build();
  }

  private static void addAccountToStore(Account account) {
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(account.getAddress().toByteArray(), accountWrapper);
  }

  @Test
  public void addAssetIssueToStoreV2() {
    byte[] id = ByteArray.fromString("100000");
    addAssetIssueToStoreV2(assetIssue1, id);
    AssetIssueWrapper assetIssueWrapper = dbManager.getAssetIssueV2Store().get(id);
    Assert.assertEquals(true, assetIssueWrapper.getName().equals(assetIssue1.getName()));
  }

  @Test
  public void UpdateAssetV2() {
    long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();

    for (AssetIssueWrapper assetIssueWrapper : dbManager.getAssetIssueStore().getAllAssetIssues()) {
      dbManager.getAssetIssueV2Store().put(ByteArray.fromLong(tokenIdNum), assetIssueWrapper);
      assetIssueWrapper.setId(String.valueOf(tokenIdNum));
      tokenIdNum++;
    }
    Assert.assertEquals(2, dbManager.getAssetIssueV2Store().getAllAssetIssues().size());

  }

  @Test
  public void getTransactionsFromThis() {
    List<Transaction> transactionList = storeAPI
        .getTransactionsFromThis(ACCOUNT_ADDRESS_ONE, 0, 1000);
    Assert.assertEquals("TransactionsFromThis1", transaction1, transactionList.get(0));
    transactionList = storeAPI.getTransactionsFromThis(ACCOUNT_ADDRESS_TWO, 0, 1000);
    Assert.assertEquals("TransactionsFromThis2", transaction2, transactionList.get(0));
    transactionList = storeAPI.getTransactionsFromThis(null, 0, 1000);
    Assert.assertEquals("TransactionsFromThis3", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsFromThis("", 0, 1000);
    Assert.assertEquals("TransactionsFromThis4", 0, transactionList.size());
  }

  @Test
  public void getTransactionsToThis() {

    List<Transaction> transactionList = storeAPI
        .getTransactionsToThis(ACCOUNT_ADDRESS_TWO, 0, 1000);
    Assert.assertEquals("TransactionsToThis1", transaction1, transactionList.get(0));
    transactionList = storeAPI.getTransactionsToThis(ACCOUNT_ADDRESS_THREE, 0, 1000);
    Assert.assertEquals("TransactionsToThis2", transaction2, transactionList.get(0));
    transactionList = storeAPI.getTransactionsToThis(null, 0, 1000);
    Assert.assertEquals("TransactionsToThis3", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsToThis("", 0, 1000);
    Assert.assertEquals("TransactionsToThis4", 0, transactionList.size());
  }

  @Test
  public void getTransactionById() {
    try {
      Transaction transaction =
          storeAPI.getTransactionById(
              new TransactionWrapper(transaction1).getTransactionId().toString());
      Assert.assertEquals("TransactionById1", transaction1, transaction);
      transaction =
          storeAPI.getTransactionById(
              new TransactionWrapper(transaction2).getTransactionId().toString());
      Assert.assertEquals("TransactionById2", transaction2, transaction);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }
}
