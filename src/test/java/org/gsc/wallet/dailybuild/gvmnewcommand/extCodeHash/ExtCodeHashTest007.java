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

package org.gsc.wallet.dailybuild.gvmnewcommand.extCodeHash;

import com.google.protobuf.ByteString;
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
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ExtCodeHashTest007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] testAddressOld = null;
  private byte[] testAddressNew = null;
  private byte[] testAddress2 = null;
  private byte[] extCodeHashContractAddress = null;

  private String expectedCodeHash = null;
  private String expectedCodeHashOld = null;


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

  @Test(enabled = true, description = "Deploy testNoPayable contract using old confirmed")
  public void test01DeployTestContractOld() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull), 5, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        5, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

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

    String contractName = "testExtHashContract";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a576000"
        + "80fd5b5060ef806100396000396000f30060806040526004361060485763ffffffff7c010000000000000000"
        + "0000000000000000000000000000000000000000600035041663c518aa0f8114604d578063e5aa3d58146089"
        + "575b600080fd5b348015605857600080fd5b50d38015606457600080fd5b50d28015607057600080fd5b5060"
        + "7760b3565b60408051918252519081900360200190f35b348015609457600080fd5b50d3801560a057600080"
        + "fd5b50d2801560ac57600080fd5b50607760bd565b6001600081905590565b600054815600a165627a7a7230"
        + "5820766b4e2fca9081689cd89419411d2cbc5588a17a5c9fa900fd9cfe4b0d9652be0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testNoPayable\","
        + "\"outputs\":[{\"name\":\"z\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,"
        + "\"inputs\":[],\"name\":\"i\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}]";

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

    testAddressOld = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(testAddressOld,
        blockingStubFull);
  }

  @Test(enabled = true, description = "Deploy testNoPayable contract using new confirmed")
  public void test02DeployTestContractNew() {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull), 5, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        5, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

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

    String contractName = "testConstantContract";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a5760"
        + "0080fd5b5060c5806100396000396000f3fe6080604052348015600f57600080fd5b50d38015601b576000"
        + "80fd5b50d28015602757600080fd5b50600436106066577c01000000000000000000000000000000000000"
        + "000000000000000000006000350463c518aa0f8114606b578063e5aa3d58146083575b600080fd5b607160"
        + "89565b60408051918252519081900360200190f35b60716093565b6001600081905590565b6000548156fe"
        + "a165627a7a723058205c5aadfbd06ea264db7b73e7b7f3c36ac64a9d520ba46b4bc7f1dc56252f17ac0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testNoPayable\",\"outputs\":[{\""
        + "name\":\"z\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"i\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\""
        + "type\":\"function\"}]";

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

    testAddressNew = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(testAddressNew,
        blockingStubFull);
  }

  @Test(enabled = true, description = "Deploy extcodehash contract")
  public void test03DeployExtCodeHashContract() {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull), 5, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        5, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

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

    String filePath = "./src/test/resources/soliditycode_v0.5.4/extCodeHash.sol";
    String contractName = "TestExtCodeHash";
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

    extCodeHashContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(extCodeHashContractAddress,
        blockingStubFull);
  }

  @Test(enabled = true, description = "Get contract code hash with old confirmed")
  public void test04GetTestOldCodeHash() {
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 5, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

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

    String param = "\"" + Base58.encode58Check(testAddressOld) + "\"";
    final String triggerTxid = PublicMethed.triggerContract(extCodeHashContractAddress,
        "getCodeHashByAddr(address)", param, false, callValue,
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

    if (infoById.get().getResultValue() != 0) {
      Assert.fail(
          "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
    }

    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getContractResult(0).toByteArray());

    logger.info(
        "the value: " + retList);

    expectedCodeHashOld = retList.get(0);
    Assert.assertEquals(
        "B4AB5B9FF1A4FF7793E60EBFF0C769443AF66D0A6F9455AF145432CE8BA78175",expectedCodeHashOld);
    Assert.assertFalse(retList.isEmpty());
  }

  @Test(enabled = true, description = "Get contract code hash with new confirmed")
  public void test05GetTestNewCodeHash() {
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 5, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

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

    String param = "\"" + Base58.encode58Check(testAddressNew) + "\"";
    final String triggerTxid = PublicMethed.triggerContract(extCodeHashContractAddress,
        "getCodeHashByAddr(address)", param, false, callValue,
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

    if (infoById.get().getResultValue() != 0) {
      Assert.fail(
          "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
    }

    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getContractResult(0).toByteArray());

    logger.info("the value: " + retList);
    Assert.assertFalse(retList.isEmpty());

    Assert.assertNotEquals(retList.get(0), expectedCodeHashOld);
    expectedCodeHash = retList.get(0);
    Assert.assertEquals(
        "34DB53BD1F7214367E8D6B2A7A6FBBF0E3B7DDB4939ECADE4CDEF6749C27A2DA",expectedCodeHash);
  }

  @Test(enabled = true, description = "Deploy contract using new confirmed again")
  public void test06DeployTest2Contract() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull), 5, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        5, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

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

    String contractName = "testConstantContract";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a5760"
        + "0080fd5b5060c5806100396000396000f3fe6080604052348015600f57600080fd5b50d38015601b576000"
        + "80fd5b50d28015602757600080fd5b50600436106066577c01000000000000000000000000000000000000"
        + "000000000000000000006000350463c518aa0f8114606b578063e5aa3d58146083575b600080fd5b607160"
        + "89565b60408051918252519081900360200190f35b60716093565b6001600081905590565b6000548156fe"
        + "a165627a7a723058205c5aadfbd06ea264db7b73e7b7f3c36ac64a9d520ba46b4bc7f1dc56252f17ac0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testNoPayable\",\"outputs\":[{\""
        + "name\":\"z\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"i\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\""
        + "type\":\"function\"}]";

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

    testAddress2 = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(testAddress2,
        blockingStubFull);
  }

  @Test(enabled = true, description = "Get contract code hash with test2")
  public void test07GetTest2CodeHash() {
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 5, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

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

    String param = "\"" + Base58.encode58Check(testAddress2) + "\"";
    final String triggerTxid = PublicMethed.triggerContract(extCodeHashContractAddress,
        "getCodeHashByAddr(address)", param, false, callValue,
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

    if (infoById.get().getResultValue() != 0) {
      Assert.fail(
          "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
    }

    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getContractResult(0).toByteArray());

    logger.info("the value: " + retList);
    Assert.assertFalse(retList.isEmpty());

    Assert.assertEquals(expectedCodeHash, retList.get(0));

    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1,
        dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0,
        dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 1,
        user001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0,
        user001Address, blockingStubFull);
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


