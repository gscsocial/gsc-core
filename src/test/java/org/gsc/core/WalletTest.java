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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BlockList;
import org.gsc.api.GrpcAPI.ExchangeList;
import org.gsc.api.GrpcAPI.ProposalList;
import org.gsc.application.GSCApplicationContext;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.Utils;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter.ChainParameters;
import org.gsc.config.args.Args;
import org.gsc.db.DynamicPropertiesStore;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.BlockHeader;
import org.gsc.protos.Protocol.BlockHeader.raw;
//import org.gsc.protos.Protocol.DeferredTransaction;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.protos.Protocol.Proposal;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class WalletTest {

  private static GSCApplicationContext context;
  private static Wallet wallet;
  private static Manager manager;
  private static String dbPath = "db_wallet_test";
  public static final String ACCOUNT_ADDRESS_ONE = "121212a9cf";
  public static final String ACCOUNT_ADDRESS_TWO = "232323a9cf";
  public static final String ACCOUNT_ADDRESS_THREE = "343434a9cf";
  public static final String ACCOUNT_ADDRESS_FOUR = "454545a9cf";
  public static final String ACCOUNT_ADDRESS_FIVE = "565656a9cf";
  public static final String ACCOUNT_ADDRESS_SIX = "12344349cf";
  private static Block block1;
  private static Block block2;
  private static Block block3;
  private static Block block4;
  private static Block block5;
  public static final long BLOCK_NUM_ONE = 1;
  public static final long BLOCK_NUM_TWO = 2;
  public static final long BLOCK_NUM_THREE = 3;
  public static final long BLOCK_NUM_FOUR = 4;
  public static final long BLOCK_NUM_FIVE = 5;
  public static final long BLOCK_TIMESTAMP_ONE = DateTime.now().minusDays(4).getMillis();
  public static final long BLOCK_TIMESTAMP_TWO = DateTime.now().minusDays(3).getMillis();
  public static final long BLOCK_TIMESTAMP_THREE = DateTime.now().minusDays(2).getMillis();
  public static final long BLOCK_TIMESTAMP_FOUR = DateTime.now().minusDays(1).getMillis();
  public static final long BLOCK_TIMESTAMP_FIVE = DateTime.now().getMillis();
  public static final long BLOCK_WITNESS_ONE = 12;
  public static final long BLOCK_WITNESS_TWO = 13;
  public static final long BLOCK_WITNESS_THREE = 14;
  public static final long BLOCK_WITNESS_FOUR = 15;
  public static final long BLOCK_WITNESS_FIVE = 16;
  private static Transaction transaction1;
  private static Transaction transaction2;
  private static Transaction transaction3;
  private static Transaction transaction4;
  private static Transaction transaction5;
  private static Transaction transaction6;
  //private static DeferredTransaction deferredTransaction;
  public static final long TRANSACTION_TIMESTAMP_ONE = DateTime.now().minusDays(4).getMillis();
  public static final long TRANSACTION_TIMESTAMP_TWO = DateTime.now().minusDays(3).getMillis();
  public static final long TRANSACTION_TIMESTAMP_THREE = DateTime.now().minusDays(2).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FOUR = DateTime.now().minusDays(1).getMillis();
  public static final long TRANSACTION_TIMESTAMP_FIVE = DateTime.now().getMillis();
  private static AssetIssueWrapper Asset1;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    wallet = context.getBean(Wallet.class);
    manager = context.getBean(Manager.class);
    initTransaction();
    initBlock();
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(5);
  }

  /**
   * initTransaction.
   */
  private static void initTransaction() {
    transaction1 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_ONE, ACCOUNT_ADDRESS_TWO),
        TRANSACTION_TIMESTAMP_ONE, BLOCK_NUM_ONE);
    addTransactionToStore(transaction1);

    transaction2 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_TWO, ACCOUNT_ADDRESS_THREE),
        TRANSACTION_TIMESTAMP_TWO, BLOCK_NUM_TWO);
    addTransactionToStore(transaction2);

    transaction3 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_THREE, ACCOUNT_ADDRESS_FOUR),
        TRANSACTION_TIMESTAMP_THREE, BLOCK_NUM_THREE);
    addTransactionToStore(transaction3);

    transaction4 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_FOUR, ACCOUNT_ADDRESS_FIVE),
        TRANSACTION_TIMESTAMP_FOUR, BLOCK_NUM_FOUR);
    addTransactionToStore(transaction4);

    transaction5 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_FIVE, ACCOUNT_ADDRESS_ONE),
        TRANSACTION_TIMESTAMP_FIVE, BLOCK_NUM_FIVE);
    addTransactionToStore(transaction5);

    transaction6 = getBuildTransaction(
        getBuildTransferContract(ACCOUNT_ADDRESS_ONE, ACCOUNT_ADDRESS_SIX),
        TRANSACTION_TIMESTAMP_FIVE, BLOCK_NUM_FIVE);
    addTransactionToStore(transaction5);
  }

  private static void addTransactionToStore(Transaction transaction) {
    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
    manager.getTransactionStore()
        .put(transactionWrapper.getTransactionId().getBytes(), transactionWrapper);
  }

  private static void   addTransactionInfoToStore(Transaction transaction) {
    TransactionInfoWrapper transactionInfo = new TransactionInfoWrapper();
    byte[] trxId = transaction.getRawData().toByteArray();
    transactionInfo.setId(trxId);
    manager.getTransactionHistoryStore().put(trxId, transactionInfo);
  }


  private static Transaction getBuildTransaction(
      TransferContract transferContract, long transactionTimestamp, long refBlockNum) {
    return Transaction.newBuilder().setRawData(
        Transaction.raw.newBuilder().setTimestamp(transactionTimestamp)
            .setRefBlockNum(refBlockNum)
            .addContract(
                Contract.newBuilder().setType(ContractType.TransferContract)
                    .setParameter(Any.pack(transferContract)).build()).build())
        .build();
  }

  private static TransferContract getBuildTransferContract(String ownerAddress, String toAddress) {
    return TransferContract.newBuilder().setAmount(10)
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress))).build();
  }

  /**
   * initBlock.
   */
  private static void initBlock() {

    block1 = getBuildBlock(BLOCK_TIMESTAMP_ONE, BLOCK_NUM_ONE, BLOCK_WITNESS_ONE,
        ACCOUNT_ADDRESS_ONE, transaction1, transaction2);
    addBlockToStore(block1);
    addTransactionInfoToStore(transaction1);

    block2 = getBuildBlock(BLOCK_TIMESTAMP_TWO, BLOCK_NUM_TWO, BLOCK_WITNESS_TWO,
        ACCOUNT_ADDRESS_TWO, transaction2, transaction3);
    addBlockToStore(block2);
    addTransactionInfoToStore(transaction2);

    block3 = getBuildBlock(BLOCK_TIMESTAMP_THREE, BLOCK_NUM_THREE, BLOCK_WITNESS_THREE,
        ACCOUNT_ADDRESS_THREE, transaction2, transaction4);
    addBlockToStore(block3);
    addTransactionInfoToStore(transaction3);

    block4 = getBuildBlock(BLOCK_TIMESTAMP_FOUR, BLOCK_NUM_FOUR, BLOCK_WITNESS_FOUR,
        ACCOUNT_ADDRESS_FOUR, transaction4, transaction5);
    addBlockToStore(block4);
    addTransactionInfoToStore(transaction4);

    block5 = getBuildBlock(BLOCK_TIMESTAMP_FIVE, BLOCK_NUM_FIVE, BLOCK_WITNESS_FIVE,
        ACCOUNT_ADDRESS_FIVE, transaction5, transaction3);
    addBlockToStore(block5);
    addTransactionInfoToStore(transaction5);
  }

  private static void addBlockToStore(Block block) {
    BlockWrapper blockWrapper = new BlockWrapper(block);
    manager.getBlockStore().put(blockWrapper.getBlockId().getBytes(), blockWrapper);
  }

  private static Block getBuildBlock(long timestamp, long num, long witnessId,
      String witnessAddress, Transaction transaction, Transaction transactionNext) {
    return Block.newBuilder().setBlockHeader(BlockHeader.newBuilder().setRawData(
        raw.newBuilder().setTimestamp(timestamp).setNumber(num).setWitnessId(witnessId)
            .setWitnessAddress(ByteString.copyFrom(ByteArray.fromHexString(witnessAddress)))
            .build()).build()).addTransactions(transaction).addTransactions(transactionNext)
        .build();
  }


  private static void buildAssetIssue() {
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    builder.setName(ByteString.copyFromUtf8("Asset1"));
    Asset1 = new AssetIssueWrapper(builder.build());
    manager.getAssetIssueStore().put(Asset1.createDbKey(), Asset1);
  }

  private static void buildProposal() {
    Proposal.Builder builder = Proposal.newBuilder();
    builder.setProposalId(1L).setProposerAddress(ByteString.copyFromUtf8("Address1"));
    ProposalWrapper proposalWrapper = new ProposalWrapper(builder.build());
    manager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);

    builder.setProposalId(2L).setProposerAddress(ByteString.copyFromUtf8("Address2"));
    proposalWrapper = new ProposalWrapper(builder.build());
    manager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
    manager.getDynamicPropertiesStore().saveLatestProposalNum(2L);
  }

  private static void buildExchange() {
    Exchange.Builder builder = Exchange.newBuilder();
    builder.setExchangeId(1L).setCreatorAddress(ByteString.copyFromUtf8("Address1"));
    ExchangeWrapper exchangeWrapper = new ExchangeWrapper(builder.build());
    manager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);

    builder.setExchangeId(2L).setCreatorAddress(ByteString.copyFromUtf8("Address2"));
    exchangeWrapper = new ExchangeWrapper(builder.build());
    manager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);

    manager.getDynamicPropertiesStore().saveLatestExchangeNum(2L);

  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testWallet() {
    Wallet wallet1 = new Wallet();
    Wallet wallet2 = new Wallet();
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    logger.info("wallet2 address = {}", ByteArray.toHexString(wallet2
        .getAddress()));
    assertFalse(wallet1.getAddress().equals(wallet2.getAddress()));
  }

  @Test
  public void testGetAddress() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    Wallet wallet1 = new Wallet(ecKey);
    logger.info("ecKey address = {}", ByteArray.toHexString(ecKey
        .getAddress()));
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    assertArrayEquals(wallet1.getAddress(), ecKey.getAddress());
  }

  @Test
  public void testGetEcKey() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    Wallet wallet1 = new Wallet(ecKey);
    logger.info("ecKey address = {}", ByteArray.toHexString(ecKey
        .getAddress()));
    logger.info("wallet address = {}", ByteArray.toHexString(wallet1
        .getAddress()));
    assertEquals("Wallet ECKey should match provided ECKey", wallet1.getEcKey(), ecKey);
  }

  @Test
  public void ss() {
    for (int i = 0; i < 4; i++) {
      ECKey ecKey = new ECKey(Utils.getRandom());
      System.out.println(i + 1);
      System.out.println("privateKey:" + ByteArray.toHexString(ecKey.getPrivKeyBytes()));
      System.out.println("publicKey:" + ByteArray.toHexString(ecKey.getPubKey()));
      System.out.println("address:" + ByteArray.toHexString(ecKey.getAddress()));
      System.out.println();
    }
  }

  @Test
  public void getBlockById() {
    Block blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockWrapper(block1).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById1", block1, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockWrapper(block2).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById2", block2, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockWrapper(block3).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById3", block3, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockWrapper(block4).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById4", block4, blockById);
    blockById = wallet
        .getBlockById(ByteString.copyFrom(new BlockWrapper(block5).getBlockId().getBytes()));
    Assert.assertEquals("getBlockById5", block5, blockById);
  }

  @Test
  public void getBlocksByLimit() {
    BlockList blocksByLimit = wallet.getBlocksByLimitNext(3, 2);
    Assert.assertTrue("getBlocksByLimit1", blocksByLimit.getBlockList().contains(block3));
    Assert.assertTrue("getBlocksByLimit2", blocksByLimit.getBlockList().contains(block4));
    blocksByLimit = wallet.getBlocksByLimitNext(0, 5);
    Assert.assertTrue("getBlocksByLimit3",
        blocksByLimit.getBlockList().contains(manager.getGenesisBlock().getInstance()));
    Assert.assertTrue("getBlocksByLimit4", blocksByLimit.getBlockList().contains(block1));
    Assert.assertTrue("getBlocksByLimit5", blocksByLimit.getBlockList().contains(block2));
    Assert.assertTrue("getBlocksByLimit6", blocksByLimit.getBlockList().contains(block3));
    Assert.assertTrue("getBlocksByLimit7", blocksByLimit.getBlockList().contains(block4));
    Assert.assertFalse("getBlocksByLimit8", blocksByLimit.getBlockList().contains(block5));
  }

  @Test
  public void getTransactionInfoById() {
    TransactionInfo transactionById1 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction1.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById1.getId().toByteArray()),
        ByteString.copyFrom(transaction1.getRawData().toByteArray()));

    TransactionInfo transactionById2 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction2.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById2.getId().toByteArray()),
        ByteString.copyFrom(transaction2.getRawData().toByteArray()));

    TransactionInfo transactionById3 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction3.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById3.getId().toByteArray()),
        ByteString.copyFrom(transaction3.getRawData().toByteArray()));

    TransactionInfo transactionById4 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction4.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById4.getId().toByteArray()),
        ByteString.copyFrom(transaction4.getRawData().toByteArray()));

    TransactionInfo transactionById5 = wallet.getTransactionInfoById(
        ByteString
            .copyFrom(transaction5.getRawData().toByteArray()));
    Assert.assertEquals("gettransactioninfobyid",
        ByteString.copyFrom(transactionById5.getId().toByteArray()),
        ByteString.copyFrom(transaction5.getRawData().toByteArray()));
  }

  @Ignore
  @Test
  public void getTransactionById() {
    Transaction transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionWrapper(transaction1).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById1", transaction1, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionWrapper(transaction2).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById2", transaction2, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionWrapper(transaction3).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById3", transaction3, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionWrapper(transaction4).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById4", transaction4, transactionById);
    transactionById = wallet.getTransactionById(
        ByteString
            .copyFrom(new TransactionWrapper(transaction5).getTransactionId().getBytes()));
    Assert.assertEquals("getTransactionById5", transaction5, transactionById);
  }

  @Test
  public void getBlockByLatestNum() {
    BlockList blockByLatestNum = wallet.getBlockByLatestNum(2);
    Assert.assertTrue("getBlockByLatestNum1", blockByLatestNum.getBlockList().contains(block5));
    Assert.assertTrue("getBlockByLatestNum2", blockByLatestNum.getBlockList().contains(block4));
  }

  @Test
  public void getPaginatedAssetIssueList() {
    buildAssetIssue();
    AssetIssueList assetList1 = wallet.getAssetIssueList(0, 100);
    Assert.assertTrue("get Asset1", assetList1.getAssetIssue(0).getName().equals(Asset1.getName()));
    try {
      assetList1.getAssetIssue(1);
    } catch (Exception e) {
      Assert.assertTrue("AssetIssueList1 size should be 1", true);
    }

    AssetIssueList assetList2 = wallet.getAssetIssueList(0, 0);
    try {
      assetList2.getAssetIssue(0);
    } catch (Exception e) {
      Assert.assertTrue("AssetIssueList2 size should be 0", true);
    }
  }

  @Test
  public void getPaginatedProposalList() {
    buildProposal();
    //
    ProposalList proposalList = wallet.getPaginatedProposalList(0, 100);

    Assert.assertEquals(2, proposalList.getProposalsCount());
    Assert.assertEquals("Address1",
        proposalList.getProposalsList().get(0).getProposerAddress().toStringUtf8());
    Assert.assertEquals("Address2",
        proposalList.getProposalsList().get(1).getProposerAddress().toStringUtf8());

    //
    proposalList = wallet.getPaginatedProposalList(1, 100);

    Assert.assertEquals(1, proposalList.getProposalsCount());
    Assert.assertEquals("Address2",
        proposalList.getProposalsList().get(0).getProposerAddress().toStringUtf8());

    //
    proposalList = wallet.getPaginatedProposalList(-1, 100);
    Assert.assertNull(proposalList);

    //
    proposalList = wallet.getPaginatedProposalList(0, -1);
    Assert.assertNull(proposalList);

    //
    proposalList = wallet.getPaginatedProposalList(0, 1000000000L);
    Assert.assertEquals(2, proposalList.getProposalsCount());

  }

  @Test
  public void getPaginatedExchangeList() {
    buildExchange();
    ExchangeList exchangeList = wallet.getPaginatedExchangeList(0, 100);
    Assert.assertEquals("Address1",
        exchangeList.getExchangesList().get(0).getCreatorAddress().toStringUtf8());
    Assert.assertEquals("Address2",
        exchangeList.getExchangesList().get(1).getCreatorAddress().toStringUtf8());
  }

  //@Test
  public void testChainParameters() {

    Protocol.ChainParameters.Builder builder = Protocol.ChainParameters.newBuilder();

    Arrays.stream(ChainParameters.values()).forEach(parameters -> {
      String methodName = Wallet.makeUpperCamelMethod(parameters.name());
      try {
        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey(methodName)
            .setValue((long) DynamicPropertiesStore.class.getDeclaredMethod(methodName)
                .invoke(manager.getDynamicPropertiesStore()))
            .build());
      } catch (Exception ex) {
        Assert.fail("get chainParameter : " + methodName + ", error : " + ex.getMessage());
      }

    });

    System.out.printf(builder.build().toString());
  }

}
