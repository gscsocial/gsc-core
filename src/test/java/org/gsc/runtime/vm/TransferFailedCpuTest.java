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

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.gsc.runtime.GVMTestUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.runtime.GVMTestResult;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.ReceiptWrapper;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;
import org.gsc.wallet.common.client.utils.AbiUtil;

public class TransferFailedCpuTest extends VMTestBase {
/*
pragma confirmed ^0.5.4;
contract CpuOfTransferFailedTest {
    constructor() payable public {

    }
    // InsufficientBalance
    function testTransferGscInsufficientBalance() payable public{
        msg.sender.transfer(10);
    }

    function testSendGscInsufficientBalance() payable public{
        msg.sender.send(10);
    }

    function testTransferTokenInsufficientBalance(grcToken tokenId) payable public{
        msg.sender.transferToken(10, tokenId);
    }

    function testCallGscInsufficientBalance(address payable caller) public {
        caller.call.value(10)(abi.encodeWithSignature("test()"));
    }

    function testCreateGscInsufficientBalance() payable public {
        (new Caller).value(10)();
    }

    // NonexistentTarget
    function testTransferGscNonexistentTarget(address payable nonexistentTarget) payable public {
        require(address(this).balance >= 10);
        nonexistentTarget.transfer(10);
    }

    function testTransferTokenNonexistentTarget(address payable nonexistentTarget, grcToken tokenId) payable public {
        require(address(this).balance >= 10);
        nonexistentTarget.transferToken(10, tokenId);
    }

    function testCallGscNonexistentTarget(address payable nonexistentTarget) payable public {
        require(address(this).balance >= 10);
        nonexistentTarget.call.value(10)(abi.encodeWithSignature("test()"));
    }

    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }

    // target is self
    function testTransferGscSelf() payable public{
        require(address(this).balance >= 10);
        address payable self = address(uint160(address(this)));
        self.transfer(10);
    }

    function testSendGscSelf() payable public{
        require(address(this).balance >= 10);
        address payable self = address(uint160(address(this)));
        self.send(10);
    }

    function testTransferTokenSelf(grcToken tokenId) payable public{
        require(address(this).balance >= 10);
        address payable self = address(uint160(address(this)));
        self.transferToken(10, tokenId);
    }
}



contract Caller {
    constructor() payable public {}
    function test() payable public {}
}
 */

/*
// 0.4.25
contract CpuOfTransferFailedTest {

    constructor() payable public {

    }

    // InsufficientBalance
    function testTransferGscInsufficientBalance() payable public{
        msg.sender.transfer(10);
    }

    function testSendGscInsufficientBalance() payable public{
        msg.sender.send(10);
    }

    function testTransferTokenInsufficientBalance(grcToken tokenId) payable public{
        msg.sender.transferToken(10, tokenId);
    }

    function testCallGscInsufficientBalance(address caller) payable public {
        caller.call.value(10)(abi.encodeWithSignature("test()"));
    }

    function testCreateGscInsufficientBalance() payable public {
        (new Caller).value(10)();
    }

    // NonexistentTarget
    function testTransferGscNonexistentTarget(address nonexistentTarget) payable public {
        require(address(this).balance >= 10);
        nonexistentTarget.transfer(10);
    }

    function testTransferTokenNonexistentTarget(address nonexistentTarget, grcToken tokenId) payable public {
        require(address(this).balance >= 10);
        nonexistentTarget.transferToken(10, tokenId);
    }

    function testCallGscNonexistentTarget(address nonexistentTarget) public {
        require(address(this).balance >= 10);
        nonexistentTarget.call.value(10)(abi.encodeWithSignature("test()"));
    }

    function testSuicideNonexistentTarget(address nonexistentTarget) public {
         selfdestruct(nonexistentTarget);
    }

    // target is self
    function testTransferGscSelf() payable public{
        require(address(this).balance >= 10);
        address self = address(uint160(address(this)));
        self.transfer(10);
    }

    function testSendGscSelf() payable public{
        require(address(this).balance >= 10);
        address self = address(uint160(address(this)));
        self.send(10);
    }

    function testTransferTokenSelf(grcToken tokenId) payable public{
        require(address(this).balance >= 10);
        address self = address(uint160(address(this)));
        self.transferToken(10, tokenId);
    }
}

contract Caller {
    constructor() payable public {}
    function test() payable public {}
}
 */

  @Data
  @AllArgsConstructor
  @ToString
  static class TestCase {
    String method;
    List<Object> params;
    boolean allCpu;
    contractResult receiptResult;
  }

  private static final String nonExistAddress = "GSCZpKQ6KvUua3r4UPhG99WEurRZxkh7rrA5";  // 21 char

  TestCase[] testCasesAfterAllowGvmConstantinop = {
      new TestCase("testTransferGscSelf()", Collections.emptyList(), false, contractResult.TRANSFER_FAILED),
      new TestCase("testSendGscSelf()", Collections.emptyList(), false, contractResult.TRANSFER_FAILED),
      new TestCase("testSuicideNonexistentTarget(address)", Collections.singletonList(nonExistAddress), false, contractResult.TRANSFER_FAILED),
      new TestCase("testTransferGscNonexistentTarget(address)", Collections.singletonList(nonExistAddress), false, contractResult.TRANSFER_FAILED),
      new TestCase("testCallGscNonexistentTarget(address)", Collections.singletonList(nonExistAddress), false, contractResult.TRANSFER_FAILED),
  };

  TestCase[] testCasesBeforeAllowGvmConstantinop = {
      new TestCase("testTransferGscSelf()", Collections.emptyList(), true, contractResult.UNKNOWN),
      new TestCase("testSendGscSelf()", Collections.emptyList(), true, contractResult.UNKNOWN),
      new TestCase("testSuicideNonexistentTarget(address)", Collections.singletonList(nonExistAddress), true, contractResult.UNKNOWN),
      new TestCase("testTransferGscNonexistentTarget(address)", Collections.singletonList(nonExistAddress), true, contractResult.UNKNOWN),
      new TestCase("testCallGscNonexistentTarget(address)", Collections.singletonList(nonExistAddress), true, contractResult.UNKNOWN),
  };

  TestCase[] testCasesInsufficientBalance = {
      new TestCase("testTransferGscInsufficientBalance()", Collections.emptyList(), false, contractResult.REVERT),
      new TestCase("testSendGscInsufficientBalance()", Collections.emptyList(), false, contractResult.SUCCESS),
      new TestCase("testCreateGscInsufficientBalance()", Collections.emptyList(), false, contractResult.REVERT),
      new TestCase("testCallGscInsufficientBalance()", Collections.emptyList(), false, contractResult.REVERT),
      new TestCase("testTransferTokenInsufficientBalance(grcToken)", Collections.singletonList(1000001), false, contractResult.REVERT),
  };



  @Test
  public void testTransferFailedAfterAllowGvmConstantinopl()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    VMConfig.initAllowGvmTransferGrc10(1);
    VMConfig.initAllowGvmConstantinople(1);

    String contractName = "CpuOfTransferFailedTest";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[]";
    String code = "6080604052610a58806100136000396000f3fe6080604052600436106100a75760003560e01c806334914b461161006457806334914b46146101c45780636c0fd95f146101ce5780637a1e10a4146101d8578063c74a1591146101e2578063d825ff5c14610210578063e2e37f6114610254576100a7565b80630ec41f66146100ac5780630f0a1a70146100da57806317b6ad5b146100e4578063226d33e914610128578063271481ec146101765780632e72d11114610180575b600080fd5b6100d8600480360360208110156100c257600080fd5b81019080803590602001909291905050506102bf565b005b6100e2610371565b005b610126600480360360208110156100fa57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506103e6565b005b6101746004803603604081101561013e57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506103ff565b005b61017e6104ac565b005b6101c26004803603602081101561019657600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506104f6565b005b6101cc610566565b005b6101d6610595565b005b6101e06105f9565b005b61020e600480360360208110156101f857600080fd5b8101908080359060200190929190505050610632565b005b6102526004803603602081101561022657600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506106b9565b005b34801561026057600080fd5b50d3801561026d57600080fd5b50d2801561027a57600080fd5b506102bd6004803603602081101561029157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061081a565b005b600a3073ffffffffffffffffffffffffffffffffffffffff163110156102e457600080fd5b60003090508073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290848015801561031857600080fd5b50806780000000000000001115801561033057600080fd5b5080620f42401015801561034357600080fd5b5060405160006040518083038185878a8ad094505050505015801561036c573d6000803e3d6000fd5b505050565b600a3073ffffffffffffffffffffffffffffffffffffffff1631101561039657600080fd5b60003090508073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f193505050501580156103e2573d6000803e3d6000fd5b5050565b8073ffffffffffffffffffffffffffffffffffffffff16ff5b600a3073ffffffffffffffffffffffffffffffffffffffff1631101561042457600080fd5b8173ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290838015801561045357600080fd5b50806780000000000000001115801561046b57600080fd5b5080620f42401015801561047e57600080fd5b5060405160006040518083038185878a8ad09450505050501580156104a7573d6000803e3d6000fd5b505050565b3373ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f193505050501580156104f3573d6000803e3d6000fd5b50565b600a3073ffffffffffffffffffffffffffffffffffffffff1631101561051b57600080fd5b8073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f19350505050158015610562573d6000803e3d6000fd5b5050565b600a60405161057490610956565b6040518091039082f08015801561058f573d6000803e3d6000fd5b50905050565b600a3073ffffffffffffffffffffffffffffffffffffffff163110156105ba57600080fd5b60003090508073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f193505050505050565b3373ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f1935050505050565b3373ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290838015801561066157600080fd5b50806780000000000000001115801561067957600080fd5b5080620f42401015801561068c57600080fd5b5060405160006040518083038185878a8ad09450505050501580156106b5573d6000803e3d6000fd5b5050565b600a3073ffffffffffffffffffffffffffffffffffffffff163110156106de57600080fd5b8073ffffffffffffffffffffffffffffffffffffffff16600a6040516024016040516020818303038152906040527ff8a8fd6d000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff83818316178352505050506040518082805190602001908083835b602083106107ad578051825260208201915060208101905060208303925061078a565b6001836020036101000a03801982511681845116808217855250505050505090500191505060006040518083038185875af1925050503d806000811461080f576040519150601f19603f3d011682016040523d82523d6000602084013e610814565b606091505b50505050565b8073ffffffffffffffffffffffffffffffffffffffff16600a6040516024016040516020818303038152906040527ff8a8fd6d000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff83818316178352505050506040518082805190602001908083835b602083106108e957805182526020820191506020810190506020830392506108c6565b6001836020036101000a03801982511681845116808217855250505050505090500191505060006040518083038185875af1925050503d806000811461094b576040519150601f19603f3d011682016040523d82523d6000602084013e610950565b606091505b50505050565b6099806109638339019056fe608060405260888060116000396000f3fe608060405260043610601c5760003560e01c8063f8a8fd6d146021575b600080fd5b60276029565b005b56fea265627a7a7230582094509b1f95f08e0a48b00418ebbdd67c65bbdb2b443d70e453abf630ba59bb4264736f6c63782a302e352e392d646576656c6f702e323031392e392e352b636f6d6d69742e31643731383164302e6d6f64005aa265627a7a723058202b4945e505c6820fd49b798a211626deb038b8112d7ad6bc67fd6c1b5f86661c64736f6c63782a302e352e392d646576656c6f702e323031392e392e352b636f6d6d69742e31643731383164302e6d6f64005a";
    long value = 100000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

//     deploy contract
    Transaction trx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent, null);
    byte[] addressWithSufficientBalance = Wallet.generateContractAddress(trx);
    runtime = GVMTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesAfterAllowGvmConstantinop) {
      checkResult(testCase, addressWithSufficientBalance);
    }

    trx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, 0, fee, consumeUserResourcePercent, null);
    byte[] addressWithoutBalance = Wallet.generateContractAddress(trx);
    runtime = GVMTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesInsufficientBalance) {
      checkResult(testCase, addressWithoutBalance);
    }
  }

  @Test
  public void testTransferFailedBeforeAllowGvmConstantinopl()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    VMConfig.initAllowGvmTransferGrc10(1);
    VMConfig.initAllowGvmConstantinople(0);

    String contractName = "CpuOfTransferFailedTest";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[]";
    String code = "60806040526109a2806100136000396000f3006080604052600436106100ba576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680630f0a1a70146100bf57806317b6ad5b146100c9578063271481ec146101265780632e72d1111461013057806334914b46146101665780633f37e6961461017057806348fdb795146101b05780636c0fd95f146101d05780637a1e10a4146101da578063c38384cf146101e4578063d825ff5c14610204578063e2e37f6114610261575b600080fd5b6100c7610297565b005b3480156100d557600080fd5b50d380156100e257600080fd5b50d280156100ef57600080fd5b50610124600480360381019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061030e565b005b61012e610327565b005b610164600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610371565b005b61016e6103e3565b005b6101ae600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919050505061040e565b005b6101ce600480360381019080803590602001909291905050506104bd565b005b6101d8610544565b005b6101e26105aa565b005b610202600480360381019080803590602001909291905050506105e3565b005b34801561021057600080fd5b50d3801561021d57600080fd5b50d2801561022a57600080fd5b5061025f600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610697565b005b610295600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506107cd565b005b6000600a3073ffffffffffffffffffffffffffffffffffffffff1631101515156102c057600080fd5b3090508073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f1935050505015801561030a573d6000803e3d6000fd5b5050565b8073ffffffffffffffffffffffffffffffffffffffff16ff5b3373ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f1935050505015801561036e573d6000803e3d6000fd5b50565b600a3073ffffffffffffffffffffffffffffffffffffffff16311015151561039857600080fd5b8073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f193505050501580156103df573d6000803e3d6000fd5b5050565b600a6103ed6108dc565b6040518091039082f080158015610408573d6000803e3d6000fd5b50905050565b600a3073ffffffffffffffffffffffffffffffffffffffff16311015151561043557600080fd5b8173ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290838015801561046457600080fd5b50806780000000000000001115801561047c57600080fd5b5080620f42401015801561048f57600080fd5b5060405160006040518083038185878a8ad09450505050501580156104b8573d6000803e3d6000fd5b505050565b3373ffffffffffffffffffffffffffffffffffffffff166108fc600a908115029083801580156104ec57600080fd5b50806780000000000000001115801561050457600080fd5b5080620f42401015801561051757600080fd5b5060405160006040518083038185878a8ad0945050505050158015610540573d6000803e3d6000fd5b5050565b6000600a3073ffffffffffffffffffffffffffffffffffffffff16311015151561056d57600080fd5b3090508073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f193505050505050565b3373ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290604051600060405180830381858888f1935050505050565b6000600a3073ffffffffffffffffffffffffffffffffffffffff16311015151561060c57600080fd5b3090508073ffffffffffffffffffffffffffffffffffffffff166108fc600a9081150290848015801561063e57600080fd5b50806780000000000000001115801561065657600080fd5b5080620f42401015801561066957600080fd5b5060405160006040518083038185878a8ad0945050505050158015610692573d6000803e3d6000fd5b505050565b600a3073ffffffffffffffffffffffffffffffffffffffff1631101515156106be57600080fd5b8073ffffffffffffffffffffffffffffffffffffffff16600a6040516024016040516020818303038152906040527ff8a8fd6d000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505060405180828051906020019080838360005b8381101561078757808201518184015260208101905061076c565b50505050905090810190601f1680156107b45780820380516001836020036101000a031916815260200191505b5091505060006040518083038185875af1925050505050565b8073ffffffffffffffffffffffffffffffffffffffff16600a6040516024016040516020818303038152906040527ff8a8fd6d000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505060405180828051906020019080838360005b8381101561089657808201518184015260208101905061087b565b50505050905090810190601f1680156108c35780820380516001836020036101000a031916815260200191505b5091505060006040518083038185875af1925050505050565b604051608b806108ec8339019056006080604052607a8060116000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063f8a8fd6d146044575b600080fd5b604a604c565b005b5600a165627a7a72305820718a7c82271e1aacfbbcec07db67eb9c7cfeef39c5bf5b68d0c82992209cd99e0029a165627a7a7230582086702245f1b1d0f75f5b4fd38b0523ee9920304dd778d30fc3f2d3097f96fc7d0029";
    long value = 100000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

//     deploy contract
    Transaction trx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent, null);
    byte[] addressWithSufficientBalance = Wallet.generateContractAddress(trx);
    System.out.println(Hex.toHexString(addressWithSufficientBalance));
    runtime = GVMTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesBeforeAllowGvmConstantinop) {
      checkResult(testCase, addressWithSufficientBalance);
    }


    trx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, 0, fee, consumeUserResourcePercent, null);
    byte[] addressWithoutBalance = Wallet.generateContractAddress(trx);
    runtime = GVMTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesInsufficientBalance) {
      checkResult(testCase, addressWithoutBalance);
    }
  }

  private void checkResult(TestCase testCase, byte[] factoryAddress)
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    String hexInput = AbiUtil.parseMethod(testCase.getMethod(), testCase.getParams());
    long fee = 100000000;
    long allCpu = 1000000;
    GVMTestResult result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    ProgramResult programResult = result.getRuntime().getResult();
    ReceiptWrapper receiptWrapper = result.getReceipt();
    Assert.assertEquals(receiptWrapper.getResult(), testCase.getReceiptResult(),
        testCase.getMethod());
    if (testCase.allCpu) {
      Assert.assertEquals(programResult.getCpuUsed(), 10000000, testCase.getMethod());
    } else {
      Assert.assertTrue(programResult.getCpuUsed() < allCpu, testCase.getMethod());
    }
  }
}
