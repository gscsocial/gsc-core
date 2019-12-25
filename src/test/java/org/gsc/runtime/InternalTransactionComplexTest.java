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

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.vm.DataWord;
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

@Slf4j
public class InternalTransactionComplexTest {

  private static Runtime runtime;
  private static Manager dbManager;
  private static GSCApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;
  private static final String dbPath = "db_InternalTransactionComplexTest";
  private static final String OWNER_ADDRESS;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath, "--debug", "--support-constant"},
        Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 100000000);
  }

  /**
   * pragma confirmed 0.4.24;
   *
   * // this is to test wither the TVM is returning vars from one contract calling another //
   * contract's functions.
   *
   * contract callerContract { // lets set up our instance of the new contract calledContract
   * CALLED_INSTANCE; // lets set the contract instance address in the constructor
   * constructor(address _addr) public { CALLED_INSTANCE = calledContract(_addr); } // lets create a
   * few public vars to store results so we know if we are // getting the callback return struct
   * SomeStruct { bool someBool; uint256 someUint; bytes32 someBytes32; } SomeStruct public
   * testCallbackReturns_; // create the function call to external contract. store return in vars
   * created // above. function makeTheCall() public { // lets call the contract and store returns
   * in to temp vars (bool _bool, uint256 _uint, bytes32 _bytes32) = CALLED_INSTANCE.testReturns();
   * // lets write those temp vars to state testCallbackReturns_.someBool = _bool;
   * testCallbackReturns_.someUint = _uint; testCallbackReturns_.someBytes32 = _bytes32; } }
   *
   * contract calledContract { function testReturns() external pure returns(bool, uint256, bytes32)
   * { return(true, 314159, 0x123456); } }
   */
  @Test
  public void internalTransactionAsInstanceTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    byte[] calledContractAddress = deployCalledContractandGetItsAddress();
    byte[] callerContractAddress = deployCallerContractAndGetItsAddress(calledContractAddress);

    /* =================================== CALL makeTheCall =================================== */
    byte[] triggerData1 = GVMTestUtils.parseAbi("makeTheCall()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            callerContractAddress, triggerData1,
            0, 100000000, deposit, null);

    /* =================================== CALL testCallbackReturns_ to check data =================================== */
    byte[] triggerData2 = GVMTestUtils.parseAbi("testCallbackReturns_()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            callerContractAddress, triggerData2,
            0, 100000000, deposit, null);

    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000001"
            + "000000000000000000000000000000000000000000000000000000000004cb2f"
            + "0000000000000000000000000000000000000000000000000000000000123456");


  }


  // Just for the caller/called example above
  private byte[] deployCalledContractandGetItsAddress()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "calledContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":true,\"inputs\":[],\"name\":\"testReturns\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"},"
            + "{\"name\":\"\",\"type\":\"uint256\"},{\"name\":\"\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"pure\","
            + "\"type\":\"function\"}]";
    String code =
        "608060405234801561001057600080fd5b5060d58061001f6000396000f300608060405260043610603f576000357c0"
            + "100000000000000000000000000000000000000000000000000000000900463ffffffff1680631a483b8c146044575b600080fd5"
            + "b348015604f57600080fd5b5060566086565b60405180841515151581526020018381526020018260001916600019168152602001"
            + "935050505060405180910390f35b600080600060016204cb2f621234568191508060010290509250925092509091925600a165627a"
            + "7a72305820040808e22827b01e497bf99a0ddd72084c95a3fa9bc8737fb022594c7656f00a0029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return GVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null,
            deposit, null);
  }

  // Just for the caller/called example above
  private byte[] deployCallerContractAndGetItsAddress(byte[] calledContractAddress)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "calledContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[],\"name\":\"makeTheCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":"
            + "\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"testCallbackReturns_\",\"outputs\":"
            + "[{\"name\":\"someBool\",\"type\":\"bool\"},{\"name\":\"someUint\",\"type\":\"uint256\"},{\"name\":\"someBytes32\",\"type\":"
            + "\"bytes32\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"name\":\"_addr\",\"type\":"
            + "\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String code =
        "608060405234801561001057600080fd5b5060405160208061029983398101806040528101908080519060200190929190505050806000806101"
            + "000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506102168"
            + "06100836000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff"
            + "1680633855721a14610051578063dda810ec14610068575b600080fd5b34801561005d57600080fd5b506100666100ad565b005b34801561007457600080fd5"
            + "b5061007d6101c5565b60405180841515151581526020018381526020018260001916600019168152602001935050505060405180910390f35b6000806000806"
            + "0009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16631a483b8c604051816"
            + "3ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401606060405180830381600087803b158015610137576000"
            + "80fd5b505af115801561014b573d6000803e3d6000fd5b505050506040513d606081101561016157600080fd5b810190808051906020019092919080519060200"
            + "1909291908051906020019092919050505092509250925082600160000160006101000a81548160ff0219169083151502179055508160018001819055508060016"
            + "002018160001916905550505050565b60018060000160009054906101000a900460ff169080600101549080600201549050835600a165627a7a72305820afe0957a"
            + "5188a2329cea5d678a10b01436ab68941b47259fc16ae84985c1abce0029" + Hex
            .toHexString(new DataWord(calledContractAddress).getData());
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return GVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null,
            deposit, null);
  }


  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

}
