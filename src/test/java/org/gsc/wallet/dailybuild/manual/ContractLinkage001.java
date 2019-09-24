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
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractLinkage001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage001Address = ecKey1.getAddress();
  String linkage001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
    PublicMethed.printAddress(linkage001Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = true, description = "Deploy contract with valid or invalid value")
  public void deployContentValue() {
    Assert.assertTrue(PublicMethed.sendcoin(linkage001Address, 20000000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeNetLimit = resourceInfo.getNetLimit();
    Long beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeCpuLimit = resourceInfo.getCpuLimit();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuLimit:" + beforeCpuLimit);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeFreeNetLimit:" + beforeFreeNetLimit);
    logger.info("beforeNetLimit:" + beforeNetLimit);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    //Value is equal balance,this will be failed.Only use FreeNet,Other not change.
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractLinkage001.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String payableCode = retMap.get("byteCode").toString();
    String payableAbi = retMap.get("abi").toString();
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account accountGet = PublicMethed.queryAccount(linkage001Key, blockingStubFull);
    Long accountBalance = accountGet.getBalance();
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, payableAbi,
        payableCode, "", maxFeeLimit, accountBalance, 100, null,
        linkage001Key, linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();
    Long fee = infoById.get().getFee();
    Long cpuFee = infoById.get().getReceipt().getCpuFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    logger.info("cpuUsageTotal:" + cpuUsageTotal);
    logger.info("fee:" + fee);
    logger.info("cpuFee:" + cpuFee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuLimit = resourceInfoafter.getCpuLimit();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterFreeNetLimit = resourceInfoafter.getFreeNetLimit();
    Long afterNetLimit = resourceInfoafter.getNetLimit();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuLimit:" + afterCpuLimit);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterFreeNetLimit:" + afterFreeNetLimit);
    logger.info("afterNetLimit:" + afterNetLimit);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertTrue(fee == 0);
    Assert.assertTrue(afterNetUsed == 0);
    Assert.assertTrue(afterCpuUsed == 0);
    Assert.assertTrue(afterFreeNetUsed > 0);

    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(linkage001Address, 50000000L,
        5, 1, linkage001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    maxFeeLimit = maxFeeLimit - 50000000L;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance1 = info1.getBalance();
    Long beforeCpuLimit1 = resourceInfo1.getCpuLimit();
    Long beforeCpuUsed1 = resourceInfo1.getCpuUsed();
    Long beforeFreeNetLimit1 = resourceInfo1.getFreeNetLimit();
    Long beforeNetLimit1 = resourceInfo1.getNetLimit();
    Long beforeNetUsed1 = resourceInfo1.getNetUsed();
    Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    logger.info("beforeBalance1:" + beforeBalance1);
    logger.info("beforeCpuLimit1:" + beforeCpuLimit1);
    logger.info("beforeCpuUsed1:" + beforeCpuUsed1);
    logger.info("beforeFreeNetLimit1:" + beforeFreeNetLimit1);
    logger.info("beforeNetLimit1:" + beforeNetLimit1);
    logger.info("beforeNetUsed1:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed1:" + beforeFreeNetUsed1);

    //Value is 1,use BalanceGetCpu,use FreeNet,fee==0.
    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, payableAbi, payableCode,
            "", maxFeeLimit, 1L, 100, null, linkage001Key,
            linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();
    Long fee1 = infoById1.get().getFee();
    Long cpuFee1 = infoById1.get().getReceipt().getCpuFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);
    logger.info("fee1:" + fee1);
    logger.info("cpuFee1:" + cpuFee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    Account infoafter1 = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuLimit1 = resourceInfoafter1.getCpuLimit();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterFreeNetLimit1 = resourceInfoafter1.getFreeNetLimit();
    Long afterNetLimit1 = resourceInfoafter1.getNetLimit();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterCpuLimit1:" + afterCpuLimit1);
    logger.info("afterCpuUsed1:" + afterCpuUsed1);
    logger.info("afterFreeNetLimit1:" + afterFreeNetLimit1);
    logger.info("afterNetLimit1:" + afterNetLimit1);
    logger.info("afterNetUsed1:" + afterNetUsed1);
    logger.info("afterFreeNetUsed1:" + afterFreeNetUsed1);

    Assert.assertTrue(beforeBalance1 - fee1 - 1L == afterBalance1);
    byte[] contractAddress = infoById1.get().getContractAddress().toByteArray();
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    Assert.assertTrue(account.getBalance() == 1L);
    Assert.assertTrue(afterNetUsed1 == 0);
    Assert.assertTrue(afterCpuUsed1 > 0);
    Assert.assertTrue(afterFreeNetUsed1 > 0);

    //Value is account all balance plus 1. balance is not sufficient,Nothing changde.
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance2 = info2.getBalance();
    Long beforeCpuLimit2 = resourceInfo2.getCpuLimit();
    Long beforeCpuUsed2 = resourceInfo2.getCpuUsed();
    Long beforeFreeNetLimit2 = resourceInfo2.getFreeNetLimit();
    Long beforeNetLimit2 = resourceInfo2.getNetLimit();
    Long beforeNetUsed2 = resourceInfo2.getNetUsed();
    Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    logger.info("beforeBalance2:" + beforeBalance2);
    logger.info("beforeCpuLimit2:" + beforeCpuLimit2);
    logger.info("beforeCpuUsed2:" + beforeCpuUsed2);
    logger.info("beforeFreeNetLimit2:" + beforeFreeNetLimit2);
    logger.info("beforeNetLimit2:" + beforeNetLimit2);
    logger.info("beforeNetUsed2:" + beforeNetUsed2);
    logger.info("beforeFreeNetUsed2:" + beforeFreeNetUsed2);

    account = PublicMethed.queryAccount(linkage001Key, blockingStubFull);
    Long valueBalance = account.getBalance();
    contractAddress = PublicMethed.deployContract(contractName, payableAbi, payableCode, "",
        maxFeeLimit, valueBalance + 1, 100, null, linkage001Key,
        linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(contractAddress == null);
    Account infoafter2 = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterCpuLimit2 = resourceInfoafter2.getCpuLimit();
    Long afterCpuUsed2 = resourceInfoafter2.getCpuUsed();
    Long afterFreeNetLimit2 = resourceInfoafter2.getFreeNetLimit();
    Long afterNetLimit2 = resourceInfoafter2.getNetLimit();
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    logger.info("afterBalance2:" + afterBalance2);
    logger.info("afterCpuLimit2:" + afterCpuLimit2);
    logger.info("afterCpuUsed2:" + afterCpuUsed2);
    logger.info("afterFreeNetLimit2:" + afterFreeNetLimit2);
    logger.info("afterNetLimit2:" + afterNetLimit2);
    logger.info("afterNetUsed2:" + afterNetUsed2);
    logger.info("afterFreeNetUsed2:" + afterFreeNetUsed2);
    Assert.assertTrue(afterNetUsed2 == 0);
    Assert.assertTrue(afterCpuUsed2 > 0);
    Assert.assertTrue(afterFreeNetUsed2 > 0);
    Assert.assertEquals(beforeBalance2, afterBalance2);

    //Value is account all balance.use freezeBalanceGetCpu ,freezeBalanceGetNet .Balance ==0
    Assert.assertTrue(PublicMethed.freezeBalance(linkage001Address, 5000000L,
        5, linkage001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfo3 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info3 = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance3 = info3.getBalance();
    Long beforeCpuLimit3 = resourceInfo3.getCpuLimit();
    Long beforeCpuUsed3 = resourceInfo3.getCpuUsed();
    Long beforeFreeNetLimit3 = resourceInfo3.getFreeNetLimit();
    Long beforeNetLimit3 = resourceInfo3.getNetLimit();
    Long beforeNetUsed3 = resourceInfo3.getNetUsed();
    Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
    logger.info("beforeBalance3:" + beforeBalance3);
    logger.info("beforeCpuLimit3:" + beforeCpuLimit3);
    logger.info("beforeCpuUsed3:" + beforeCpuUsed3);
    logger.info("beforeFreeNetLimit3:" + beforeFreeNetLimit3);
    logger.info("beforeNetLimit3:" + beforeNetLimit3);
    logger.info("beforeNetUsed3:" + beforeNetUsed3);
    logger.info("beforeFreeNetUsed3:" + beforeFreeNetUsed3);
    account = PublicMethed.queryAccount(linkage001Key, blockingStubFull);
    valueBalance = account.getBalance();
    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, payableAbi, payableCode,
            "", maxFeeLimit, valueBalance, 100, null, linkage001Key,
            linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    fee = infoById.get().getFee();
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    Account infoafter3 = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance3 = infoafter3.getBalance();
    Long afterCpuLimit3 = resourceInfoafter3.getCpuLimit();
    Long afterCpuUsed3 = resourceInfoafter3.getCpuUsed();
    Long afterFreeNetLimit3 = resourceInfoafter3.getFreeNetLimit();
    Long afterNetLimit3 = resourceInfoafter3.getNetLimit();
    Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
    Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    logger.info("afterBalance3:" + afterBalance3);
    logger.info("afterCpuLimit3:" + afterCpuLimit3);
    logger.info("afterCpuUsed3:" + afterCpuUsed3);
    logger.info("afterFreeNetLimit3:" + afterFreeNetLimit3);
    logger.info("afterNetLimit3:" + afterNetLimit3);
    logger.info("afterNetUsed3:" + afterNetUsed3);
    logger.info("afterFreeNetUsed3:" + afterFreeNetUsed3);

    Assert.assertTrue(afterNetUsed3 > 0);
    Assert.assertTrue(afterCpuUsed3 > 0);
    Assert.assertTrue(afterFreeNetUsed3 > 0);
    Assert.assertTrue(beforeBalance2 - fee == afterBalance2);
    Assert.assertTrue(afterBalance3 == 0);
    Assert.assertTrue(PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance() == valueBalance);
    PublicMethed
        .unFreezeBalance(linkage001Address, linkage001Key, 1,
            linkage001Address, blockingStubFull);
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


