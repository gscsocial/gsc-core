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

package org.gsc.wallet.contract.linkage;

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
      .getStringList("fullnode.ip.list").get(0);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage007Address = ecKey1.getAddress();
  String linkage007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    PublicMethed.printAddress(linkage007Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = true)
  public void testRangeOfFeeLimit() {

    //Now the feelimit range is 0-1000000000,including 0 and 1000000000
    Assert.assertTrue(PublicMethed.sendcoin(linkage007Address, 2000000000L, fromAddress,
        testKey002, blockingStubFull));

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info;
    info = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
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
    //When the feelimit is large, the deploy will be failed,No used everything.

    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractLinkage002.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    String txid;
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit + 1, 0L, 100, null, linkage007Key,
        linkage007Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage007Address,
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
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertTrue(afterCpuUsed == 0);
    Assert.assertTrue(afterNetUsed == 0);
    Assert.assertTrue(afterFreeNetUsed == 0);

    Assert.assertTrue(txid == null);
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
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
    //When the feelimit is 0, the deploy will be failed.Only use FreeNet,balance not change.
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", 0L, 0L, 100, null, linkage007Key,
        linkage007Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account infoafter1 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(linkage007Address,
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
    Assert.assertEquals(beforeBalance1, afterBalance1);
    Assert.assertTrue(afterFreeNetUsed1 > 0);
    Assert.assertTrue(afterNetUsed1 == 0);
    Assert.assertTrue(afterCpuUsed1 == 0);
    Optional<TransactionInfo> infoById;

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);

    //Deploy the contract.success.use FreeNet,CpuFee.balcne change
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
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
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 100, null, linkage007Key,
        linkage007Address, blockingStubFull);
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
    Account infoafter2 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage007Address,
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
    logger.info("---------------:");
    Assert.assertTrue((beforeBalance2 - fee2) == afterBalance2);
    Assert.assertTrue(afterCpuUsed2 == 0);
    Assert.assertTrue(afterFreeNetUsed2 > beforeFreeNetUsed2);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    contractAddress = infoById2.get().getContractAddress().toByteArray();

    //When the feelimit is large, the trigger will be failed.Only use FreeNetUsed,Balance not change
    AccountResourceMessage resourceInfo3 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info3 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
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
    String num = "4" + "," + "2";
    txid = PublicMethed.triggerContract(contractAddress,
        "divideIHaveArgsReturn(int256,int256)", num, false,
        1000, maxFeeLimit + 1, linkage007Address, linkage007Key, blockingStubFull);
    Account infoafter3 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(linkage007Address,
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
    logger.info("---------------:");
    Assert.assertTrue(txid == null);
    Assert.assertEquals(beforeBalance3, afterBalance3);
    Assert.assertTrue(afterFreeNetUsed3 > beforeNetUsed3);
    Assert.assertTrue(afterNetUsed3 == 0);
    Assert.assertTrue(afterCpuUsed3 == 0);
    //When the feelimit is 0, the trigger will be failed.Only use FreeNetUsed,Balance not change
    AccountResourceMessage resourceInfo4 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull);
    Account info4 = PublicMethed.queryAccount(linkage007Address, blockingStubFull);
    Long beforeBalance4 = info4.getBalance();
    Long beforeCpuLimit4 = resourceInfo4.getCpuLimit();
    Long beforeCpuUsed4 = resourceInfo4.getCpuUsed();
    Long beforeFreeNetLimit4 = resourceInfo4.getFreeNetLimit();
    Long beforeNetLimit4 = resourceInfo4.getNetLimit();
    Long beforeNetUsed4 = resourceInfo4.getNetUsed();
    Long beforeFreeNetUsed4 = resourceInfo4.getFreeNetUsed();
    logger.info("beforeBalance4:" + beforeBalance4);
    logger.info("beforeCpuLimit4:" + beforeCpuLimit4);
    logger.info("beforeCpuUsed4:" + beforeCpuUsed4);
    logger.info("beforeFreeNetLimit4:" + beforeFreeNetLimit4);
    logger.info("beforeNetLimit4:" + beforeNetLimit4);
    logger.info("beforeNetUsed4:" + beforeNetUsed4);
    logger.info("beforeFreeNetUsed4:" + beforeFreeNetUsed4);
    txid = PublicMethed.triggerContract(contractAddress,
        "divideIHaveArgsReturn(int256,int256)", num, false,
        1000, maxFeeLimit + 1, linkage007Address, linkage007Key, blockingStubFull);
    Account infoafter4 = PublicMethed.queryAccount(linkage007Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter4 = PublicMethed.getAccountResource(linkage007Address,
        blockingStubFull1);
    Long afterBalance4 = infoafter4.getBalance();
    Long afterCpuLimit4 = resourceInfoafter4.getCpuLimit();
    Long afterCpuUsed4 = resourceInfoafter4.getCpuUsed();
    Long afterFreeNetLimit4 = resourceInfoafter4.getFreeNetLimit();
    Long afterNetLimit4 = resourceInfoafter4.getNetLimit();
    Long afterNetUsed4 = resourceInfoafter4.getNetUsed();
    Long afterFreeNetUsed4 = resourceInfoafter4.getFreeNetUsed();
    logger.info("afterBalance4:" + afterBalance4);
    logger.info("afterCpuLimit4:" + afterCpuLimit4);
    logger.info("afterCpuUsed4:" + afterCpuUsed4);
    logger.info("afterFreeNetLimit4:" + afterFreeNetLimit4);
    logger.info("afterNetLimit4:" + afterNetLimit4);
    logger.info("afterNetUsed4:" + afterNetUsed4);
    logger.info("afterFreeNetUsed4:" + afterFreeNetUsed4);
    Assert.assertEquals(beforeBalance4, afterBalance4);
    Assert.assertTrue(afterFreeNetUsed4 > beforeNetUsed4);
    Assert.assertTrue(afterNetUsed4 == 0);
    Assert.assertTrue(afterCpuUsed4 == 0);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(Integer.toString(infoById.get().getResultValue()));
    Assert.assertTrue(infoById.get().getFee() == 0);

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


