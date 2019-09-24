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

import java.math.BigInteger;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.GVMTestUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.runtime.GVMTestResult;
import org.gsc.runtime.config.VMConfig;
import org.gsc.core.Wallet;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.wallet.common.client.utils.AbiUtil;

@Slf4j
public class ExtCodeHashTest extends VMTestBase {
/*
pragma confirmed ^0.5.0;

contract TestExtCodeHash {

    function getCodeHashByAddr(address _addr) public view returns (bytes32 _hash) {
        assembly {
                _hash := extcodehash(_addr)
            }
    }
    function getCodeHashByUint(uint256 _addr) public view returns (bytes32 _hash) {
        assembly {
                _hash := extcodehash(_addr)
            }
    }
}
*/

    @Test
    public void testExtCodeHash()
            throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
        VMConfig.initAllowGvmConstantinople(1);
        String contractName = "TestExtCodeHash";
        byte[] address = Hex.decode(OWNER_ADDRESS);
        String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"_addr\",\"type\":\"uint256\"}],\"name\":\"getCodeHashByUint\",\"outputs\":[{\"name\":\"_hash\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"getCodeHashByAddr\",\"outputs\":[{\"name\":\"_hash\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}]";
        String factoryCode = "608060405234801561001057600080fd5b5061010d806100206000396000f3fe6080604052348015600f57600080fd5b506004361060325760003560e01c80637b77fd191460375780637d5e422d146076575b600080fd5b606060048036036020811015604b57600080fd5b810190808035906020019092919050505060cb565b6040518082815260200191505060405180910390f35b60b560048036036020811015608a57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505060d6565b6040518082815260200191505060405180910390f35b6000813f9050919050565b6000813f905091905056fea165627a7a723058200f30933f006db4e1adeee12c030b87e720dad3cb169769159fc56ec25d9af66f0029";
        long value = 0;
        long fee = 100000000;
        long consumeUserResourcePercent = 0;

        // deploy contract
        Transaction trx = GVMTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent, null);
        byte[] factoryAddress = Wallet.generateContractAddress(trx);
//        System.out.println("ContractAddress: " + Wallet.encode58Check(factoryAddress));
        runtime = GVMTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
        Assert.assertNull(runtime.getRuntimeError());

        // Trigger contract method: getCodeHashByAddr(address)
        String methodByAddr = "getCodeHashByAddr(address)";
        String nonexistentAccount = "GSCZpKQ6KvUua3r4UPhG99WEurRZxkh7rrA5";
        String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(nonexistentAccount));
        GVMTestResult result = GVMTestUtils
                .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        byte[] returnValue = result.getRuntime().getResult().getHReturn();
        // check deployed contract
        Assert.assertEquals(Hex.toHexString(returnValue),
                "0000000000000000000000000000000000000000000000000000000000000000");

        // trigger deployed contract
        String existentAccount = "GSCZV38r7WQiMaGmA56gRBVSf8sq8qbyckaT";
        hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(existentAccount));
        result = GVMTestUtils
                .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        returnValue = result.getRuntime().getResult().getHReturn();
        // check deployed contract
//        System.out.println(Hex.toHexString(returnValue));
        Assert.assertEquals(Hex.toHexString(returnValue),
                "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");

        // trigger deployed contract
        String methodByUint = "getCodeHashByUint(uint256)";
        byte[] fullHexAddr = new DataWord(factoryAddress).getData();
//        System.out.println("fullHexAddr: " + Hex.toHexString(fullHexAddr));

        hexInput = AbiUtil.parseMethod(methodByUint, Hex.toHexString(fullHexAddr), true);
//        System.out.println("hexInput: " + hexInput);
        result = GVMTestUtils
                .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        returnValue = result.getRuntime().getResult().getHReturn();
        // check deployed contract
        Assert.assertEquals(Hex.toHexString(returnValue),
                "0837cd5e284138b633cd976ea6fcb719d61d7bc33d946ec5a2d0c7da419a0bd4");

        // trigger deployed contract
//        System.out.println("factoryAddress: " + Hex.toHexString(factoryAddress));
        BigInteger bigIntAddr = new DataWord(factoryAddress).sValue();
//        System.out.println("bigIntAddr: " + bigIntAddr);
        String bigIntAddrChange = BigInteger.valueOf(2).pow(160).add(bigIntAddr).toString(16);
//        System.out.println("bigIntAddrChange: " + bigIntAddrChange + ": " + bigIntAddrChange.length());
//      factoryAddress: 01f80ce428f5b41079a1c43215a1c521c7c89230e00b1e
//      factoryAddress: a02e161d294be4d84afc53183d4321b6919e628111
//      bigIntAddr: 188587627837959727508491020852268122364426672209398558
//      bigIntAddr: 234103368705105480599528105116645787326478310080785
//      bigIntAddrChange: 1f80de428f5b41079a1c43215a1c521c7c89230e00b1e: 45
//      bigIntAddrChange: a12e161d294be4d84afc53183d4321b6919e628111: 42
        fullHexAddr = new DataWord("0" + bigIntAddrChange).getData();

        hexInput = AbiUtil.parseMethod(methodByUint, Hex.toHexString(fullHexAddr), true);
        result = GVMTestUtils
                .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        returnValue = result.getRuntime().getResult().getHReturn();
        // check deployed contract
        Assert.assertEquals(Hex.toHexString(returnValue),
                "0837cd5e284138b633cd976ea6fcb719d61d7bc33d946ec5a2d0c7da419a0bd4");

    }

}


