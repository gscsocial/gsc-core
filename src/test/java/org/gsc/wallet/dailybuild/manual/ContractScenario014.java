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

package org.gsc.wallet.dailybuild.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractScenario014 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress1 = null;
  byte[] contractAddress2 = null;
  byte[] contractAddress3 = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  String contractName = "";

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract014Address = ecKey1.getAddress();
  String contract014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String priKey014 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey2.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Triple trigger in smart contract")
  public void testTripleTrigger() {

    ecKey2 = new ECKey(Utils.getRandom());
    receiverAddress = ecKey2.getAddress();
    receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(contract014Key);
    PublicMethed.printAddress(receiverKey);

    Assert.assertTrue(PublicMethed.sendcoin(contract014Address, 5000000000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceGetCpu(contract014Address, 1000000000000L, 0, 1, priKey014,
            blockingStubFull));

    logger.info("contract014Address : == " + contract014Key);
    //Deploy contract1, contract1 has a function to transaction 5 sun to target account
    String contractName = "Contract1";
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractScenario014.sol";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, contract014Key, contract014Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress1 = infoById.get().getContractAddress().toByteArray();

    //Deploy contract2, contract2 has a function to call contract1 transaction sun function.
    // and has a revert function.
    contractName = "contract2";
    String filePath1 = "./src/test/resources/soliditycode_v0.5.4/contractScenario014.sol";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath1, contractName);

    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String parame = "\"" + Base58.encode58Check(contractAddress1) + "\"";

    txid = PublicMethed.deployContractWithConstantParame(contractName, abi1, code1,
        "constructor(address)", parame, "", maxFeeLimit, 0L, 100, null,
        contract014Key, contract014Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress2 = infoById.get().getContractAddress().toByteArray();

    //Deploy contract3, trigger contract2 function.
    contractName = "contract3";
    String filePath2 = "./src/test/resources/soliditycode_v0.5.4/contractScenario014.sol";
    HashMap retMap2 = PublicMethed.getByCodeAbi(filePath2, contractName);

    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abi").toString();
    parame = "\"" + Base58.encode58Check(contractAddress2) + "\"";

    txid = PublicMethed.deployContractWithConstantParame(contractName, abi2, code2,
        "constructor(address)", parame, "", maxFeeLimit, 0L, 100, null,
        contract014Key, contract014Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress3 = infoById.get().getContractAddress().toByteArray();

    Assert.assertTrue(PublicMethed.sendcoin(contractAddress1, 1000000L, fromAddress, testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverAddress, 1000000L, fromAddress, testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(contractAddress2, 1000000L, fromAddress, testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(contractAddress3, 1000000L, fromAddress, testKey002,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Test contract2 trigger contract1 to test call function
    Account contract2AccountInfo = PublicMethed.queryAccount(contractAddress2, blockingStubFull);
    final Long contract2BeforeBalance = contract2AccountInfo.getBalance();
    Account receiverAccountInfo = PublicMethed.queryAccount(receiverAddress, blockingStubFull);
    Long receiverBeforeBalance = receiverAccountInfo.getBalance();
    Account contract1AccountInfo = PublicMethed.queryAccount(contractAddress1, blockingStubFull);
    Long contract1BeforeBalance = contract1AccountInfo.getBalance();
    logger.info("before contract1 balance is " + Long.toString(contract1BeforeBalance));
    logger.info("before receiver balance is " + Long.toString(receiverBeforeBalance));
    String receiveAddress = "\"" + Base58.encode58Check(receiverAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress2,
        "triggerContract1(address)", receiveAddress, false,
        0, maxFeeLimit, contract014Address, contract014Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contract2AccountInfo = PublicMethed.queryAccount(contractAddress2, blockingStubFull);
    final Long contract2AfterBalance = contract2AccountInfo.getBalance();
    //contract2AccountInfo.getAccountResource().getFrozenBalanceForCpu();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress, blockingStubFull);
    Long receiverAfterBalance = receiverAccountInfo.getBalance();
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1, blockingStubFull);
    Long contract1AfterBalance = contract1AccountInfo.getBalance();
    logger.info("after contract1 balance is " + Long.toString(contract1AfterBalance));
    Assert.assertTrue(receiverAfterBalance - receiverBeforeBalance == 5);
    Assert.assertTrue(contract2BeforeBalance - contract2AfterBalance == 0);
    Assert.assertTrue(contract1BeforeBalance - contract1AfterBalance == 5);

    //Test contract2 trigger contract1 but revert
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1, blockingStubFull);
    contract1BeforeBalance = contract1AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress, blockingStubFull);
    receiverBeforeBalance = receiverAccountInfo.getBalance();
    receiveAddress = "\"" + Base58.encode58Check(receiverAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress2,
        "triggerContract1ButRevert(address)", receiveAddress, false,
        0, 10000000L, contract014Address, contract014Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1, blockingStubFull);
    contract1AfterBalance = contract1AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress, blockingStubFull);
    receiverAfterBalance = receiverAccountInfo.getBalance();
    logger.info("after receiver balance is " + Long.toString(receiverAfterBalance));
    Assert.assertTrue(receiverAfterBalance - receiverBeforeBalance == 0);
    Assert.assertTrue(contract1BeforeBalance - contract1AfterBalance == 0);

    //Test contract3 trigger contract2 to call contract1
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1, blockingStubFull);
    contract1BeforeBalance = contract1AccountInfo.getBalance();
    Account contract3AccountInfo = PublicMethed.queryAccount(contractAddress3, blockingStubFull);
    final Long contract3BeforeBalance = contract3AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress, blockingStubFull);
    receiverBeforeBalance = receiverAccountInfo.getBalance();
    logger.info("before receiver balance is " + Long.toString(receiverBeforeBalance));
    logger.info("before contract3 balance is " + Long.toString(contract3BeforeBalance));
    receiveAddress = "\"" + Base58.encode58Check(receiverAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress3,
        "triggerContract2(address)", receiveAddress, false,
        0, 10000000L, contract014Address, contract014Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contract3AccountInfo = PublicMethed.queryAccount(contractAddress3, blockingStubFull);
    final Long contract3AfterBalance = contract3AccountInfo.getBalance();
    receiverAccountInfo = PublicMethed.queryAccount(receiverAddress, blockingStubFull);
    receiverAfterBalance = receiverAccountInfo.getBalance();
    logger.info("after receiver balance is " + Long.toString(receiverAfterBalance));
    logger.info("after contract3 balance is " + Long.toString(contract3AfterBalance));
    contract1AccountInfo = PublicMethed.queryAccount(contractAddress1, blockingStubFull);
    contract1AfterBalance = contract1AccountInfo.getBalance();

    Assert.assertTrue(receiverAfterBalance - receiverBeforeBalance == 5);
    Assert.assertTrue(contract3BeforeBalance - contract3AfterBalance == 0);
    Assert.assertTrue(contract1BeforeBalance - contract1AfterBalance == 5);


  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


