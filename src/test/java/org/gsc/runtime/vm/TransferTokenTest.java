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

import com.google.protobuf.ByteString;

import java.io.File;

import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.GVMTestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.config.VMConfig;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
public class TransferTokenTest {

    private static Runtime runtime;
    private static Manager dbManager;
    private static GSCApplicationContext context;
    private static Application appT;
    private static DepositImpl deposit;
    private static final String dbPath = "db_TransferTokenTest";
    private static final String OWNER_ADDRESS;
    private static final String TRANSFER_TO;
    private static final long TOTAL_SUPPLY = 1000_000_000L;
    private static final int GSC_NUM = 10;
    private static final int NUM = 1;
    private static final long START_TIME = 1;
    private static final long END_TIME = 2;
    private static final int VOTE_SCORE = 2;
    private static final String DESCRIPTION = "GSC";
    private static final String URL = "https://gsc.social";
    private static AccountWrapper ownerWrapper;

    static {
        Args.setParam(new String[]{"--db-directory", dbPath, "--debug"}, Constant.TEST_NET_CONF);
        context = new GSCApplicationContext(DefaultConfig.class);
        appT = ApplicationFactory.create(context);
        OWNER_ADDRESS = Wallet.getAddressPreFixString() + "0ef66235aec0b2bd93bf9bebc1279175e478b9e4";
        TRANSFER_TO = Wallet.getAddressPreFixString() + "6d1d156bd87069262ede95c4f822eea0d6a6a7c7";
        dbManager = context.getBean(Manager.class);
        deposit = DepositImpl.createRoot(dbManager);
        deposit.createAccount(Hex.decode(TRANSFER_TO), AccountType.Normal);
        deposit.addBalance(Hex.decode(TRANSFER_TO), 10);
        deposit.commit();
        ownerWrapper =
                new AccountWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                        ByteString.copyFromUtf8("owner"),
                        AccountType.AssetIssue);

        ownerWrapper.setBalance(1000_1000_1000L);
    }


    private long createAsset(String tokenName) {
        dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        VMConfig.initAllowGvmTransferGrc10(1);
        long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
        dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
        AssetIssueContract assetIssueContract =
                AssetIssueContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setName(ByteString.copyFrom(ByteArray.fromString(tokenName)))
                        .setId(Long.toString(id))
                        .setTotalSupply(TOTAL_SUPPLY)
                        .setGscNum(GSC_NUM)
                        .setNum(NUM)
                        .setStartTime(START_TIME)
                        .setEndTime(END_TIME)
                        .setVoteScore(VOTE_SCORE)
                        .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
                        .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
                        .build();
        AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
        dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

        ownerWrapper.addAssetV2(ByteArray.fromString(String.valueOf(id)), 100_000_000);
        dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
        return id;
    }


    /**
     * pragma confirmed ^0.4.24;
     * <p>
     * contract TokenTest{ constructor() public payable{} // positive case function
     * TransferTokenTo(address toAddress, grcToken id,uint256 amount) public payable{
     * toAddress.transferToken(amount,id); } function suicide(address toAddress) payable public{
     * selfdestruct(toAddress); } function get(grcToken grc) public payable returns(uint256){ return
     * address(this).tokenBalance(grc); } }
     * <p>
     * 1. deploy 2. trigger and internal transaction 3. suicide (all token)
     */
    @Test
    public void TransferTokenTest()
            throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
        /*  1. Test deploy with tokenValue and tokenId */
        long id = createAsset("testToken1");
        System.out.println("id: " + id);
        byte[] contractAddress = deployTransferTokenContract(id);
        deposit.commit();
        Assert.assertEquals(100,
                dbManager.getAccountStore().get(contractAddress).getAssetMapV2().get(String.valueOf(id))
                        .longValue());
        Assert.assertEquals(1000, dbManager.getAccountStore().get(contractAddress).getBalance());

        String selectorStr = "TransferTokenTo(address,grcToken,uint256)";
        String params = "00000000000000000001f80c6d1d156bd87069262ede95c4f822eea0d6a6a7c7" +
                Hex.toHexString(new DataWord(id).getData()) +
                "0000000000000000000000000000000000000000000000000000000000000009"; //TRANSFER_TO, 100001, 9
        System.out.println("- id: " + Hex.toHexString(new DataWord(id).getData()));
        System.out.println(params);
        byte[] triggerData = GVMTestUtils.parseAbi(selectorStr, params);

        /*  2. Test trigger with tokenValue and tokenId, also test internal transaction transferToken function */
        long triggerCallValue = 100;
        long feeLimit = 100000000;
        long tokenValue = 8;
        Transaction transaction = GVMTestUtils
                .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
                        triggerData,
                        triggerCallValue, feeLimit, tokenValue, id);
        runtime = GVMTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);

        org.testng.Assert.assertNull(runtime.getRuntimeError());
        Assert.assertEquals(100 + tokenValue - 9,
                dbManager.getAccountStore().get(contractAddress).getAssetMapV2().get(String.valueOf(id))
                        .longValue());
        Assert.assertEquals(9, dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getAssetMapV2()
                .get(String.valueOf(id)).longValue());

        /*   suicide test  */
        // create new token: testToken2
        long id2 = createAsset("testToken2");
        // add token balance for last created contract
        AccountWrapper changeAccountWrapper = dbManager.getAccountStore().get(contractAddress);
        changeAccountWrapper.addAssetAmountV2(String.valueOf(id2).getBytes(), 99, dbManager);
        dbManager.getAccountStore().put(contractAddress, changeAccountWrapper);
        String selectorStr2 = "suicide(address)";
        String params2 = "0000000000000000000000006d1d156bd87069262ede95c4f822eea0d6a6a7c7"; //TRANSFER_TO
        byte[] triggerData2 = GVMTestUtils.parseAbi(selectorStr2, params2);
        Transaction transaction2 = GVMTestUtils
                .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
                        triggerData2,
                        triggerCallValue, feeLimit, 0, id);
        runtime = GVMTestUtils.processTransactionAndReturnRuntime(transaction2, dbManager, null);
        org.testng.Assert.assertNull(runtime.getRuntimeError());
        Assert.assertEquals(100 + tokenValue - 9 + 9,
                dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getAssetMapV2()
                        .get(String.valueOf(id)).longValue());
        Assert.assertEquals(99, dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getAssetMapV2()
                .get(String.valueOf(id2)).longValue());
    }


    private byte[] deployTransferTokenContract(long id)
            throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
        String contractName = "TokenTest";
        byte[] address = Hex.decode(OWNER_ADDRESS);
        String ABI =
                "[]";

        String code =
                "6080604052610238806100136000396000f300608060405260043610610057576000357c010000000000000" +
                        "0000000000000000000000000000000000000000000900463ffffffff1680630807ef331461005c5780632378bec3146100a6578063dbc1f226146100da575b600080fd5b6100a4600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919080359060200190929190505050610110565b005b6100c460048036038101908080359060200190929190505050610198565b6040518082815260200191505060405180910390f35b61010e600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506101f3565b005b8273ffffffffffffffffffffffffffffffffffffffff166108fc829081150290848015801561013e57600080fd5b50806780000000000000001115801561015657600080fd5b5080620f42401015801561016957600080fd5b5060405160006040518083038185878a8ad0945050505050158015610192573d6000803e3d6000fd5b50505050565b60003073ffffffffffffffffffffffffffffffffffffffff1682801580156101bf57600080fd5b5080678000000000000000111580156101d757600080fd5b5080620f4240101580156101ea57600080fd5b50d19050919050565b8073ffffffffffffffffffffffffffffffffffffffff16ff00a165627a7a7230582097a092b9e9f3ab8a9d618998e087e163a28d1424d6b26bc7e178ad4d0db40ff80029";

        long value = 1000;
        long feeLimit = 100000000;
        long consumeUserResourcePercent = 0;
        long tokenValue = 100;
        long tokenId = id;

        byte[] contractAddress = GVMTestUtils
                .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
                        feeLimit, consumeUserResourcePercent, null, tokenValue, tokenId,
                        deposit, null);
        return contractAddress;
    }

    /**
     * contract TokenPerformanceTest{ uint256 public counter = 0; constructor() public payable{} //
     * positive case function TransferTokenTo(address toAddress, grcToken id,uint256 amount) public
     * payable{ while(true){ counter++; toAddress.transferToken(amount,id); } } }
     */
    @Test
    public void TransferTokenSingleInstructionTimeTest()
            throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
        long id = createAsset("testPerformanceToken");
        byte[] contractAddress = deployTransferTokenPerformanceContract(id);
        long triggerCallValue = 100;
        long feeLimit = 1000_000_000;
        long tokenValue = 0;
        String selectorStr = "trans(address,grcToken,uint256)";
        String params = "0000000000000000000000006d1d156bd87069262ede95c4f822eea0d6a6a7c7" +
                Hex.toHexString(new DataWord(id).getData()) +
                "0000000000000000000000000000000000000000000000000000000000000002"; //TRANSFER_TO, 100001, 9
        byte[] triggerData = GVMTestUtils.parseAbi(selectorStr, params);
        Transaction transaction = GVMTestUtils
                .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
                        triggerData,
                        triggerCallValue, feeLimit, tokenValue, id);
        long start = System.nanoTime() / 1000;

        runtime = GVMTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);
        long end = System.nanoTime() / 1000;
        System.err.println("running time:" + (end - start));
        Assert.assertTrue((end - start) < 50_0000);

    }


    private byte[] deployTransferTokenPerformanceContract(long id)
            throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
        String contractName = "TransferTokenPerformanceContract";
        byte[] address = Hex.decode(OWNER_ADDRESS);
        String ABI =
                "[]";
        String code =
                "6080604052600080556101b8806100176000396000f30060806040526004361061004c576000357c0100000" +
                        "000000000000000000000000000000000000000000000000000900463ffffffff1680630807ef33" +
                        "1461005157806361bc221a1461009b575b600080fd5b610099600480360381019080803573fffff" +
                        "fffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190803590" +
                        "602001909291905050506100e0565b005b3480156100a757600080fd5b50d380156100b45760008" +
                        "0fd5b50d280156100c157600080fd5b506100ca610186565b604051808281526020019150506040" +
                        "5180910390f35b5b6001156101815760008081548092919060010191905055508273fffffffffff" +
                        "fffffffffffffffffffffffffffff166108fc829081150290848015801561012757600080fd5b50" +
                        "806780000000000000001115801561013f57600080fd5b5080620f4240101580156101525760008" +
                        "0fd5b5060405160006040518083038185878a8ad094505050505015801561017b573d6000803e3d" +
                        "6000fd5b506100e1565b505050565b600054815600a165627a7a72305820a448a4d22277c1123c6" +
                        "d656ae64709b59a6f88e23113f7aeefb9a61ed0b2e3380029";

        long value = 1000;
        long feeLimit = 100000000;
        long consumeUserResourcePercent = 0;
        long tokenValue = 1000_000;
        long tokenId = id;

        byte[] contractAddress = GVMTestUtils
                .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
                        feeLimit, consumeUserResourcePercent, null, tokenValue, tokenId,
                        deposit, null);
        return contractAddress;
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
