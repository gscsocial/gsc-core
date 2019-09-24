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

import static org.hamcrest.core.StringContains.containsString;

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
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractUnknownException {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelConfirmed = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] grammarAddress2 = ecKey2.getAddress();
  String testKeyForGrammarAddress2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] grammarAddress3 = ecKey3.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] grammarAddress4 = ecKey4.getAddress();
  String testKeyForGrammarAddress4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

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
    PublicMethed.printAddress(testKeyForGrammarAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = true, description = "trigger selfdestruct method")
  public void testGrammar001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(grammarAddress, 204800000,
        5, 1, testKeyForGrammarAddress, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforecpuLimit = resourceInfo.getCpuLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforecpuLimit:" + beforecpuLimit);
    String filePath = "src/test/resources/soliditycode_v0.5.4/contractUnknownException.sol";
    String contractName = "testA";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final String s = infoById.get().getResMessage().toStringUtf8();
    long fee = infoById.get().getFee();
    long cpuUsage = infoById.get().getReceipt().getCpuUsage();
    long cpuFee = infoById.get().getReceipt().getCpuFee();
    Account infoafter = PublicMethed.queryAccount(grammarAddress, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long afterecpuLimit = resourceInfo.getCpuLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("aftercpuLimit:" + afterecpuLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));

  }

  @Test(enabled = true, description = "trigger revert method")
  public void testGrammar002() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress2, 100000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(grammarAddress2, 10000000L,
        5, 1, testKeyForGrammarAddress2, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress2,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress2, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforecpuLimit = resourceInfo.getCpuLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforecpuLimit:" + beforecpuLimit);
    String filePath = "src/test/resources/soliditycode_v0.5.4/contractUnknownException.sol";
    String contractName = "testB";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress2,
            grammarAddress2, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final long fee = infoById.get().getFee();
    final long cpuUsage = infoById.get().getReceipt().getCpuUsage();
    final long cpuFee = infoById.get().getReceipt().getCpuFee();

    final String s = infoById.get().getResMessage().toStringUtf8();

    Account infoafter = PublicMethed.queryAccount(grammarAddress2, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress2,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long afterecpuLimit = resourceInfo.getCpuLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("aftercpuLimit:" + afterecpuLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    Assert.assertFalse(cpuFee == 1000000000);

    Assert.assertTrue(beforeBalance - fee == afterBalance);

  }

  @Test(enabled = true, description = "trigger assert method")
  public void testGrammar003() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress3, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(grammarAddress3, 1000000000L,
        5, 1, testKeyForGrammarAddress3, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress3,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress3, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforecpuLimit = resourceInfo.getCpuLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforecpuLimit:" + beforecpuLimit);
    String filePath = "src/test/resources/soliditycode_v0.5.4/contractUnknownException.sol";
    String contractName = "testC";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final long fee = infoById.get().getFee();
    final long cpuUsage = infoById.get().getReceipt().getCpuUsage();
    final long cpuFee = infoById.get().getReceipt().getCpuFee();
    String s = infoById.get().getResMessage().toStringUtf8();
    Account infoafter = PublicMethed.queryAccount(grammarAddress3, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress3,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long afterecpuLimit = resourceInfo.getCpuLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("aftercpuLimit:" + afterecpuLimit);
    logger.info("s:" + s);
    Assert.assertThat(s, containsString("Not enough cpu for"));

    Assert.assertTrue(beforeBalance - fee == afterBalance);

  }


  @Test(enabled = true, description = "trigger require method")
  public void testGrammar004() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress4, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(grammarAddress4, 100000000L,
        5, 1, testKeyForGrammarAddress4, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(grammarAddress4,
        blockingStubFull);
    info = PublicMethed.queryAccount(grammarAddress4, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforecpuLimit = resourceInfo.getCpuLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforecpuLimit:" + beforecpuLimit);
    String filePath = "src/test/resources/soliditycode_v0.5.4/contractUnknownException.sol";
    String contractName = "testD";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress4,
            grammarAddress4, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    final String s = infoById.get().getResMessage().toStringUtf8();
    final long fee = infoById.get().getFee();
    long cpuUsage = infoById.get().getReceipt().getCpuUsage();
    final long cpuFee = infoById.get().getReceipt().getCpuFee();

    Account infoafter = PublicMethed.queryAccount(grammarAddress4, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(grammarAddress4,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterNetUsed = resourceInfo.getNetUsed();
    Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long afterecpuLimit = resourceInfo.getCpuLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("aftercpuLimit:" + afterecpuLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    Assert.assertTrue(beforeBalance - fee == afterBalance);
    Assert.assertFalse(cpuFee == 1000000000);


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
