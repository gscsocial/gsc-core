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

package org.gsc.wallet.dailybuild.gvmnewcommand.create2;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class Create2Test020 {

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
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


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
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
  }

  @Test(enabled = true, description = "Deploy Factory contract, create2 with salt type : grcToken")
  public void testTriggerContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode_v0.5.4/create2contract22.sol";
    String contractName = "Factory";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String contractName1 = "TestConstract";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + "," + 1000001;
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy(bytes,grcToken)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterCpuUsed:" + afterCpuUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterCpuUsed + cpuUsed1 >= afterCpuUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);

  }


  @Test(enabled = true, description = "Deploy Factory contract, create2 with salt type : uint8")
  public void testTriggerContract1() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String contractName1 = "TestConstract";
    String filePath = "src/test/resources/soliditycode_v0.5.4/create2contract22.sol";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + "," + 1000001;
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy1(bytes,uint8)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterCpuUsed:" + afterCpuUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterCpuUsed + cpuUsed1 >= afterCpuUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }


  @Test(enabled = true, description = "Deploy Factory contract, create2 with salt type : address")
  public void testTriggerContract2() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String contractName1 = "TestConstract";
    String filePath = "src/test/resources/soliditycode_v0.5.4/create2contract22.sol";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + ",\"" + Base58.encode58Check(contractExcAddress) + "\"";
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy2(bytes,address)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterCpuUsed:" + afterCpuUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterCpuUsed + cpuUsed1 >= afterCpuUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }


  @Test(enabled = true, description = "Deploy Factory contract, create2 with salt type : string")
  public void testTriggerContract3() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String contractName1 = "TestConstract";
    String filePath = "src/test/resources/soliditycode_v0.5.4/create2contract22.sol";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + ",\"" + Base58.encode58Check(contractExcAddress) + "\"";
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy3(bytes,string)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterCpuUsed:" + afterCpuUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterCpuUsed + cpuUsed1 >= afterCpuUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }


  @Test(enabled = true, description = "TriggerContract a constant function created by create2")
  public void testTriggerContract4() {
    Account info;
    Assert.assertTrue(PublicMethed
        .sendcoin(contractAddress, 1000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String contractName1 = "TestConstract1";
    String filePath = "src/test/resources/soliditycode_v0.5.4/create2contract22.sol";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + ",\"" + Base58.encode58Check(contractAddress) + "\"";
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy3(bytes,string)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterCpuUsed:" + afterCpuUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterCpuUsed + cpuUsed1 >= afterCpuUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(2 == returnnumber);

    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy3(bytes,string)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() != 0);
    Assert
        .assertThat(ByteArray
                .toStr(infoById2.get().getResMessage().toByteArray()),
            containsString("Not enough cpu"));
  }


  @Test(enabled = true, description = "TriggerContract a constant function created by create2")
  public void testTriggerContract5() {

    Account info;
    Assert.assertTrue(PublicMethed
        .sendcoin(contractAddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String contractName1 = "TestConstract2";
    String filePath = "src/test/resources/soliditycode_v0.5.4/create2contract22.sol";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + ",\"" + Base58.encode58Check(contractAddress) + "\"";
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy3(bytes,string)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterCpuUsed:" + afterCpuUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterCpuUsed + cpuUsed1 >= afterCpuUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(3 == returnnumber);
    num = "\"" + code1 + "\"" + ",\"" + Base58.encode58Check(testNetAccountAddress) + "\"";

    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy3(bytes,string)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() != 0);
    Assert
        .assertThat(ByteArray
                .toStr(infoById2.get().getResMessage().toByteArray()),
            containsString("Not enough cpu"));
  }


  @Test(enabled = true, description = "TriggerContract a constant function created by create2")
  public void testTriggerContract6() {

    Account info;
    Assert.assertTrue(PublicMethed
        .sendcoin(contractAddress, 1000000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String contractName1 = "TestConstract3";
    String filePath = "src/test/resources/soliditycode_v0.5.4/create2contract22.sol";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    String txid = "";
    String num = "\"" + code1 + "\"" + ",\"" + Base58.encode58Check(contractAddress) + "\"";
    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy3(bytes,string)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long cpuUsed = infoById.get().getReceipt().getCpuUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    byte[] returnAddressBytes = infoById.get().getInternalTransactions(0).getTransferToAddress()
        .toByteArray();
    String returnAddress = Base58.encode58Check(returnAddressBytes);
    logger.info("returnAddress:" + returnAddress);
    txid = PublicMethed
        .triggerContract(returnAddressBytes,
            "i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee1 = infoById1.get().getFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long cpuUsed1 = infoById1.get().getReceipt().getCpuUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    long cpuUsageTotal1 = infoById1.get().getReceipt().getCpuUsageTotal();

    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("cpuUsed1:" + cpuUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("cpuUsageTotal1:" + cpuUsageTotal1);

    Account infoafter1 = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterCpuUsed1 = resourceInfoafter1.getCpuUsed();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterCpuUsed:" + afterCpuUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance1 + fee1 == afterBalance);
    Assert.assertTrue(afterCpuUsed + cpuUsed1 >= afterCpuUsed1);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(4 == returnnumber);
    String fakeAddress = "FFFFFFF";

    num = "\"" + code1 + "\"" + ",\"" + fakeAddress + "\"";

    txid = PublicMethed
        .triggerContract(contractAddress,
            "deploy3(bytes,string)", num, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertTrue(infoById2.get().getResultValue() != 0);
    Assert
        .assertThat(ByteArray
                .toStr(infoById2.get().getResMessage().toByteArray()),
            containsString("Not enough cpu"));
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


}
