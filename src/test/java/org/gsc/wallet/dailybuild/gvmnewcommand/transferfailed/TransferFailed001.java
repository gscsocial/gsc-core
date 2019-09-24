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

package org.gsc.wallet.dailybuild.gvmnewcommand.transferfailed;

import static org.gsc.protos.Protocol.TransactionInfo.code.FAILED;

import com.google.protobuf.ByteString;
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
import org.gsc.protos.Protocol.Transaction.Result.contractResult;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.WalletClient;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TransferFailed001 {

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

  @Test(enabled = true, description = "Transfer Gsc insufficient balance")
  public void test1TransferGscInsufficientBalance() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    String filePath = "src/test/resources/soliditycode_v0.5.4/TransferFailed001.sol";
    String contractName = "CpuOfTransferFailedTest";
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
    String txid = "";
    String num = "1";
    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferGscInsufficientBalance(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("infoById:" + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, cpuUsageTotal);


  }


  @Test(enabled = true, description = "Transfer balance enough")
  public void test2TransferEnough() {

    Assert.assertTrue(PublicMethed
        .sendcoin(contractAddress, 3000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
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
    String txid = "";
    String num = "1";
    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferGscInsufficientBalance(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    logger.info("infoById" + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee - 1 == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);


  }


  @Test(enabled = true, description = "Transfer Gsc nonexistent target")
  public void test3TransferGscNonexistentTarget() {

    Assert.assertTrue(PublicMethed
        .sendcoin(contractAddress, 1000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));

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
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] nonexistentAddress = ecKey2.getAddress();
    String txid = "";
    String num = "1" + ",\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferGscNonexistentTarget(uint256,address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.TRANSFER_FAILED, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "transfer gsc failed: Validate InternalTransfer error, no ToAccount."
            + " And not allowed to create account in smart contract.",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, cpuUsageTotal);


  }


  @Test(enabled = true, description = "Transfer Gsc to myself")
  public void test4TransferGscSelf() {


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
    String txid = "";
    String num = "1";

    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferGscSelf(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.TRANSFER_FAILED, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "transfer gsc failed: Cannot transfer gsc to yourself.",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, cpuUsageTotal);


  }


  @Test(enabled = true, description = "Transfer gsc nonexistent target and insufficient balance")
  public void test5TransferGscNonexistentTarget() {

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
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] nonexistentAddress = ecKey2.getAddress();
    String txid = "";
    String num = "10000000" + ",\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferGscNonexistentTarget(uint256,address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, cpuUsageTotal);


  }


  @Test(enabled = true, description = "Transfer Gsc to myself and insufficient balance")
  public void test6TransferGscSelf() {


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
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    String txid = "";
    String num = "1000000000";

    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferGscSelf(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeCpuUsed + cpuUsed >= afterCpuUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, cpuUsageTotal);


  }

  @Test(enabled = true, description = "PreCompiled transfertoken with value,"
      + " long.max < value or long.min > value")
  public void test7TransferGrckenPreCompiled() {

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Account info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = "";
    String num = "1";

    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferTokenCompiledLongMax1()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
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

    resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    beforeBalance = info.getBalance();
    beforeCpuUsed = resourceInfo.getCpuUsed();
    beforeNetUsed = resourceInfo.getNetUsed();
    beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertTrue(cpuUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals("REVERT opcode executed", infoById.get().getResMessage().toStringUtf8());

    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferTokenCompiledLongMin1()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    fee = infoById.get().getFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    cpuUsed = infoById.get().getReceipt().getCpuUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    cpuUsageTotal = infoById.get().getReceipt().getCpuUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("cpuUsed:" + cpuUsed);
    logger.info("netFee:" + netFee);
    logger.info("cpuUsageTotal:" + cpuUsageTotal);

    resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    beforeBalance = info.getBalance();
    beforeCpuUsed = resourceInfo.getCpuUsed();
    beforeNetUsed = resourceInfo.getNetUsed();
    beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertTrue(cpuUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals("REVERT opcode executed", infoById.get().getResMessage().toStringUtf8());

  }

  @Test(enabled = false, description = "PreCompiled tokenbalance")
  public void test8TransferGrctoken() {

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    String description = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetDescription");
    String url = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetUrl");

    ByteString assetAccountId = null;
    final long TotalSupply = 10000000L;
    long now = System.currentTimeMillis();
    String tokenName = "testAssetIssue_" + Long.toString(now);

    //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethed.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethed.queryAccount(contractExcAddress, blockingStubFull)
        .getAssetIssuedID();

    String filePath = "src/test/resources/soliditycode_v0.5.4/TransferFailed001.sol";
    String contractName = "CpuOfTransferFailedTest";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.transferAsset(contractAddress,
        assetAccountId.toByteArray(), 100L, contractExcAddress, contractExcKey,
        blockingStubFull));

    Long returnAddressBytesAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("returnAddressBytesAccountCountBefore : " + returnAddressBytesAccountCountBefore);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long testNetAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
    String txid = "";
    String num =
        "\"" + Base58.encode58Check(contractAddress) + "\"," + "\"" + assetAccountId.toStringUtf8()
            + "\"";
    //String num = "\""+Base58.encode58Check(contractAddress) +"\","+ "\"" + -1 + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferTokenTest(address,uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(cpuUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals(100, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));


  }

  @Test(enabled = true, description = "PreCompiled address(0x1) query tokenbalance")
  public void test9TransferGrctoken() {
    //address: 410000000000000000000000000000000000000001
    String addressx = "GSCZ6Kr6ZhrXrq38JQBawcrEyoVRLa7vtaz2";
    byte[] addressxx = WalletClient.decodeFromBase58Check(addressx);

    Assert.assertTrue(PublicMethed
        .sendcoin(addressxx, 1000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    String description = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetDescription");
    String url = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetUrl");

    ByteString assetAccountId = null;
    final long TotalSupply = 10000000L;
    long now = System.currentTimeMillis();
    String tokenName = "testAssetIssue_" + Long.toString(now);

    //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethed.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethed.queryAccount(contractExcAddress, blockingStubFull)
        .getAssetIssuedID();

    Assert.assertTrue(PublicMethed.transferAsset(addressxx,
        assetAccountId.toByteArray(), 100L, contractExcAddress, contractExcKey,
        blockingStubFull));

    Long returnAddressBytesAccountCountBefore = PublicMethed
        .getAssetIssueValue(addressxx, assetAccountId, blockingStubFull);
    logger.info("returnAddressBytesAccountCountBefore : " + returnAddressBytesAccountCountBefore);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long testNetAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountBefore = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
    String txid = "";
    //String num = "\""+Base58.encode58Check(contractAddress)
    // +"\","+ "\"" + assetAccountId.toStringUtf8() + "\"";
    String num = "\"" + assetAccountId.toStringUtf8() + "\"";
    //String num = "\""+Base58.encode58Check(contractAddress) +"\","+ "\"" + -1 + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
        "testTransferTokenCompiledTokenId(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long testNetAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
    Long contractAccountCountAfter = PublicMethed
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(cpuUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals(100, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
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
