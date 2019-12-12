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

package org.gsc.wallet.contract.scenario;

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
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractScenario012 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  String receiveAddressParam;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract012Address = ecKey1.getAddress();
  String contract012Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
    PublicMethed.printAddress(contract012Key);
    PublicMethed.printAddress(receiverKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void test1DeployTransactionCoin() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract012Address = ecKey1.getAddress();
    contract012Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(contract012Address, 2000000000L, fromAddress,
        testKey002, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract012Address,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();

    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractScenario012.sol";
    String contractName = "PayTest";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100,
            null, contract012Key, contract012Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    logger.info("cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
  }

  @Test(enabled = true)
  public void test3TriggerTransactionCanNotCreateAccount() {
    ecKey2 = new ECKey(Utils.getRandom());
    receiverAddress = ecKey2.getAddress();
    receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    //Send some Gsc to the contract account.
    Assert.assertTrue(PublicMethed.sendcoin(contractAddress, 1000000000L, toAddress,
        testKey003, blockingStubFull));
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    receiveAddressParam = "\"" + Base58.encode58Check(receiverAddress)
        + "\"";
    //In smart contract, you can't create account
    txid = PublicMethed.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddressParam, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    logger.info("result is " + infoById.get().getResultValue());
    logger.info("cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getCpuFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

  }


  @Test(enabled = true)
  public void test2TriggerTransactionCoin() {
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    receiveAddressParam = "\"" + Base58.encode58Check(fromAddress)
            + "\"";
    //When the contract has no money,transaction coin failed.
    txid = PublicMethed.triggerContract(contractAddress,
            "sendToAddress2(address)", receiveAddressParam, false,
            0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    logger.info("cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getCpuFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());
  }

  @Test(enabled = true)
  public void test4TriggerTransactionCoin() {
    receiveAddressParam = "\"" + Base58.encode58Check(receiverAddress)
            + "\"";
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    //This time, trigger the methed sendToAddress2 is OK.
    Assert.assertTrue(PublicMethed.sendcoin(receiverAddress, 10000000L, toAddress,
            testKey003, blockingStubFull));
    txid = PublicMethed.triggerContract(contractAddress,
            "sendToAddress2(address)", receiveAddressParam, false,
            0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    logger.info("result is " + infoById.get().getResultValue());
    logger.info("cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getCpuFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


