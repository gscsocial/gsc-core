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

package org.gsc.runtime;

import static org.gsc.runtime.GVMTestUtils.generateDeploySmartContractAndGetTransaction;
import static org.gsc.runtime.GVMTestUtils.generateTriggerSmartContractAndGetTransaction;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction;


@Slf4j

public class RuntimeImplTest {

  private Manager dbManager;
  private GSCApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "db_RuntimeImplTest";
  private Application AppT;
  private byte[] callerAddress;
  private long callerTotalBalance = 4_000_000_000L;
  private byte[] creatorAddress;
  private long creatorTotalBalance = 3_000_000_000L;

  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--db-directory", dbPath, "--debug"}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    callerAddress = Hex
        .decode(Wallet.getAddressPreFixString() + "0ef66235aec0b2bd93bf9bebc1279175e478b9e4");
    creatorAddress = Hex
        .decode(Wallet.getAddressPreFixString() + "6d1d156bd87069262ede95c4f822eea0d6a6a7c7");
    dbManager = context.getBean(Manager.class);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1569859200000L);
    dbManager.getDynamicPropertiesStore().saveTotalCpuWeight(5_000_000_000L); // unit is gsc
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(callerAddress, AccountType.Normal);
    deposit.addBalance(callerAddress, callerTotalBalance);
    deposit.createAccount(creatorAddress, AccountType.Normal);
    deposit.addBalance(creatorAddress, creatorTotalBalance);
    deposit.commit();
  }

  // // confirmed src code
  // pragma confirmed ^0.4.2;
  //
  // contract TestCpuLimit {
  //
  //   function testNotConstant(uint256 count) {
  //     uint256 curCount = 0;
  //     while(curCount < count) {
  //       uint256 a = 1;
  //       curCount += 1;
  //     }
  //   }
  //
  //   function testConstant(uint256 count) constant {
  //     uint256 curCount = 0;
  //     while(curCount < count) {
  //       uint256 a = 1;
  //       curCount += 1;
  //     }
  //   }
  //
  // }

  @Test
  public void getCreatorCpuLimit2Test() {

    long value = 10L;
    long feeLimit = 1_000_000_000L;
    long consumeUserResourcePercent = 0L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;

    Transaction trx = generateDeploySmartContractAndGetTransaction(contractName, creatorAddress,
        ABI,
        code, value, feeLimit, consumeUserResourcePercent, libraryAddressPair);

    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    deposit = DepositImpl.createRoot(dbManager);
    AccountWrapper creatorAccount = deposit.getAccount(creatorAddress);

    long expectCpuLimit1 = 100_000_000L;
    Assert.assertEquals(
        runtimeImpl.getAccountCpuLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectCpuLimit1);

    value = 2_500_000_000L;
    long expectCpuLimit2 = 50_000_000L;
    Assert.assertEquals(
        runtimeImpl.getAccountCpuLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectCpuLimit2);

    value = 10L;
    feeLimit = 1_000_000L;
    long expectCpuLimit3 = 100_000L;
    Assert.assertEquals(
        runtimeImpl.getAccountCpuLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectCpuLimit3);

    long frozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - frozenBalance;
    creatorAccount.setFrozenForCpu(frozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    long expectCpuLimit4 = 100000000L;
    Assert.assertEquals(
        runtimeImpl.getAccountCpuLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectCpuLimit4);

    feeLimit = 3_000_000_000L;
    value = 10L;
    long expectCpuLimit5 = 200_002_499L;
    Assert.assertEquals(
        runtimeImpl.getAccountCpuLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectCpuLimit5);

    feeLimit = 3_000L;
    value = 10L;
    long expectCpuLimit6 = 300L;
    Assert.assertEquals(
        runtimeImpl.getAccountCpuLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectCpuLimit6);

  }

  @Test
  public void getCallerAndCreatorCpuLimit2With0PercentTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {

    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 0L;
    long creatorCpuLimit = 5_000L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;
    GVMTestResult result = GVMTestUtils
        .deployContractWithCreatorCpuLimitAndReturnGvmTestResult(contractName, creatorAddress,
            ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, dbManager, null,
            creatorCpuLimit);

    byte[] contractAddress = result.getContractAddress();
    byte[] triggerData = GVMTestUtils.parseAbi("testNotConstant()", null);
    Transaction trx = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        triggerData, value, feeLimit);

    deposit = DepositImpl.createRoot(dbManager);
    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    AccountWrapper creatorAccount = deposit.getAccount(creatorAddress);
    AccountWrapper callerAccount = deposit.getAccount(callerAddress);
    Contract.TriggerSmartContract contract = ContractWrapper.getTriggerContractFromTransaction(trx);

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectCpuLimit1 = 100_000_000L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit1);

    long creatorFrozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - creatorFrozenBalance;
    creatorAccount.setFrozenForCpu(creatorFrozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectCpuLimit2 = 100_002_500L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit2);

    value = 3_500_000_000L;
    long expectCpuLimit3 = 5_0002_500L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit3);

    value = 10L;
    feeLimit = 5_000_000_000L;
    long expectCpuLimit4 = 400_002_499L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit4);

    long callerFrozenBalance = 1_000_000_000L;
    callerAccount.setFrozenForCpu(callerFrozenBalance, 0L);
    callerAccount.setBalance(callerAccount.getBalance() - callerFrozenBalance);
    deposit.putAccountValue(callerAddress, callerAccount);
    deposit.commit();

    value = 10L;
    feeLimit = 5_000_000_000L;
    long expectCpuLimit5 = 300_004_999L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit5);

  }

  @Test
  public void getCallerAndCreatorCpuLimit2With40PercentTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {

    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 40L;
    long creatorCpuLimit = 5_000L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;
    GVMTestResult result = GVMTestUtils
        .deployContractWithCreatorCpuLimitAndReturnGvmTestResult(contractName, creatorAddress,
            ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, dbManager, null,
            creatorCpuLimit);

    byte[] contractAddress = result.getContractAddress();
    byte[] triggerData = GVMTestUtils.parseAbi("testNotConstant()", null);
    Transaction trx = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        triggerData, value, feeLimit);

    deposit = DepositImpl.createRoot(dbManager);
    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    AccountWrapper creatorAccount = deposit.getAccount(creatorAddress);
    AccountWrapper callerAccount = deposit.getAccount(callerAddress);
    Contract.TriggerSmartContract contract = ContractWrapper.getTriggerContractFromTransaction(trx);

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectCpuLimit1 = 100_000_000L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit1);

    long creatorFrozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - creatorFrozenBalance;
    creatorAccount.setFrozenForCpu(creatorFrozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectCpuLimit2 = 10_0002_500L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit2);

    value = 3_999_950_000L;
    long expectCpuLimit3 = 7_500L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit3);

  }

  @Test
  public void getCallerAndCreatorCpuLimit2With100PercentTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {

    long value = 0;
    long feeLimit = 1_000_000_000L; // dot
    long consumeUserResourcePercent = 100L;
    long creatorCpuLimit = 5_000L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;
    GVMTestResult result = GVMTestUtils
        .deployContractWithCreatorCpuLimitAndReturnGvmTestResult(contractName, creatorAddress,
            ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, dbManager, null,
            creatorCpuLimit);

    byte[] contractAddress = result.getContractAddress();
    byte[] triggerData = GVMTestUtils.parseAbi("testNotConstant()", null);
    Transaction trx = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        triggerData, value, feeLimit);

    deposit = DepositImpl.createRoot(dbManager);
    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    AccountWrapper creatorAccount = deposit.getAccount(creatorAddress);
    AccountWrapper callerAccount = deposit.getAccount(callerAddress);
    Contract.TriggerSmartContract contract = ContractWrapper.getTriggerContractFromTransaction(trx);

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectCpuLimit1 = 100_000_000L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit1);

    long creatorFrozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - creatorFrozenBalance;
    creatorAccount.setFrozenForCpu(creatorFrozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectCpuLimit2 = 100_000_000L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit2);

    value = 3_999_950_000L;
    long expectCpuLimit3 = 5000L;
    Assert.assertEquals(
        runtimeImpl
            .getTotalCpuLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit,
                value),
        expectCpuLimit3);

  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}

