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
import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.GVMTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.Runtime;
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
public class InternalTransactionCallTest {

  private Runtime runtime;
  private Manager dbManager;
  private GSCApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "db_InternalTransactionCallTest";
  private String OWNER_ADDRESS;
  private Application AppT;

  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.clearParam();
    Args.setParam(new String[]{"--db-directory", dbPath, "--support-constant", "--debug"},
        Constant.TEST_NET_CONF);

    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 100000000);
  }


  /**
   * contract A { uint256 public numberForB; address public senderForB; function callTest(address
   * bAddress, uint256 _number) { bAddress.call(bytes4(sha3("setValue(uint256)")), _number); // B's
   * storage is set, A is not modified }
   *
   * function callcodeTest(address bAddress, uint256 _number) { bAddress.callcode(bytes4(sha3("setValue(uint256)")),
   * _number); // A's storage is set, B is not modified }
   *
   * function delegatecallTest(address bAddress, uint256 _number) { bAddress.delegatecall(bytes4(sha3("setValue(uint256)")),
   * _number); // A's storage is set, B is not modified } }
   *
   * contract B { uint256 public numberForB; address public senderForB;
   *
   * function setValue(uint256 _number) { numberForB = _number; senderForB = msg.sender; //
   * senderForB is A if invoked by A's callTest. B's storage will be updated // senderForB is A if
   * invoked by A's callcodeTest. None of B's storage is updated // senderForB is OWNER if invoked
   * by A's delegatecallTest. None of B's storage is updated } }
   */


  /*
      A call B, anything belongs to A should not be changed, B should be changed.
      msg.sender for contractB should be A's address.
   */
  @Test
  public void callTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    byte[] contractBAddress = deployBContractAndGetItsAddress();
    byte[] contractAAddress = deployAContractandGetItsAddress();

    /* =================================== CALL callTest() to change B storage =================================== */
    String params =
        Hex.toHexString(new DataWord(new DataWord(contractBAddress).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = GVMTestUtils.parseAbi("callTest(address,uint256)", params);
    GVMTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
        contractAAddress, triggerData,
        0, 1000000000, deposit, null);

    /* =================================== CALL numberForB() to check A's numberForB =================================== */
    byte[] triggerData2 = GVMTestUtils.parseAbi("numberForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData2,
            0, 1000000000, deposit, null);
    // A should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* =================================== CALL senderForB() to check A's senderForB =================================== */
    byte[] triggerData3 = GVMTestUtils.parseAbi("senderForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData3,
            0, 1000000000, deposit, null);
    // A should be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* =================================== CALL numberForB() to check B's numberForB =================================== */
    byte[] triggerData4 = GVMTestUtils.parseAbi("numberForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData4,
            0, 1000000000, deposit, null);
    // B's numberForB should be changed to 3
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000003");

    /* =================================== CALL senderForB() to check B's senderForB =================================== */
    byte[] triggerData5 = GVMTestUtils.parseAbi("senderForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData5,
            0, 1000000000, deposit, null);
    // B 's senderForB should be A
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        Hex.toHexString(new DataWord(new DataWord(contractAAddress).getLast20Bytes()).getData()));
  }

  /*
     A delegatecall B, A should be changed, anything belongs to B should not be changed.
     msg.sender for contractB should be Caller(OWNER_ADDRESS), but this value will not be effected in B's senderForB since we use delegatecall.
     We store it in A's senderForB.
   */
  @Test
  public void delegateCallTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    byte[] contractBAddress = deployBContractAndGetItsAddress();
    byte[] contractAAddress = deployAContractandGetItsAddress();
    /* =================================== CALL delegatecallTest() to change B storage =================================== */
    String params =
        Hex.toHexString(new DataWord(new DataWord(contractBAddress).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = GVMTestUtils.parseAbi("delegatecallTest(address,uint256)", params);
    GVMTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
        contractAAddress, triggerData,
        0, 1000000000, deposit, null);

    /* =================================== CALL numberForB() to check A's numberForB =================================== */
    byte[] triggerData2 = GVMTestUtils.parseAbi("numberForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData2,
            0, 1000000000, deposit, null);
    // A should be changed to 3
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000003");

    /* =================================== CALL senderForB() to check A's senderForB =================================== */
    byte[] triggerData3 = GVMTestUtils.parseAbi("senderForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData3,
            0, 1000000000, deposit, null);
    // A's senderForB should be changed to caller's contract Address (OWNER_ADDRESS)
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        Hex.toHexString(new DataWord(new DataWord(OWNER_ADDRESS).getLast20Bytes()).getData()));

    /* =================================== CALL numberForB() to check B's numberForB =================================== */
    byte[] triggerData4 = GVMTestUtils.parseAbi("numberForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData4,
            0, 1000000000, deposit, null);
    // B's numberForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* =================================== CALL senderForB() to check B's senderForB =================================== */
    byte[] triggerData5 = GVMTestUtils.parseAbi("senderForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData5,
            0, 1000000000, deposit, null);
    // B 's senderForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

  }

  /*
     A callcode B, A should be changed, anything belongs to B should not be changed.
     msg.sender for contractB should be A, but this value will not be effected in B's senderForB since we use callcode.
     We store it in A's senderForB.
   */
  @Test
  public void callCodeTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    byte[] contractBAddress = deployBContractAndGetItsAddress();
    byte[] contractAAddress = deployAContractandGetItsAddress();
    /* =================================== CALL callcodeTest() to change B storage =================================== */
    String params =
        Hex.toHexString(new DataWord(new DataWord(contractBAddress).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = GVMTestUtils.parseAbi("callcodeTest(address,uint256)", params);
    GVMTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
        contractAAddress, triggerData,
        0, 1000000000, deposit, null);

    /* =================================== CALL numberForB() to check A's numberForB =================================== */
    byte[] triggerData2 = GVMTestUtils.parseAbi("numberForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData2,
            0, 1000000000, deposit, null);
    // A should be changed to 3
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000003");

    /* =================================== CALL senderForB() to check A's senderForB =================================== */
    byte[] triggerData3 = GVMTestUtils.parseAbi("senderForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData3,
            0, 1000000000, deposit, null);
    // A's senderForB should be changed to A's contract Address
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        Hex.toHexString(new DataWord(new DataWord(contractAAddress).getLast20Bytes()).getData()));

    /* =================================== CALL numberForB() to check B's numberForB =================================== */
    byte[] triggerData4 = GVMTestUtils.parseAbi("numberForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData4,
            0, 1000000000, deposit, null);
    // B's numberForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* =================================== CALL senderForB() to check B's senderForB =================================== */
    byte[] triggerData5 = GVMTestUtils.parseAbi("senderForB()", "");
    runtime = GVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData5,
            0, 1000000000, deposit, null);
    // B 's senderForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");
  }

  @Test
  public void staticCallTest() {
    //TODO: need to implement this
  }


  // Just for the AB example above
  public byte[] deployAContractandGetItsAddress()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "AContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\"_number\",\"type\":\"uint256\"}],"
            + "\"name\":\"delegatecallTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
            + "\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\"_number\",\"type\":\"uint256\"}],\"name\":\"callTest\","
            + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
            + "\"senderForB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
            + "\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\"_number\",\"type\":\"uint256\"}],"
            + "\"name\":\"callcodeTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,"
            + "\"inputs\":[],\"name\":\"numberForB\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\","
            + "\"type\":\"function\"}]";
    String code =
        "608060405234801561001057600080fd5b506102d4806100206000396000f3006080604052600436106100535763ffffffff60e060020a6000350"
            + "416633da5d187811461005857806343c3a43a1461007e578063b053ebd4146100a2578063d7d21f5b146100d3578063dd92afef146100f7575b600080fd5b"
            + "34801561006457600080fd5b5061007c600160a060020a036004351660243561011e565b005b34801561008a57600080fd5b5061007c600160a060020a036"
            + "0043516602435610199565b3480156100ae57600080fd5b506100b7610216565b60408051600160a060020a039092168252519081900360200190f35b3480"
            + "156100df57600080fd5b5061007c600160a060020a0360043516602435610225565b34801561010357600080fd5b5061010c6102a2565b604080519182525"
            + "19081900360200190f35b81600160a060020a031660405180807f73657456616c75652875696e743235362900000000000000000000000000000081525060"
            + "11019050604051809103902060e060020a9004826040518263ffffffff1660e060020a02815260040180828152602001915050600060405180830381865af4"
            + "505050505050565b81600160a060020a031660405180807f73657456616c75652875696e743235362900000000000000000000000000000081525060110190"
            + "50604051809103902060e060020a9004826040518263ffffffff1660e060020a028152600401808281526020019150506000604051808303816000875af150"
            + "5050505050565b600154600160a060020a031681565b81600160a060020a031660405180807f73657456616c75652875696e74323536290000000000000000"
            + "000000000000008152506011019050604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082815260200191505060"
            + "00604051808303816000875af2505050505050565b600054815600a165627a7a723058206d36ef7c6f6d387ad915f299e715c9b360f3719843a1113badb28b"
            + "6595e66c1e0029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;
    String libraryAddressPair = null;

    byte[] contractAddress = GVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);
    return contractAddress;
  }

  // Just for the AB example above
  public byte[] deployBContractAndGetItsAddress()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "BContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[{\"name\":\"_number\",\"type\":\"uint256\"}],\"name\":\"setValue\","
            + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,"
            + "\"inputs\":[],\"name\":\"senderForB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,"
            + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"numberForB\","
            + "\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
            + "\"function\"}]";
    String code =
        "608060405234801561001057600080fd5b5061014c806100206000396000f3006080604052600436106100565763ffffffff7"
            + "c010000000000000000000000000000000000000000000000000000000060003504166355241077811461005b578063b053ebd4146100"
            + "75578063dd92afef146100b3575b600080fd5b34801561006757600080fd5b506100736004356100da565b005b34801561008157600080"
            + "fd5b5061008a6100fe565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b348015"
            + "6100bf57600080fd5b506100c861011a565b60408051918252519081900360200190f35b6000556001805473ffffffffffffffffffffff"
            + "ffffffffffffffffff191633179055565b60015473ffffffffffffffffffffffffffffffffffffffff1681565b600054815600a165627a7"
            + "a72305820e2c513cf46bb32018879ec48f8fe264c985b6d2c7a853a578f4f56583fe1ffb80029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;
    String libraryAddressPair = null;

    byte[] contractAddress = GVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);
    return contractAddress;
  }


  /**
   * Release resources.
   */
  @After
  public void destroy() {
    context.destroy();
    AppT.shutdown();
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.warn("Release resources failure.");
    }

  }
}
