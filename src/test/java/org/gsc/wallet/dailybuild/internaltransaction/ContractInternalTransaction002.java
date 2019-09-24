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

package org.gsc.wallet.dailybuild.internaltransaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j

public class ContractInternalTransaction002 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelConfirmed = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForinternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForinternalTxsAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }


  @Test(enabled = true, description = "Type is create create call call")
  public void test1InternalTransaction007() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode_v0.5.4/"
        + "contractInternalTransaction002test1InternalTransaction007.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName1 = "C";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";

    String txid = "";

    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertNotNull(infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertTrue(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("create", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);
    Assert.assertTrue(5 == vaule3);
    Assert.assertTrue(0 == vaule4);
    String initParmes1 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "test2(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(1, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    String note5 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    Long vaule5 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(1 == vaule5);
    Assert.assertEquals("call", note5);
    Assert.assertTrue(infoById1.get().getInternalTransactions(0).getRejected());


  }

  @Test(enabled = true, description = "Type is call call")
  public void test2InternalTransaction008() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode_v0.5.4/"
        + "contractInternalTransaction002test2InternalTransaction008.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "B";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "testAssert(address,uint256)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    Assert.assertTrue(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertFalse(infoById.get().getInternalTransactions(1).getRejected());

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note);
    Assert.assertEquals("call", note1);
    Assert.assertTrue(1 == vaule1);
    Assert.assertTrue(1 == vaule2);
    String contractName2 = "C";
    HashMap retMap2 = PublicMethed.getByCodeAbi(filePath, contractName2);
    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abi").toString();

    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes1 = "\"" + Base58.encode58Check(contractAddress2) + "\",\"1\"";
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "testRequire(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    Assert.assertTrue(infoById1.get().getInternalTransactions(0).getRejected());
    Assert.assertFalse(infoById1.get().getInternalTransactions(1).getRejected());
    String note2 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule3 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById1.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(1 == vaule3);
    Assert.assertTrue(1 == vaule4);

    String txid2 = PublicMethed.triggerContract(contractAddress,
        "testAssert1(address,uint256)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    int transactionsCount2 = infoById2.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount2);
    dupInternalTrsansactionHash(infoById2.get().getInternalTransactionsList());
    Assert.assertFalse(infoById2.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById2.get().getInternalTransactions(1).getRejected());

    String note5 = ByteArray
        .toStr(infoById2.get().getInternalTransactions(0).getNote().toByteArray());
    String note6 = ByteArray
        .toStr(infoById2.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule5 = infoById2.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule6 = infoById2.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note5);
    Assert.assertEquals("call", note6);
    Assert.assertTrue(1 == vaule5);
    Assert.assertTrue(1 == vaule6);

    String txid3 = PublicMethed.triggerContract(contractAddress,
        "testtRequire2(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);
    int transactionsCount3 = infoById3.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount3);
    dupInternalTrsansactionHash(infoById3.get().getInternalTransactionsList());

    Assert.assertFalse(infoById3.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById3.get().getInternalTransactions(1).getRejected());
    String note7 = ByteArray
        .toStr(infoById3.get().getInternalTransactions(0).getNote().toByteArray());
    String note8 = ByteArray
        .toStr(infoById3.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule7 = infoById3.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule8 = infoById3.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note7);
    Assert.assertEquals("call", note8);
    Assert.assertTrue(1 == vaule7);
    Assert.assertTrue(1 == vaule8);

  }

  @Test(enabled = true, description = "Test suicide type in internalTransaction after call")
  public void test3InternalTransaction009() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode_v0.5.4/"
        + "contractInternalTransaction002test3InternalTransaction009.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "B";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName2 = "C";
    HashMap retMap2 = PublicMethed.getByCodeAbi(filePath, contractName2);
    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abi").toString();
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName3 = "D";
    HashMap retMap3 = PublicMethed.getByCodeAbi(filePath, contractName3);
    String code3 = retMap3.get("byteCode").toString();
    String abi3 = retMap3.get("abi").toString();
    byte[] contractAddress3 = PublicMethed
        .deployContract(contractName3, abi3, code3, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress2)
        + "\",\"" + Base58.encode58Check(contractAddress3) + "\",\"" + Base58
        .encode58Check(contractAddress1) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(7, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(6).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertEquals("suicide", note2);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(5 == vaule2);

    String txid1 = "";
    txid1 = PublicMethed.triggerContract(contractAddress,
        "test1(address,address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(6, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
  }

  @Test(enabled = false, description = "Test maxfeelimit can trigger create type max time")
  public void test4InternalTransaction010() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode_v0.5.4/"
        + "contractInternalTransaction002test4InternalTransaction010.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "transfer()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(76, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid1 = PublicMethed.triggerContract(contractAddress,
        "transfer2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(76, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById1.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById1.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());

    }


  }


  @Test(enabled = true, description = "Type is call create->call->call.Three-level nesting")
  public void test5InternalTransaction012() {
    Assert.assertTrue(PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode_v0.5.4/"
        + "contractInternalTransaction002test5InternalTransaction012.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "B";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName2 = "E";
    HashMap retMap2 = PublicMethed.getByCodeAbi(filePath, contractName2);
    String code2 = retMap1.get("byteCode").toString();
    String abi2 = retMap1.get("abi").toString();
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "test1(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Assert.assertEquals("call", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);

    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(1 == vaule1);
    Assert.assertTrue(1000 == vaule2);
    Assert.assertTrue(0 == vaule3);
    Assert.assertTrue(1 == vaule4);


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelConfirmed != null) {
      channelConfirmed.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


  /**
   * constructor.
   */

  public void dupInternalTrsansactionHash(
      List<org.gsc.protos.Protocol.InternalTransaction> internalTransactionList) {
    List<String> hashList = new ArrayList<>();
    internalTransactionList.forEach(
        internalTransaction -> hashList
            .add(Hex.toHexString(internalTransaction.getHash().toByteArray())));
    List<String> dupHash = hashList.stream()
        .collect(Collectors.toMap(e -> e, e -> 1, (a, b) -> a + b))
        .entrySet().stream().filter(entry -> entry.getValue() > 1).map(entry -> entry.getKey())
        .collect(Collectors.toList());
    Assert.assertEquals(dupHash.size(), 0);
  }
}
