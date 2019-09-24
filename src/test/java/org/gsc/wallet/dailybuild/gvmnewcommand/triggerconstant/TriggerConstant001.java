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

package org.gsc.wallet.dailybuild.gvmnewcommand.triggerconstant;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TriggerConstant001 {

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

  byte[] contractAddressNoAbi = null;
  byte[] contractAddressWithAbi = null;

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

    {
      Assert.assertTrue(PublicMethed
          .sendcoin(contractExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
              blockingStubFull));
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      String filePath = "src/test/resources/soliditycode_v0.5.4/TriggerConstant001.sol";
      String contractName = "testConstantContract";
      HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
      String code = retMap.get("byteCode").toString();
      final String abi = retMap.get("abi").toString();

      contractAddressNoAbi = PublicMethed.deployContract(contractName, "[]", code, "", maxFeeLimit,
          0L, 100, null, contractExcKey,
          contractExcAddress, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract = PublicMethed.getContract(
          contractAddressNoAbi, blockingStubFull);
      Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

      contractAddressWithAbi = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
          0L, 100, null, contractExcKey,
          contractExcAddress, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract2 = PublicMethed.getContract(
          contractAddressWithAbi, blockingStubFull);
      Assert.assertFalse(smartContract2.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract2.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract2.getBytecode().toString().isEmpty());

    }
  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function without ABI")
  public void test01TriggerConstantContract() {

    String txid = "";

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_EXE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("Attempt to call a state modifying opcode inside STATICCALL"));


  }

  @Test(enabled = true, description = "TriggerConstantContract a non-payable function without ABI")
  public void test02TriggerConstantContract() {

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testNoPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_EXE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("Attempt to call a state modifying opcode inside STATICCALL"));


  }

  @Test(enabled = true, description = "TriggerConstantContract a view function without ABI")
  public void test03TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testView()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function without ABI")
  public void test04TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testPure()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));


  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function with ABI")
  public void test05TriggerConstantContract() {

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("CONTRACT_EXE_ERROR"));
    Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
        containsString("Attempt to call a state modifying opcode inside STATICCALL"));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true, description = "TriggerConstantContract a non-payable function with ABI")
  public void test06TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testNoPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("CONTRACT_EXE_ERROR"));
    Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
        containsString("Attempt to call a state modifying opcode inside STATICCALL"));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true, description = "TriggerConstantContract a view function with ABI")
  public void test07TriggerConstantContract() {

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testView()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));


  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function with ABI")
  public void test08TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testPure()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));


  }

  @Test(enabled = true, description = "TriggerContract a payable function without ABI")
  public void test09TriggerContract() {
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

    txid = PublicMethed
        .triggerContract(contractAddressNoAbi,
            "testPayable()", "#", false,
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
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a non-payable function without ABI")
  public void test10TriggerContract() {
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

    txid = PublicMethed
        .triggerContract(contractAddressNoAbi,
            "testNoPayable()", "#", false,
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
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a view function without ABI")
  public void test11TriggerContract() {

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

    txid = PublicMethed
        .triggerContract(contractAddressNoAbi,
            "testView()", "#", false,
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
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a pure function without ABI")
  public void test12TriggerContract() {

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

    txid = PublicMethed
        .triggerContract(contractAddressNoAbi,
            "testPure()", "#", false,
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
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a pure function with ABI")
  public void test18TriggerContract() {

    TransactionExtention transactionExtention = PublicMethed
        .triggerContractForExtention(contractAddressWithAbi,
            "testPure()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

  }

  @Test(enabled = true, description = "TriggerContract a payable function with ABI")
  public void test19TriggerContract() {

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
    txid = PublicMethed
        .triggerContract(contractAddressWithAbi,
            "testPayable()", "#", false,
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
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);

  }

  @Test(enabled = true, description = "TriggerContract a non-payable function with ABI")
  public void test20TriggerContract() {
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
    txid = PublicMethed
        .triggerContract(contractAddressNoAbi,
            "testNoPayable()", "#", false,
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
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);

  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI")
  public void test21TriggerContract() {

    TransactionExtention transactionExtention = PublicMethed
        .triggerContractForExtention(contractAddressWithAbi,
            "testView()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

  }

  @Test(enabled = true, description = "TriggerConstantContract a view method with ABI ,method has "
      + "revert()")
  public void test24TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testView2()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert
        .assertThat(transaction.getRet(0).getRet().toString(),
            containsString("FAILED"));
    Assert
        .assertThat(ByteArray
                .toStr(transactionExtention.getResult().getMessage().toByteArray()),
            containsString("REVERT opcode executed"));


  }

  @Test(enabled = true, description = "TriggerContract a view method with ABI ,method has "
      + "revert()")
  public void test25TriggerContract() {

    TransactionExtention transactionExtention = PublicMethed
        .triggerContractForExtention(contractAddressWithAbi,
            "testView2()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(),
        containsString("FAILED"));
    Assert.assertThat(ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));


  }

  @Test(enabled = true, description = "TriggerConstantContract a view method without ABI,method has"
      + "revert()")
  public void testTriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testView2()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert
        .assertThat(transaction.getRet(0).getRet().toString(),
            containsString("FAILED"));
    Assert
        .assertThat(ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
            containsString("REVERT opcode executed"));


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
