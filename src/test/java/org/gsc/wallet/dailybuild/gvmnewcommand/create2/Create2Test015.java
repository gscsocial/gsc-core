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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
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
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.WalletClient;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class Create2Test015 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] factoryContractAddress = null;
  private byte[] callerContractAddress = null;
  private byte[] callContractAddress = null;
  private byte[] delegateCallContractAddress = null;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);
  }

  @Test(enabled = true, description = "Deploy caller contract")
  public void test01DeployCallerContract() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long cpuLimit = accountResource.getCpuLimit();
    long cpuUsage = accountResource.getCpuUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before cpuLimit is " + Long.toString(cpuLimit));
    logger.info("before cpuUsage is " + Long.toString(cpuUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String filePath = "./src/test/resources/soliditycode_v0.5.4/create2CallContract.sol";
    String contractName = "callerContract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    final String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    cpuUsage = accountResource.getCpuUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after cpuLimit is " + Long.toString(cpuLimit));
    logger.info("after cpuUsage is " + Long.toString(cpuUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("CpuUsageTotal: " + transactionInfo.getReceipt().getCpuUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    callerContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(callerContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "Deploy factory contract")
  public void test02DeployFactoryContract() {

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long cpuLimit = accountResource.getCpuLimit();
    long cpuUsage = accountResource.getCpuUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before cpuLimit is " + Long.toString(cpuLimit));
    logger.info("before cpuUsage is " + Long.toString(cpuUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String filePath = "./src/test/resources/soliditycode_v0.5.4/create2contract.sol";
    String contractName = "Factory";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    final String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    cpuLimit = accountResource.getCpuLimit();
    cpuUsage = accountResource.getCpuUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after cpuLimit is " + Long.toString(cpuLimit));
    logger.info("after cpuUsage is " + Long.toString(cpuUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("CpuUsageTotal: " + transactionInfo.getReceipt().getCpuUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    factoryContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(factoryContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "Trigger callCreate2 function in caller contract "
      + "with factory contract address")
  public void test03TriggerCreate2ToDeployTestContract() {

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devCpuLimitBefore = accountResource.getCpuLimit();
    long devCpuUsageBefore = accountResource.getCpuUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devCpuLimitBefore is " + Long.toString(devCpuLimitBefore));
    logger.info("before trigger, devCpuUsageBefore is " + Long.toString(devCpuUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitBefore = accountResource.getCpuLimit();
    long userCpuUsageBefore = accountResource.getCpuUsed();
    long userBalanceBefore = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userCpuLimitBefore is " + Long.toString(userCpuLimitBefore));
    logger.info("before trigger, userCpuUsageBefore is " + Long.toString(userCpuUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));

    Long callValue = Long.valueOf(0);

    String filePath = "./src/test/resources/soliditycode_v0.5.4/create2contract.sol";
    String contractName = "TestConstract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String testContractCode = retMap.get("byteCode").toString();
    Long salt = 1L;

    String param = "\"" + Base58.encode58Check(factoryContractAddress)
        + "\",\"" + testContractCode + "\"," + salt;

    final String triggerTxid = PublicMethed.triggerContract(callerContractAddress,
        "callCreate2(address,bytes,uint256)", param, false, callValue,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devCpuLimitAfter = accountResource.getCpuLimit();
    long devCpuUsageAfter = accountResource.getCpuUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devCpuLimitAfter is " + Long.toString(devCpuLimitAfter));
    logger.info("after trigger, devCpuUsageAfter is " + Long.toString(devCpuUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitAfter = accountResource.getCpuLimit();
    long userCpuUsageAfter = accountResource.getCpuUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("after trigger, userCpuLimitAfter is " + Long.toString(userCpuLimitAfter));
    logger.info("after trigger, userCpuUsageAfter is " + Long.toString(userCpuUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("CpuUsageTotal: " + transactionInfo.getReceipt().getCpuUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    logger.info(
        "the value: " + PublicMethed
            .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray()));

    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray());

    Long actualSalt = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));

    logger.info("actualSalt: " + actualSalt);

    byte[] tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(0)),
        12, tmpAddress, 0, 20);
    String addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
    String addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);

    callContractAddress = WalletClient.decodeFromBase58Check(addressFinal);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail(
          "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
    }

    SmartContract smartContract = PublicMethed.getContract(callContractAddress, blockingStubFull);

    // contract created by create2, doesn't have ABI
    Assert.assertEquals(0, smartContract.getAbi().getEntrysCount());

    // the contract owner of contract created by create2 is the factory contract
    Assert.assertEquals(Base58.encode58Check(factoryContractAddress),
        Base58.encode58Check(smartContract.getOriginAddress().toByteArray()));

    // call type make the caller contract to be the owner of
    // test contract (the contract address in transaction info)
    Assert.assertEquals(Base58.encode58Check(callerContractAddress),
        Base58.encode58Check(infoById.get().getContractAddress().toByteArray()));
  }

  @Test(enabled = true, description = "Trigger delegateCallCreate2 function in caller contract "
      + "with factory contract address")
  public void test04TriggerCreate2ToDeployTestContract() {

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devCpuLimitBefore = accountResource.getCpuLimit();
    long devCpuUsageBefore = accountResource.getCpuUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devCpuLimitBefore is " + Long.toString(devCpuLimitBefore));
    logger.info("before trigger, devCpuUsageBefore is " + Long.toString(devCpuUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitBefore = accountResource.getCpuLimit();
    long userCpuUsageBefore = accountResource.getCpuUsed();
    long userBalanceBefore = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userCpuLimitBefore is " + Long.toString(userCpuLimitBefore));
    logger.info("before trigger, userCpuUsageBefore is " + Long.toString(userCpuUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));

    Long callValue = Long.valueOf(0);

    String filePath = "./src/test/resources/soliditycode_v0.5.4/create2contract.sol";
    String contractName = "TestConstract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String testContractCode = retMap.get("byteCode").toString();

    Long salt = 1L;

    String param = "\"" + Base58.encode58Check(factoryContractAddress)
        + "\",\"" + testContractCode + "\"," + salt;

    final String triggerTxid = PublicMethed.triggerContract(callerContractAddress,
        "delegateCallCreate2(address,bytes,uint256)", param, false, callValue,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devCpuLimitAfter = accountResource.getCpuLimit();
    long devCpuUsageAfter = accountResource.getCpuUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devCpuLimitAfter is " + Long.toString(devCpuLimitAfter));
    logger.info("after trigger, devCpuUsageAfter is " + Long.toString(devCpuUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitAfter = accountResource.getCpuLimit();
    long userCpuUsageAfter = accountResource.getCpuUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("after trigger, userCpuLimitAfter is " + Long.toString(userCpuLimitAfter));
    logger.info("after trigger, userCpuUsageAfter is " + Long.toString(userCpuUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("CpuUsageTotal: " + transactionInfo.getReceipt().getCpuUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    logger.info(
        "the value: " + PublicMethed
            .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray()));

    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray());

    Long actualSalt = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));

    logger.info("actualSalt: " + actualSalt);

    byte[] tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(0)), 12, tmpAddress, 0, 20);
    String addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
    String addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);

    delegateCallContractAddress = WalletClient.decodeFromBase58Check(addressFinal);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail(
          "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
    }

    SmartContract smartContract = PublicMethed
        .getContract(delegateCallContractAddress, blockingStubFull);

    // contract created by create2, doesn't have ABI
    Assert.assertEquals(0, smartContract.getAbi().getEntrysCount());

    // delegatecall type make the caller contract to be the owner of test contract (contract info)
    Assert.assertEquals(Base58.encode58Check(callerContractAddress),
        Base58.encode58Check(smartContract.getOriginAddress().toByteArray()));

    // call type make the caller contract to be the owner of test contract
    // (the contract address in transaction info)
    Assert.assertEquals(Base58.encode58Check(callerContractAddress),
        Base58.encode58Check(infoById.get().getContractAddress().toByteArray()));
  }

  @Test(enabled = true, description = "Trigger test contract")
  public void test09TriggerTestContract() {

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devCpuLimitBefore = accountResource.getCpuLimit();
    long devCpuUsageBefore = accountResource.getCpuUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devCpuLimitBefore is " + Long.toString(devCpuLimitBefore));
    logger.info("before trigger, devCpuUsageBefore is " + Long.toString(devCpuUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitBefore = accountResource.getCpuLimit();
    long userCpuUsageBefore = accountResource.getCpuUsed();
    long userBalanceBefore = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userCpuLimitBefore is " + Long.toString(userCpuLimitBefore));
    logger.info("before trigger, userCpuUsageBefore is " + Long.toString(userCpuUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));

    Long callValue = Long.valueOf(0);

    final String triggerTxid = PublicMethed.triggerContract(callContractAddress,
        "plusOne()", "#", false, callValue,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devCpuLimitAfter = accountResource.getCpuLimit();
    long devCpuUsageAfter = accountResource.getCpuUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devCpuLimitAfter is " + Long.toString(devCpuLimitAfter));
    logger.info("after trigger, devCpuUsageAfter is " + Long.toString(devCpuUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userCpuLimitAfter = accountResource.getCpuLimit();
    long userCpuUsageAfter = accountResource.getCpuUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("after trigger, userCpuLimitAfter is " + Long.toString(userCpuLimitAfter));
    logger.info("after trigger, userCpuUsageAfter is " + Long.toString(userCpuUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("CpuUsageTotal: " + transactionInfo.getReceipt().getCpuUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    logger.info(
        "the value: " + PublicMethed
            .getStrings(transactionInfo.getContractResult(0).toByteArray()));

    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getContractResult(0).toByteArray());

    Long ret = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));

    logger.info("ret: " + ret);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    SmartContract smartContract = PublicMethed.getContract(infoById.get().getContractAddress()
        .toByteArray(), blockingStubFull);

    long consumeUserPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeUserPercent);

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


