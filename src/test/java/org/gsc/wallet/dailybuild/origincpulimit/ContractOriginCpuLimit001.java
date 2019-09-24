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

package org.gsc.wallet.dailybuild.origincpulimit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Parameter;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractOriginCpuLimit001 {


    private final String testNetAccountKey = Configuration.getByPath("testng.conf")
            .getString("foundationAccount.key1");
    private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
    private Long maxFeeLimit = Configuration.getByPath("testng.conf")
            .getLong("defaultParameter.maxFeeLimit");
    private ManagedChannel channelConfirmed = null;

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;

    private ManagedChannel channelFull1 = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


    private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;

    private String fullnode = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(0);
    private String fullnode1 = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(1);


    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] grammarAddress3 = ecKey1.getAddress();
    String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


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
        PublicMethed.printAddress(testKeyForGrammarAddress3);
        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
                .usePlaintext(true)
                .build();
        blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    }


    //Origin_cpu_limit001,028,029
    @Test(enabled = true, description = "Boundary value and update test")
    public void testOrigin_cpu_limit001() {
        Assert.assertTrue(PublicMethed
                .sendcoin(grammarAddress3, 100000000000L, testNetAccountAddress, testNetAccountKey,
                        blockingStubFull));
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        String filePath = "src/test/resources/soliditycode_v0.5.4/contractOriginCpuLimit001.sol";
        String contractName = "findArgsContractTest";
        HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
        String code = retMap.get("byteCode").toString();
        String abi = retMap.get("abi").toString();
        String contractAddress = PublicMethed
                .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
                        0L, 100, -1, "0", 0,
                        null, testKeyForGrammarAddress3,
                        grammarAddress3, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        Assert.assertTrue(contractAddress == null);

        String contractAddress1 = PublicMethed
                .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
                        0L, 100, 0, "0", 0,
                        null, testKeyForGrammarAddress3,
                        grammarAddress3, blockingStubFull);

        Assert.assertTrue(contractAddress1 == null);

        byte[] contractAddress2 = PublicMethed
                .deployContract(contractName, abi, code, "", maxFeeLimit,
                        0L, 100, 9223372036854775807L, "0",
                        0, null, testKeyForGrammarAddress3,
                        grammarAddress3, blockingStubFull);

        PublicMethed.waitProduceNextBlock(blockingStubFull);

        Assert.assertFalse(PublicMethed.updateCpuLimit(contractAddress2, -1L,
                testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
        SmartContract smartContract = PublicMethed.getContract(contractAddress2, blockingStubFull);
        Assert.assertTrue(smartContract.getOriginCpuLimit() == 9223372036854775807L);

        Assert.assertFalse(PublicMethed.updateCpuLimit(contractAddress2, 0L,
                testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
        SmartContract smartContract1 = PublicMethed.getContract(contractAddress2, blockingStubFull);
        Assert.assertTrue(smartContract1.getOriginCpuLimit() == 9223372036854775807L);

        Assert.assertTrue(PublicMethed.updateCpuLimit(contractAddress2,
                9223372036854775807L, testKeyForGrammarAddress3,
                grammarAddress3, blockingStubFull));
        SmartContract smartContract2 = PublicMethed.getContract(contractAddress2, blockingStubFull);
        Assert.assertTrue(smartContract2.getOriginCpuLimit() == 9223372036854775807L);

        Assert.assertTrue(PublicMethed.updateCpuLimit(contractAddress2, 'c',
                testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
        SmartContract smartContract3 = PublicMethed.getContract(contractAddress2, blockingStubFull);
        Assert.assertEquals(smartContract3.getOriginCpuLimit(), 99);

        Assert.assertFalse(PublicMethed.updateCpuLimit(contractAddress2, 1L,
                testNetAccountKey, testNetAccountAddress, blockingStubFull));
        SmartContract smartContract4 = PublicMethed.getContract(contractAddress2, blockingStubFull);
        Assert.assertEquals(smartContract4.getOriginCpuLimit(), 99);
    }

    /**
     * constructor.
     */
    @AfterClass
    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (channelFull1 != null) {
            channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
