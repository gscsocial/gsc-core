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
import org.springframework.util.StringUtils;
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
public class ContractScenario011 {

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


  String kittyCoreAddressAndCut = "";
  byte[] kittyCoreContractAddress = null;
  byte[] saleClockAuctionContractAddress = null;
  byte[] siringClockAuctionContractAddress = null;
  byte[] geneScienceInterfaceContractAddress = null;
  Integer consumeUserResourcePercent = 50;
  String txid = "";
  Optional<TransactionInfo> infoById = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] deployAddress = ecKey1.getAddress();
  String deployKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] triggerAddress = ecKey2.getAddress();
  String triggerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
    PublicMethed.printAddress(deployKey);
    PublicMethed.printAddress(triggerKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(deployAddress, 50000000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed.sendcoin(triggerAddress, 50000000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Kitty Core\"")
  public void deployErc721KittyCore() {
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(deployAddress, 100000000L,
        5, 1, deployKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Assert.assertTrue(PublicMethed.freezeBalance(deployAddress, 100000000L, 5,
        deployKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Assert.assertTrue(PublicMethed.freezeBalance(triggerAddress, 100000000L, 5,
        triggerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Account account = PublicMethed.queryAccount(deployAddress, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "KittyCore";
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractScenario011.sol";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    logger.info("Kitty Core");
    kittyCoreContractAddress = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
        deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(kittyCoreContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));

    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    cpuUsage = accountResource.getCpuUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress));
    logger.info(ByteArray.toHexString(kittyCoreContractAddress).substring(2));

    kittyCoreAddressAndCut = "000000000000000000000000" + ByteArray
        .toHexString(kittyCoreContractAddress).substring(2);
    kittyCoreAddressAndCut = kittyCoreAddressAndCut + "0000000000000000000000000000000000000000000"
        + "000000000000000000100";
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Sale Clock Auction\"")
  public void deploySaleClockAuction() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "SaleClockAuction";
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractScenario011.sol";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    logger.info("Sale Clock Auction");
    //saleClockAuctionContractAddress;
    String data = "\"" + Base58.encode58Check(kittyCoreContractAddress) + "\"," + 100;
    String deplTxid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address,uint256)",
            data, "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey, deployAddress,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(deplTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);

    saleClockAuctionContractAddress = info.get().getContractAddress().toByteArray();
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(saleClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    cpuUsage = accountResource.getCpuUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));

    String triggerTxid = PublicMethed
        .triggerContract(saleClockAuctionContractAddress, "isSaleClockAuction()", "#", false, 0,
            maxFeeLimit, deployAddress, deployKey, blockingStubFull);
    Optional<TransactionInfo> inFoByid = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Ttttt " + triggerTxid);
    Assert.assertTrue(inFoByid.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Siring Clock Auction\"")
  public void deploySiringClockAuction() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "SiringClockAuction";
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractScenario011.sol";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    String data = "\"" + Base58.encode58Check(kittyCoreContractAddress) + "\"," + 100;
    String siringClockAuctionContractAddressTxid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address,uint256)",
            data,
            "", maxFeeLimit, 0L, consumeUserResourcePercent, null, deployKey,
            deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info2 = PublicMethed
        .getTransactionInfoById(siringClockAuctionContractAddressTxid, blockingStubFull);
    siringClockAuctionContractAddress = info2.get().getContractAddress().toByteArray();
    Assert.assertTrue(info2.get().getResultValue() == 0);
    SmartContract smartContract = PublicMethed.getContract(siringClockAuctionContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    cpuUsage = accountResource.getCpuUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  }

  @Test(enabled = true, description = "Deploy Erc721 contract \"Gene Science Interface\"")
  public void deployGeneScienceInterface() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(deployAddress,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Account account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    String contractName = "GeneScienceInterface";
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractScenario011.sol";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit,
        0L, consumeUserResourcePercent, null, deployKey, deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info2 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    geneScienceInterfaceContractAddress = info2.get().getContractAddress().toByteArray();
    Assert.assertTrue(info2.get().getResultValue() == 0);

    SmartContract smartContract = PublicMethed.getContract(geneScienceInterfaceContractAddress,
        blockingStubFull);
    Assert.assertFalse(StringUtils.isEmpty(smartContract.getBytecode()));
    Assert.assertTrue(smartContract.getAbi() != null);
    accountResource = PublicMethed.getAccountResource(deployAddress, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    cpuUsage = accountResource.getCpuUsed();
    account = PublicMethed.queryAccount(deployKey, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after cpu limit is " + Long.toString(cpuLimit));
    logger.info("after cpu usage is " + Long.toString(cpuUsage));
  }

  @Test(enabled = true, description = "Set three contract address for Kitty Core, "
      + "set three CXO roles")
  public void triggerToSetThreeContractAddressToKittyCore() {
    //Set SaleAuctionAddress to kitty core.
    String saleContractString = "\"" + Base58.encode58Check(saleClockAuctionContractAddress) + "\"";
    txid = PublicMethed.triggerContract(kittyCoreContractAddress, "setSaleAuctionAddress(address)",
        saleContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    //Set SiringAuctionAddress to kitty core.
    String siringContractString = "\"" + Base58.encode58Check(siringClockAuctionContractAddress)
        + "\"";
    txid = PublicMethed
        .triggerContract(kittyCoreContractAddress, "setSiringAuctionAddress(address)",
            siringContractString, false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    logger.info(txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    //Set gen contract to kitty core
    String genContractString = "\"" + Base58.encode58Check(geneScienceInterfaceContractAddress)
        + "\"";
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setGeneScienceAddress(address)", genContractString,
        false, 0, 10000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    //Start the game.
    Integer result = 1;
    Integer times = 0;
    while (result == 1) {
      txid = PublicMethed.triggerContract(kittyCoreContractAddress, "unpause()", "", false, 0,
          10000000L, deployAddress, deployKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      result = infoById.get().getResultValue();
      if (times++ == 3) {
        break;
      }
    }

    Assert.assertTrue(result == 0);
    logger.info("start the game " + txid);

    //Create one gen0 cat.
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "-1000000000000000", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "gen0CreatedCount()", "#", false,
        0, 100000000L, deployAddress, deployKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "getKitty(uint256)", "1", false, 0, 10000000, triggerAddress,
        triggerKey, blockingStubFull);
    logger.info("getKitty " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    String newCxoAddress = "\"" + Base58.encode58Check(triggerAddress)
        + "\"";

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("COO " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, 10000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CFO " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, 1000000, deployAddress,
        deployKey, blockingStubFull);
    logger.info("CEO " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Create Gen0 cat")
  public void triggerUseTriggerCpuUsage() {
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] triggerUseTriggerCpuUsageAddress = ecKey3.getAddress();
    final String triggerUseTriggerCpuUsageKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    Assert.assertTrue(
        PublicMethed.sendcoin(triggerUseTriggerCpuUsageAddress, 100000000000L,
            fromAddress, testKey002, blockingStubFull));
    String newCxoAddress = "\"" + Base58.encode58Check(triggerUseTriggerCpuUsageAddress)
        + "\"";
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final String txid1;
    final String txid2;
    final String txid3;
    txid1 = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCOO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("COO " + txid);

    txid2 = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCFO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("CFO " + txid);

    txid3 = PublicMethed.triggerContract(kittyCoreContractAddress,
        "setCEO(address)", newCxoAddress, false, 0, maxFeeLimit, triggerAddress,
        triggerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("CEO " + txid);

    infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    infoById = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    infoById = PublicMethed.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long beforeBalance = PublicMethed
        .queryAccount(triggerUseTriggerCpuUsageKey, blockingStubFull).getBalance();
    logger.info("before balance is " + Long.toString(beforeBalance));
    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "0", false,
        0, 100000000L, triggerUseTriggerCpuUsageAddress, triggerUseTriggerCpuUsageKey,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);
    logger.info("Q " + Long
        .toString(infoById.get().getReceipt().getCpuFee()));
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getCpuFee() > 10000);
    //    Assert.assertTrue(infoById.get().getReceipt().getOriginCpuUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsageTotal()
        == infoById.get().getReceipt().getCpuFee() / 100 + infoById.get().getReceipt()
        .getOriginCpuUsage());

    Long fee = infoById.get().getFee();
    Long afterBalance = PublicMethed
        .queryAccount(triggerUseTriggerCpuUsageKey, blockingStubFull1).getBalance();
    logger.info("after balance is " + Long.toString(afterBalance));
    logger.info("fee is " + Long.toString(fee));
    Assert.assertTrue(beforeBalance == afterBalance + fee);

    logger.info("before CpuUsage is " + infoById.get().getReceipt().getCpuUsage());
    logger.info("before CpuFee is " + infoById.get().getReceipt().getCpuFee());
    logger.info("before OriginCpuUsage is " + infoById.get().getReceipt()
        .getOriginCpuUsage());
    logger.info("before CpuTotal is " + infoById.get().getReceipt().getCpuUsageTotal());

    Assert.assertTrue(
        PublicMethed.freezeBalanceGetCpu(triggerUseTriggerCpuUsageAddress, 100000000L,
            5, 1, triggerUseTriggerCpuUsageKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    beforeBalance = PublicMethed.queryAccount(triggerUseTriggerCpuUsageKey, blockingStubFull)
        .getBalance();
    logger.info("before balance is " + Long.toString(beforeBalance));

    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(triggerUseTriggerCpuUsageAddress, blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    logger.info("before CpuLimit is " + Long.toString(cpuLimit));

    txid = PublicMethed.triggerContract(kittyCoreContractAddress,
        "createGen0Auction(uint256)", "0", false,
        0, 100000000L, triggerUseTriggerCpuUsageAddress, triggerUseTriggerCpuUsageKey,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull1);
    logger.info("after CpuUsage is " + infoById.get().getReceipt().getCpuUsage());
    logger.info("after CpuFee is " + infoById.get().getReceipt().getCpuFee());
    logger.info("after OriginCpuUsage is " + infoById.get().getReceipt().getOriginCpuUsage());
    logger.info("after CpuTotal is " + infoById.get().getReceipt().getCpuUsageTotal());
    fee = infoById.get().getFee();
    afterBalance = PublicMethed.queryAccount(triggerUseTriggerCpuUsageKey, blockingStubFull1)
        .getBalance();
    logger.info("after balance is " + Long.toString(afterBalance));
    logger.info("fee is " + Long.toString(fee));

    accountResource = PublicMethed
        .getAccountResource(triggerUseTriggerCpuUsageAddress, blockingStubFull1);
    cpuLimit = accountResource.getCpuLimit();

    logger.info("after CpuLimit is " + Long.toString(cpuLimit));

    Assert.assertTrue(infoById.get().getReceipt().getCpuUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getCpuFee() == 0);

    //Assert.assertTrue(infoById.get().getReceipt().getOriginCpuUsage() > 10000);
    Assert.assertTrue(infoById.get().getReceipt().getCpuUsageTotal() == infoById.get()
        .getReceipt().getCpuUsage() + infoById.get().getReceipt().getOriginCpuUsage());
    //    Assert.assertTrue(infoById.get().getReceipt().getCpuUsage() == infoById.get()
    //        .getReceipt().getOriginCpuUsage());

    Assert.assertTrue(beforeBalance == afterBalance + fee);
    PublicMethed.unFreezeBalance(deployAddress, deployKey, 1,
        deployAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(triggerAddress, triggerKey, 1,
        triggerAddress, blockingStubFull);

    PublicMethed
        .unFreezeBalance(triggerUseTriggerCpuUsageAddress, triggerUseTriggerCpuUsageKey, 1,
            triggerUseTriggerCpuUsageAddress, blockingStubFull);
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


