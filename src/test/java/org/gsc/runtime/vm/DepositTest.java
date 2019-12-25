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

package org.gsc.runtime.vm;

import java.io.File;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.GVMTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.GVMTestResult;
import org.gsc.db.dbsource.Deposit;
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
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
public class DepositTest {

  private Manager manager;
  private GSCApplicationContext context;
  private String dbPath = "db_DepostitTest";
  private String OWNER_ADDRESS;
  private Deposit rootDeposit;

  @Before
  public void init() {
    Args.setParam(new String[]{"--db-directory", dbPath, "--debug"}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    manager = context.getBean(Manager.class);
    rootDeposit = DepositImpl.createRoot(manager);
    rootDeposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    rootDeposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);
    rootDeposit.commit();
  }

/*  pragma confirmed ^0.4.0;

  contract A {
    uint256 public n1;
    uint256 public n2;
    function callBcallA(address addr, uint256 _n1, uint256 _n2) public {
      n1 = _n1;
      string memory method  = "callA(address,uint256)";
      bytes4 methodId = bytes4(keccak256(method));
      addr.call(methodId, address(this), _n2);
    }

    function callBcallARevert(address addr, uint256 _n1, uint256 _n2) public {
      n1 = _n1;
      string memory method  = "callARevert(address,uint256)";
      bytes4 methodId = bytes4(keccak256(method));
      addr.call(methodId, address(this), _n2);
    }

    function changeN(uint256 _n2) {
      n2 = _n2;
    }
  }

  contract B {
    function callA(address addr, uint256 _n2) {
      string memory method  = "changeN(uint256)";
      bytes4 methodId = bytes4(keccak256(method));
      addr.call(methodId, _n2);
    }

    function callARevert(address addr, uint256 _n2) {
      string memory method  = "changeN(uint256)";
      bytes4 methodId = bytes4(keccak256(method));
      addr.call(methodId, _n2);
      revert();
    }
  }*/


  @Test
  public void loopCallTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
//    this.manager.getDynamicPropertiesStore()
//        .statsByVersion(ForkBlockVersionConsts.CPU_LIMIT, stats);
//    VMConfig.initVmHardFork();
    String contractA = "A";
    String contractB = "B";
    byte[] address = Hex.decode(OWNER_ADDRESS);

    String aABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"n1\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"n2\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"changeN\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n1\",\"type\":\"uint256\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callBcallARevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n1\",\"type\":\"uint256\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callBcallA\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String bABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callA\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callARevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    String aCode = "608060405234801561001057600080fd5b506102c8806100206000396000f30060806040526004361061006c5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166336fe1f2581146100715780634aaa831d146100985780636aac216d146100ad578063a71d1d68146100c7578063b9d3e3ce146100fb575b600080fd5b34801561007d57600080fd5b5061008661012f565b60408051918252519081900360200190f35b3480156100a457600080fd5b50610086610135565b3480156100b957600080fd5b506100c560043561013b565b005b3480156100d357600080fd5b506100c573ffffffffffffffffffffffffffffffffffffffff60043516602435604435610140565b34801561010757600080fd5b506100c573ffffffffffffffffffffffffffffffffffffffff6004351660243560443561024e565b60005481565b60015481565b600155565b6000828155604080518082018252601c8082527f63616c6c4152657665727428616464726573732c75696e7432353629000000006020830190815292519193928492918291908083835b602083106101a95780518252601f19909201916020918201910161018a565b5181516000196020949094036101000a939093019283169219169190911790526040805193909101839003832063ffffffff7c0100000000000000000000000000000000000000000000000000000000808304918216028552306004860152602485018a9052915190965073ffffffffffffffffffffffffffffffffffffffff8b16955090935060448084019360009350829003018183875af1505050505050505050565b600082815560408051808201825260168082527f63616c6c4128616464726573732c75696e7432353629000000000000000000006020830190815292519193928492918291908083836101a95600a165627a7a72305820f6164f67f8344af6aa83a5f724f6c3cb7d09905c4bc94729d1d72e9f11d0df190029";
    String bCode = "608060405234801561001057600080fd5b5061029f806100206000396000f3006080604052600436106100325763ffffffff60e060020a600035041663417c7cc581146100375780637a9a0e721461006a575b600080fd5b34801561004357600080fd5b5061006873ffffffffffffffffffffffffffffffffffffffff6004351660243561009b565b005b34801561007657600080fd5b5061006873ffffffffffffffffffffffffffffffffffffffff60043516602435610187565b60408051808201825260108082527f6368616e67654e2875696e74323536290000000000000000000000000000000060208301908152925191926000928492909182918083835b602083106101015780518252601f1990920191602091820191016100e2565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051809103902090508373ffffffffffffffffffffffffffffffffffffffff168160e060020a9004846040518263ffffffff1660e060020a028152600401808281526020019150506000604051808303816000875af15050505050505050565b60408051808201825260108082527f6368616e67654e2875696e74323536290000000000000000000000000000000060208301908152925191926000928492909182918083835b602083106101ed5780518252601f1990920191602091820191016101ce565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051809103902090508373ffffffffffffffffffffffffffffffffffffffff168160e060020a9004846040518263ffffffff1660e060020a028152600401808281526020019150506000604051808303816000875af19250505050600080fd00a165627a7a723058209cb73b547b599b5d2a96ade83e03d656f8025f4ac7642f8b9aab37cc2574f8310029";

    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;
    long engeryLiimt = 100000000;

    Transaction aTrx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractA, address, aABI, aCode, value, fee, consumeUserResourcePercent, null, engeryLiimt);
    Runtime runtime = GVMTestUtils
        .processTransactionAndReturnRuntime(aTrx, DepositImpl.createRoot(manager), null);
    Assert.assertNull(runtime.getRuntimeError());

    Transaction bTrx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractB, address, bABI, bCode, value, fee, consumeUserResourcePercent, null, engeryLiimt);
    runtime = GVMTestUtils
        .processTransactionAndReturnRuntime(bTrx, DepositImpl.createRoot(manager), null);
    Assert.assertNull(runtime.getRuntimeError());

    byte[] aAddress = Wallet.generateContractAddress(aTrx);
    byte[] bAddress = Wallet.generateContractAddress(bTrx);

    // tigger contractA
    // callBcallA(address,uint256,uint256)
    // <bAddress>,1,2
    //
    String params1 = Hex.toHexString(new DataWord(bAddress).getData())
        + "00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000002";
    System.err.println(params1);

    byte[] triggerData = GVMTestUtils
        .parseAbi("callBcallARevert(address,uint256,uint256)", params1);
    GVMTestResult result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, triggerData, 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    // check result
    // expected: n1 = 1; n2 = 0
    byte[] checkN1Data = GVMTestUtils.parseAbi("n1()", null);
    byte[] checkN2Data = GVMTestUtils.parseAbi("n2()", null);

    GVMTestResult checkN1 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN1Data, 0, fee, manager, null);
    GVMTestResult checkN2 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN2Data, 0, fee, manager, null);

    System.out.println(Hex.toHexString(checkN1.getRuntime().getResult().getHReturn()));
    System.out.println(Hex.toHexString(checkN2.getRuntime().getResult().getHReturn()));

    Assert.assertEquals(checkN1.getRuntime().getResult().getHReturn(), new DataWord(1).getData());
    Assert.assertEquals(checkN2.getRuntime().getResult().getHReturn(), new DataWord(0).getData());

    // tigger contractA
    // callBcallA(address,uint256,uint256)
    // <bAddress>,100,1000
    String params2 = Hex.toHexString(new DataWord(bAddress).getData())
        + "000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000003e8";
    triggerData = GVMTestUtils.parseAbi("callBcallA(address,uint256,uint256)", params2);
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, triggerData, 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    checkN1 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN1Data, 0, fee, manager, null);
    checkN2 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN2Data, 0, fee, manager, null);
    System.out.println(Hex.toHexString(checkN1.getRuntime().getResult().getHReturn()));
    System.out.println(Hex.toHexString(checkN2.getRuntime().getResult().getHReturn()));
    Assert.assertEquals(checkN1.getRuntime().getResult().getHReturn(), new DataWord(100).getData());
    Assert
        .assertEquals(checkN2.getRuntime().getResult().getHReturn(), new DataWord(1000).getData());
//    VMConfig.setCPU_LIMIT_HARD_FORK(false);
  }

  @Test
  @Ignore
  public void loopCallTestOldVersion()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 0);
//    this.manager.getDynamicPropertiesStore()
//        .statsByVersion(ForkBlockVersionConsts.CPU_LIMIT, stats);

    String contractA = "A";
    String contractB = "B";
    byte[] address = Hex.decode(OWNER_ADDRESS);

    String aABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"n1\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"n2\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"changeN\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n1\",\"type\":\"uint256\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callBcallARevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n1\",\"type\":\"uint256\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callBcallA\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String bABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callA\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"},{\"name\":\"_n2\",\"type\":\"uint256\"}],\"name\":\"callARevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    String aCode = "608060405234801561001057600080fd5b506102c8806100206000396000f30060806040526004361061006c5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166336fe1f2581146100715780634aaa831d146100985780636aac216d146100ad578063a71d1d68146100c7578063b9d3e3ce146100fb575b600080fd5b34801561007d57600080fd5b5061008661012f565b60408051918252519081900360200190f35b3480156100a457600080fd5b50610086610135565b3480156100b957600080fd5b506100c560043561013b565b005b3480156100d357600080fd5b506100c573ffffffffffffffffffffffffffffffffffffffff60043516602435604435610140565b34801561010757600080fd5b506100c573ffffffffffffffffffffffffffffffffffffffff6004351660243560443561024e565b60005481565b60015481565b600155565b6000828155604080518082018252601c8082527f63616c6c4152657665727428616464726573732c75696e7432353629000000006020830190815292519193928492918291908083835b602083106101a95780518252601f19909201916020918201910161018a565b5181516000196020949094036101000a939093019283169219169190911790526040805193909101839003832063ffffffff7c0100000000000000000000000000000000000000000000000000000000808304918216028552306004860152602485018a9052915190965073ffffffffffffffffffffffffffffffffffffffff8b16955090935060448084019360009350829003018183875af1505050505050505050565b600082815560408051808201825260168082527f63616c6c4128616464726573732c75696e7432353629000000000000000000006020830190815292519193928492918291908083836101a95600a165627a7a72305820f6164f67f8344af6aa83a5f724f6c3cb7d09905c4bc94729d1d72e9f11d0df190029";
    String bCode = "608060405234801561001057600080fd5b5061029f806100206000396000f3006080604052600436106100325763ffffffff60e060020a600035041663417c7cc581146100375780637a9a0e721461006a575b600080fd5b34801561004357600080fd5b5061006873ffffffffffffffffffffffffffffffffffffffff6004351660243561009b565b005b34801561007657600080fd5b5061006873ffffffffffffffffffffffffffffffffffffffff60043516602435610187565b60408051808201825260108082527f6368616e67654e2875696e74323536290000000000000000000000000000000060208301908152925191926000928492909182918083835b602083106101015780518252601f1990920191602091820191016100e2565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051809103902090508373ffffffffffffffffffffffffffffffffffffffff168160e060020a9004846040518263ffffffff1660e060020a028152600401808281526020019150506000604051808303816000875af15050505050505050565b60408051808201825260108082527f6368616e67654e2875696e74323536290000000000000000000000000000000060208301908152925191926000928492909182918083835b602083106101ed5780518252601f1990920191602091820191016101ce565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051809103902090508373ffffffffffffffffffffffffffffffffffffffff168160e060020a9004846040518263ffffffff1660e060020a028152600401808281526020019150506000604051808303816000875af19250505050600080fd00a165627a7a723058209cb73b547b599b5d2a96ade83e03d656f8025f4ac7642f8b9aab37cc2574f8310029";

    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    Transaction aTrx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractA, address, aABI, aCode, value, fee, consumeUserResourcePercent, null);
    Deposit rootDeposit = DepositImpl.createRoot(manager);
    Runtime runtime = GVMTestUtils.processTransactionAndReturnRuntime(aTrx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    Transaction bTrx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractB, address, bABI, bCode, value, fee, consumeUserResourcePercent, null);
    rootDeposit = DepositImpl.createRoot(manager);
    runtime = GVMTestUtils.processTransactionAndReturnRuntime(bTrx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    byte[] aAddress = Wallet.generateContractAddress(aTrx);
    byte[] bAddress = Wallet.generateContractAddress(bTrx);

    // tigger contractA
    // callBcallA(address,uint256,uint256)
    // <bAddress>,1,2
    //
    String params1 = Hex.toHexString(new DataWord(bAddress).getData())
        + "00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000002";

    byte[] triggerData = GVMTestUtils
        .parseAbi("callBcallARevert(address,uint256,uint256)", params1);
    GVMTestResult result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, triggerData, 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    // check result
    // expected: n1 = 1; n2 = 0
    byte[] checkN1Data = GVMTestUtils.parseAbi("n1()", null);
    byte[] checkN2Data = GVMTestUtils.parseAbi("n2()", null);

    GVMTestResult checkN1 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN1Data, 0, fee, manager, null);
    GVMTestResult checkN2 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN2Data, 0, fee, manager, null);

    System.out.println(Hex.toHexString(checkN1.getRuntime().getResult().getHReturn()));
    System.out.println(Hex.toHexString(checkN2.getRuntime().getResult().getHReturn()));

    Assert.assertEquals(checkN1.getRuntime().getResult().getHReturn(), new DataWord(1).getData());
    Assert.assertEquals(checkN2.getRuntime().getResult().getHReturn(), new DataWord(2).getData());

    // tigger contractA
    // callBcallA(address,uint256,uint256)
    // <bAddress>,100,1000
    String params2 = Hex.toHexString(new DataWord(bAddress).getData())
        + "000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000003e8";
    triggerData = GVMTestUtils.parseAbi("callBcallA(address,uint256,uint256)", params2);
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, triggerData, 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    checkN1 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN1Data, 0, fee, manager, null);
    checkN2 = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            aAddress, checkN2Data, 0, fee, manager, null);
    System.out.println(Hex.toHexString(checkN1.getRuntime().getResult().getHReturn()));
    System.out.println(Hex.toHexString(checkN2.getRuntime().getResult().getHReturn()));
    Assert.assertEquals(checkN1.getRuntime().getResult().getHReturn(), new DataWord(100).getData());
    Assert
        .assertEquals(checkN2.getRuntime().getResult().getHReturn(), new DataWord(1000).getData());
//    VMConfig.setCPU_LIMIT_HARD_FORK(false);
  }


  @After
  public void destroy() {
    Args.clearParam();
    ApplicationFactory.create(context).shutdown();
    ApplicationFactory.create(context).shutdownServices();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
  }

}
