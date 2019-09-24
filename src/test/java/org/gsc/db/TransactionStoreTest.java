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

package org.gsc.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Random;

import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract.AccountCreateContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Contract.VoteWitnessContract;
import org.gsc.protos.Contract.VoteWitnessContract.Vote;
import org.gsc.protos.Contract.WitnessCreateContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;

@Ignore
public class TransactionStoreTest {

  private static String dbPath = "db_TransactionStore_test";
  private static String dbDirectory = "db_TransactionStore_test";
  private static String indexDirectory = "index_TransactionStore_test";
  private static TransactionStore transactionStore;
  private static GSCApplicationContext context;
  private static Application AppT;
  private static final byte[] key1 = TransactionStoreTest.randomBytes(21);
  private static Manager dbManager;
  private static final byte[] key2 = TransactionStoreTest.randomBytes(21);


  private static final String URL = "https://gsc.network";

  private static final String ACCOUNT_NAME = "ownerF";
  private static final String OWNER_ADDRESS =
      Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
  private static final String TO_ADDRESS =
      Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
  private static final long AMOUNT = 100;
  private static final String WITNESS_ADDRESS =
      Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

  static {
    Args.setParam(
        new String[]{
            "--db-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w"
        },
        Constant.TEST_NET_CONF
    );
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    transactionStore = dbManager.getTransactionStore();

  }

  /**
   * get AccountCreateContract.
   */
  private AccountCreateContract getContract(String name, String address) {
    return AccountCreateContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .build();
  }

  /**
   * get TransferContract.
   */
  private TransferContract getContract(long count, String owneraddress, String toaddress) {
    return TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owneraddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toaddress)))
        .setAmount(count)
        .build();
  }

  /**
   * get WitnessCreateContract.
   */
  private WitnessCreateContract getWitnessContract(String address, String url) {
    return WitnessCreateContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setUrl(ByteString.copyFrom(ByteArray.fromString(url)))
        .build();
  }

  /**
   * get VoteWitnessContract.
   */
  private VoteWitnessContract getVoteWitnessContract(String address, String voteaddress,
      Long value) {
    return
        VoteWitnessContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .addVotes(Vote.newBuilder()
                .setVoteAddress(ByteString.copyFrom(ByteArray.fromHexString(voteaddress)))
                .setVoteCount(value).build())
            .build();
  }

  @Test
  public void GetTransactionTest() throws BadItemException, ItemNotFoundException {
    final BlockStore blockStore = dbManager.getBlockStore();
    final TransactionStore trxStore = dbManager.getTransactionStore();
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";

    BlockWrapper blockWrapper =
        new BlockWrapper(
            1,
            Sha256Hash.wrap(dbManager.getGenesisBlockId().getByteString()),
            1,ByteString.EMPTY,
                ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(key)).getAddress()));

    // save in database with block number
    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionWrapper trx = new TransactionWrapper(tc, ContractType.TransferContract);
    blockWrapper.addTransaction(trx);
    trx.setBlockNum(blockWrapper.getNum());
    blockStore.put(blockWrapper.getBlockId().getBytes(), blockWrapper);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.get(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());

    // no found in transaction store database
    tc =
        TransferContract.newBuilder()
            .setAmount(1000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionWrapper(tc, ContractType.TransferContract);
    Assert.assertNull(trxStore.get(trx.getTransactionId().getBytes()));

    // no block number, directly save in database
    tc =
        TransferContract.newBuilder()
            .setAmount(10000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionWrapper(tc, ContractType.TransferContract);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.get(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());
  }

  /**
   * put and get CreateAccountTransaction.
   */
  @Test
  public void CreateAccountTransactionStoreTest() throws BadItemException {
    AccountCreateContract accountCreateContract = getContract(ACCOUNT_NAME,
        OWNER_ADDRESS);
    TransactionWrapper ret = new TransactionWrapper(accountCreateContract,
        dbManager.getAccountStore());
    transactionStore.put(key1, ret);
    Assert.assertEquals("Store CreateAccountTransaction is error",
        transactionStore.get(key1).getInstance(),
        ret.getInstance());
    Assert.assertTrue(transactionStore.has(key1));
  }

  @Test
  public void GetUncheckedTransactionTest() {
    final BlockStore blockStore = dbManager.getBlockStore();
    final TransactionStore trxStore = dbManager.getTransactionStore();
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";

    BlockWrapper blockWrapper =
        new BlockWrapper(
            1,
            Sha256Hash.wrap(dbManager.getGenesisBlockId().getByteString()),
            1,ByteString.EMPTY,
                ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(key)).getAddress()));

    // save in database with block number
    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionWrapper trx = new TransactionWrapper(tc, ContractType.TransferContract);
    blockWrapper.addTransaction(trx);
    trx.setBlockNum(blockWrapper.getNum());
    blockStore.put(blockWrapper.getBlockId().getBytes(), blockWrapper);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.getUnchecked(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());

    // no found in transaction store database
    tc =
        TransferContract.newBuilder()
            .setAmount(1000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionWrapper(tc, ContractType.TransferContract);
    Assert.assertNull(trxStore.getUnchecked(trx.getTransactionId().getBytes()));

    // no block number, directly save in database
    tc =
        TransferContract.newBuilder()
            .setAmount(10000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionWrapper(tc, ContractType.TransferContract);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.getUnchecked(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());
  }

  /**
   * put and get CreateWitnessTransaction.
   */
  @Test
  public void CreateWitnessTransactionStoreTest() throws BadItemException {
    WitnessCreateContract witnessContract = getWitnessContract(OWNER_ADDRESS, URL);
    TransactionWrapper transactionWrapper = new TransactionWrapper(witnessContract);
    transactionStore.put(key1, transactionWrapper);
    Assert.assertEquals("Store CreateWitnessTransaction is error",
        transactionStore.get(key1).getInstance(),
        transactionWrapper.getInstance());
  }

  /**
   * put and get TransferTransaction.
   */
  @Test
  public void TransferTransactionStorenTest() throws BadItemException {
    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.AssetIssue,
            1000000L
        );
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
    TransferContract transferContract = getContract(AMOUNT, OWNER_ADDRESS, TO_ADDRESS);
    TransactionWrapper transactionWrapper = new TransactionWrapper(transferContract,
        dbManager.getAccountStore());
    transactionStore.put(key1, transactionWrapper);
    Assert.assertEquals("Store TransferTransaction is error",
        transactionStore.get(key1).getInstance(),
        transactionWrapper.getInstance());
  }

  /**
   * put and get VoteWitnessTransaction.
   */

  @Test
  public void voteWitnessTransactionTest() throws BadItemException {

    AccountWrapper ownerAccountFirstWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            1_000_000_000_000L);
    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    ownerAccountFirstWrapper.setFrozen(frozenBalance, duration);
    dbManager.getAccountStore()
        .put(ownerAccountFirstWrapper.getAddress().toByteArray(), ownerAccountFirstWrapper);
    VoteWitnessContract operator = getVoteWitnessContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L);
    TransactionWrapper transactionWrapper = new TransactionWrapper(operator);
    transactionStore.put(key1, transactionWrapper);
    Assert.assertEquals("Store VoteWitnessTransaction is error",
        transactionStore.get(key1).getInstance(),
        transactionWrapper.getInstance());
  }

  /**
   * put value is null and get it.
   */
  @Test
  public void TransactionValueNullTest() throws BadItemException {
    TransactionWrapper transactionWrapper = null;
    transactionStore.put(key2, transactionWrapper);
    Assert.assertNull("put value is null", transactionStore.get(key2));

  }

  /**
   * put key is null and get it.
   */
  @Test
  public void TransactionKeyNullTest() throws BadItemException {
    AccountCreateContract accountCreateContract = getContract(ACCOUNT_NAME,
        OWNER_ADDRESS);
    TransactionWrapper ret = new TransactionWrapper(accountCreateContract,
        dbManager.getAccountStore());
    byte[] key = null;
    transactionStore.put(key, ret);
    try {
      transactionStore.get(key);
    } catch (RuntimeException e) {
      Assert.assertNull(e.getMessage());
    }
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }


  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    byte[] addressPre = Wallet.getAddressPreFixByte();
    System.arraycopy(addressPre, 0, result, 0, addressPre.length);
    return result;
  }
}
