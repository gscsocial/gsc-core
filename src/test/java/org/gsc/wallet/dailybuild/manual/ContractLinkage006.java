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
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractLinkage006 {

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey003);

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

  String contractName;
  String code;
  String abi;
  byte[] contractAddress;
  String txid;
  Optional<TransactionInfo> infoById;
  String initParmes;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage006Address = ecKey1.getAddress();
  String linkage006Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] linkage006Address2 = ecKey2.getAddress();
  String linkage006Key2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
    PublicMethed.printAddress(linkage006Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Deploy contract with stack function")
  public void teststackOutByContract() {

    Assert.assertTrue(PublicMethed.sendcoin(linkage006Address, 20000000000L, fromAddress,
        testKey003, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalance(linkage006Address, 1000000L,
        5, linkage006Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(linkage006Address, 1000000L,
        5, 1, linkage006Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage006Address,
        blockingStubFull);
    Account info;
    info = PublicMethed.queryAccount(linkage006Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuLimit = resourceInfo.getCpuLimit();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
    Long beforeNetLimit = resourceInfo.getNetLimit();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuLimit:" + beforeCpuLimit);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeFreeNetLimit:" + beforeFreeNetLimit);
    logger.info("beforeNetLimit:" + beforeNetLimit);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractLinkage006.sol";
    String contractName = "AA";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    //success ,balnace change.use CpuUsed and NetUsed
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 1000L, 100, null, linkage006Key,
        linkage006Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid is " + txid);
    contractAddress = infoById.get().getContractAddress().toByteArray();
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
    Account infoafter = PublicMethed.queryAccount(linkage006Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage006Address,
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

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue((beforeBalance - fee - 1000L) == afterBalance);
    Assert.assertTrue((beforeNetUsed + netUsed) >= afterNetUsed);
    Assert.assertTrue((beforeCpuUsed + cpuUsed) >= afterCpuUsed);
    PublicMethed.unFreezeBalance(linkage006Address, linkage006Key, 1,
        null, blockingStubFull);
  }

  @Test(enabled = true, description = "Boundary value for contract stack(63 is the largest level)")
  public void teststackOutByContract1() {
    Assert.assertTrue(PublicMethed.sendcoin(linkage006Address2, 20000000000L, fromAddress,
        testKey003, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalance(linkage006Address2, 1000000L,
        5, linkage006Key2, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(linkage006Address2, 1000000L,
        5, 1, linkage006Key2, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage006Address2,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull);
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

    //success ,balance change.use CpuUsed and NetUsed
    initParmes = "\"" + Base58.encode58Check(fromAddress) + "\",\"63\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)", initParmes, false,
        0, 100000000L, linkage006Address2, linkage006Key2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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
    Account infoafter1 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed
        .getAccountResource(linkage006Address2,
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
    logger.info("---------------:");
    Assert.assertTrue((beforeBalance1 - fee1) == afterBalance1);
    Assert.assertTrue(afterNetUsed1 > beforeNetUsed1);
    Assert.assertTrue((beforeCpuUsed1 + cpuUsed1) >= afterCpuUsed1);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Boundary value for contract stack"
      + "(Trigger 64 level can't success)")
  public void teststackOutByContract2() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    initParmes = "\"" + Base58.encode58Check(fromAddress) + "\",\"64\"";
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage006Address2,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull);
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
    //failed ,use CpuUsed and NetUsed
    txid = PublicMethed.triggerContract(contractAddress,
        "init(address,uint256)", initParmes, false,
        1000, 100000000L, linkage006Address2, linkage006Key2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long cpuUsageTotal2 = infoById2.get().getReceipt().getCpuUsageTotal();
    Long fee2 = infoById2.get().getFee();
    Long cpuFee2 = infoById2.get().getReceipt().getCpuFee();
    Long netUsed2 = infoById2.get().getReceipt().getNetUsage();
    Long cpuUsed2 = infoById2.get().getReceipt().getCpuUsage();
    Long netFee2 = infoById2.get().getReceipt().getNetFee();
    logger.info("cpuUsageTotal2:" + cpuUsageTotal2);
    logger.info("fee2:" + fee2);
    logger.info("cpuFee2:" + cpuFee2);
    logger.info("netUsed2:" + netUsed2);
    logger.info("cpuUsed2:" + cpuUsed2);
    logger.info("netFee2:" + netFee2);

    Account infoafter2 = PublicMethed.queryAccount(linkage006Address2, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage006Address2,
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

    Assert.assertTrue((beforeBalance2 - fee2) == afterBalance2);
    Assert.assertTrue((beforeCpuUsed2 + cpuUsed2) >= afterCpuUsed2);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    PublicMethed.unFreezeBalance(linkage006Address2, linkage006Key2, 1,
        linkage006Address2, blockingStubFull);
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


