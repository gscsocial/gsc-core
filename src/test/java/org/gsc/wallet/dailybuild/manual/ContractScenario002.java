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
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractScenario002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract002Address = ecKey1.getAddress();
  String contract002Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
    PublicMethed.printAddress(contract002Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = true, description = "Deploy contract with java-gsc support interface")
  public void deployGSCNative() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] contract002Address = ecKey1.getAddress();
    String contract002Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(contract002Address, 500000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(contract002Address, 1000000L,
        5, 1, contract002Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract002Address,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Long balanceBefore = PublicMethed.queryAccount(contract002Key, blockingStubFull).getBalance();

    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));

    String contractName = "GscNative";
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractScenario002.sol";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, contract002Key, contract002Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);

    logger.info(txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    com.google.protobuf.ByteString contractAddress = infoById.get().getContractAddress();
    SmartContract smartContract = PublicMethed
        .getContract(contractAddress.toByteArray(), blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed.getAccountResource(contract002Address, blockingStubFull1);
    cpuLimit = accountResource.getCpuLimit();
    cpuUsage = accountResource.getCpuUsed();
    Long balanceAfter = PublicMethed.queryAccount(contract002Address, blockingStubFull1)
        .getBalance();

    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    logger.info("after balance is " + Long.toString(balanceAfter));
    logger.info("transaction fee is " + Long.toString(infoById.get().getFee()));

    Assert.assertTrue(cpuUsage > 0);
    Assert.assertTrue(balanceBefore == balanceAfter + infoById.get().getFee());
    PublicMethed.unFreezeBalance(contract002Address, contract002Key, 1,
        contract002Address, blockingStubFull);

  }

  @Test(enabled = true, description = "Get smart contract with invalid address")
  public void getContractWithInvalidAddress() {
    byte[] contractAddress = contract002Address;
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    logger.info(smartContract.getAbi().toString());
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
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


