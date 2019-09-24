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
public class TransferFailed006 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private final Long maxFeeLimit = Configuration.getByPath("testng.cong")
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

  byte[] contractAddress = null;
  byte[] contractAddress1 = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] accountExcAddress = ecKey1.getAddress();
  String accountExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(accountExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = false, description = "Deploy contract for trigger")
  public void deployContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(accountExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode_v0.5.4/TransferFailed006.sol";
    String contractName = "CpuOfTransferFailedTest";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    String Txid1 = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(Txid1, blockingStubFull);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    Assert.assertEquals(0, infoById.get().getResultValue());

    filePath = "src/test/resources/soliditycode_v0.5.4/TransferFailed006.sol";
    contractName = "Caller";
    retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abi").toString();

    Txid1 = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(Txid1, blockingStubFull);
    contractAddress1 = infoById.get().getContractAddress().toByteArray();
    logger.info("caller address : " + Base58.encode58Check(contractAddress1));
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = false, description = "TransferFailed for create")
  public void triggerContract() {
    Account info = null;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(accountExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(accountExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    Assert.assertTrue(PublicMethed
        .sendcoin(contractAddress, 1000100L, accountExcAddress, accountExcKey, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(contractAddress1, 1, accountExcAddress, accountExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info(
        "contractAddress balance before: " + PublicMethed
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    logger.info(
        "callerAddress balance before: " + PublicMethed
            .queryAccount(contractAddress1, blockingStubFull)
            .getBalance());
    long paramValue = 1000000;
    String param = "\"" + paramValue + "\"";

    String triggerTxid = PublicMethed.triggerContract(contractAddress,
        "testCreateGscInsufficientBalance(uint256)", param, false, 0L,
        maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(infoById.get().getReceipt().getResult() + "");

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

    logger.info(
        "contractAddress balance before: " + PublicMethed
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    logger.info(
        "callerAddress balance before: " + PublicMethed
            .queryAccount(contractAddress1, blockingStubFull)
            .getBalance());
    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertFalse(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertEquals(100L,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsageTotal() < 10000000);

    triggerTxid = PublicMethed.triggerContract(contractAddress,
        "testCreateGscInsufficientBalance(uint256)", param, false, 0L,
        maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);
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

    logger.info(
        "contractAddress balance before: " + PublicMethed
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    logger.info(
        "callerAddress balance before: " + PublicMethed
            .queryAccount(contractAddress1, blockingStubFull)
            .getBalance());

    Assert.assertEquals(infoById.get().getResultValue(), 1);
    Assert.assertEquals(infoById.get().getResMessage().toStringUtf8(), "REVERT opcode executed");
    Assert.assertEquals(100L,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1L,
        PublicMethed.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsageTotal() < 10000000);


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
