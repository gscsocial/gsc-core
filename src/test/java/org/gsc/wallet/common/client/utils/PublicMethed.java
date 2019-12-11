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

package org.gsc.wallet.common.client.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.primitives.Longs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.WalletClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.AccountNetMessage;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.api.GrpcAPI.DelegatedResourceList;
import org.gsc.api.GrpcAPI.DelegatedResourceMessage;
import org.gsc.api.GrpcAPI.EmptyMessage;
import org.gsc.api.GrpcAPI.ExchangeList;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.api.GrpcAPI.TransactionApprovedList;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.ECKey.ECDSASignature;
import org.gsc.crypto.Hash;
import org.gsc.utils.ByteArray;
import org.gsc.utils.ByteUtil;
import org.gsc.core.Wallet;
import org.gsc.keystore.WalletFile;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.CreateSmartContract.Builder;
import org.gsc.protos.Contract.UpdateSettingContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.DelegatedResourceAccountIndex;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.protos.Protocol.Key;
import org.gsc.protos.Protocol.Permission;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Result;
import org.gsc.protos.Protocol.TransactionInfo;

public class PublicMethed {

    Wallet wallet = new Wallet();
    private static final String FilePath = "Wallet";
    private static List<WalletFile> walletFile = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger("TestLogger");

    /**
     * constructor.
     */

    public static Boolean createAssetIssue(byte[] address, String name, Long totalSupply,
                                           Integer gscNum, Integer icoNum, Long startTime, Long endTime, Integer voteScore,
                                           String description, String url, Long freeAssetNetLimit, Long publicFreeAssetNetLimit,
                                           Long fronzenAmount, Long frozenDay, String priKey,
                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey = temKey;
        try {
            Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(address));
            builder.setName(ByteString.copyFrom(name.getBytes()));
            builder.setTotalSupply(totalSupply);
            builder.setGscNum(gscNum);
            builder.setNum(icoNum);
            builder.setStartTime(startTime);
            builder.setEndTime(endTime);
            builder.setVoteScore(voteScore);
            builder.setDescription(ByteString.copyFrom(description.getBytes()));
            builder.setUrl(ByteString.copyFrom(url.getBytes()));
            builder.setFreeAssetNetLimit(freeAssetNetLimit);
            builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
            Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder = Contract.AssetIssueContract
                    .FrozenSupply.newBuilder();
            frozenBuilder.setFrozenAmount(fronzenAmount);
            frozenBuilder.setFrozenDays(frozenDay);
            builder.addFrozenSupply(0, frozenBuilder);

            Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
            if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                logger.info("transaction == null");
                return false;
            }
            transaction = signTransaction(ecKey, transaction);

            GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

            return response.getResult();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * constructor.
     */

    public static Boolean createAssetIssue(byte[] address, String name, String abbreviation,
                                           Long totalSupply, Integer gscNum, Integer icoNum, Long startTime, Long endTime,
                                           Integer voteScore, String description, String url, Long freeAssetNetLimit,
                                           Long publicFreeAssetNetLimit, Long fronzenAmount, Long frozenDay, String priKey,
                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey = temKey;
        try {
            Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(address));
            builder.setName(ByteString.copyFrom(name.getBytes()));
            builder.setAbbr(ByteString.copyFrom(abbreviation.getBytes()));
            builder.setTotalSupply(totalSupply);
            builder.setGscNum(gscNum);
            builder.setNum(icoNum);
            builder.setStartTime(startTime);
            builder.setEndTime(endTime);
            builder.setVoteScore(voteScore);
            builder.setDescription(ByteString.copyFrom(description.getBytes()));
            builder.setUrl(ByteString.copyFrom(url.getBytes()));
            builder.setFreeAssetNetLimit(freeAssetNetLimit);
            builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
            Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
                    Contract.AssetIssueContract.FrozenSupply.newBuilder();
            frozenBuilder.setFrozenAmount(fronzenAmount);
            frozenBuilder.setFrozenDays(frozenDay);
            builder.addFrozenSupply(0, frozenBuilder);

            Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
            if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                logger.info("transaction == null");
                return false;
            }
            transaction = signTransaction(ecKey, transaction);

            GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

            return response.getResult();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * constructor.
     */

    public static Boolean createAssetIssue(byte[] address, String name, Long totalSupply,
                                           Integer gscNum, Integer icoNum, int precision, Long startTime, Long endTime,
                                           Integer voteScore,
                                           String description, String url, Long freeAssetNetLimit, Long publicFreeAssetNetLimit,
                                           Long fronzenAmount, Long frozenDay, String priKey,
                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey = temKey;
        try {
            Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(address));
            builder.setName(ByteString.copyFrom(name.getBytes()));
            builder.setTotalSupply(totalSupply);
            builder.setGscNum(gscNum);
            builder.setNum(icoNum);
            builder.setStartTime(startTime);
            builder.setEndTime(endTime);
            builder.setVoteScore(voteScore);
            builder.setPrecision(precision);
            builder.setDescription(ByteString.copyFrom(description.getBytes()));
            builder.setUrl(ByteString.copyFrom(url.getBytes()));
            builder.setFreeAssetNetLimit(freeAssetNetLimit);
            builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
            Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
                    Contract.AssetIssueContract.FrozenSupply.newBuilder();
            frozenBuilder.setFrozenAmount(fronzenAmount);
            frozenBuilder.setFrozenDays(frozenDay);
            builder.addFrozenSupply(0, frozenBuilder);

            Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
            if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                logger.info("transaction == null");
                return false;
            }
            transaction = signTransaction(ecKey, transaction);

            GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

            return response.getResult();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * constructor.
     */

    public static Return createAssetIssue2(byte[] address, String name, Long totalSupply,
                                           Integer gscNum, Integer icoNum, Long startTime, Long endTime, Integer voteScore,
                                           String description, String url, Long freeAssetNetLimit, Long publicFreeAssetNetLimit,
                                           Long fronzenAmount, Long frozenDay, String priKey,
                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey = temKey;
        //Protocol.Account search = queryAccount(ecKey, blockingStubFull);
        try {
            Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(address));
            builder.setName(ByteString.copyFrom(name.getBytes()));
            builder.setTotalSupply(totalSupply);
            builder.setGscNum(gscNum);
            builder.setNum(icoNum);
            builder.setStartTime(startTime);
            builder.setEndTime(endTime);
            builder.setVoteScore(voteScore);
            builder.setDescription(ByteString.copyFrom(description.getBytes()));
            builder.setUrl(ByteString.copyFrom(url.getBytes()));
            builder.setFreeAssetNetLimit(freeAssetNetLimit);
            builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
            //builder.setPublicFreeAssetNetUsage();
            //builder.setPublicLatestFreeNetTime();
            Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
                    Contract.AssetIssueContract.FrozenSupply.newBuilder();
            frozenBuilder.setFrozenAmount(fronzenAmount);
            frozenBuilder.setFrozenDays(frozenDay);
            builder.addFrozenSupply(0, frozenBuilder);

            TransactionExtention transactionExtention = blockingStubFull
                    .createAssetIssue2(builder.build());

            if (transactionExtention == null) {
                return transactionExtention.getResult();
            }
            Return ret = transactionExtention.getResult();
            if (!ret.getResult()) {
                System.out.println("Code = " + ret.getCode());
                System.out.println("Message = " + ret.getMessage().toStringUtf8());
                return ret;
            } else {
                System.out.println("Code = " + ret.getCode());
                System.out.println("Message = " + ret.getMessage().toStringUtf8());
            }
            Transaction transaction = transactionExtention.getTransaction();
            if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                System.out.println("Transaction is empty");
                return transactionExtention.getResult();
            }
            System.out.println(
                    "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
            transaction = signTransaction(ecKey, transaction);

            GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
            if (response.getResult() == false) {
                return response;
            } else {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
            //return false;
            return Return.getDefaultInstance();
        }
    }



    /**
     * constructor.
     */

    public static Account queryAccount(byte[] address, WalletGrpc
            .WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString addressBs = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBs).build();
        return blockingStubFull.getAccount(request);
    }

    public static Account queryAccountByAddress(byte[] address,
                                                WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString addressBs = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBs).build();
        return blockingStubFull.getAccount(request);
    }

    /**
     * constructor.
     */

    public static Account queryAccount(byte[] address, WalletConfirmedGrpc
            .WalletConfirmedBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString addressBs = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBs).build();
        return blockingStubFull.getAccount(request);
    }


    /**
     * constructor.
     */

    public static Protocol.Account queryAccount(String priKey,
                                                WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        byte[] address;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey = temKey;
        if (ecKey == null) {
            String pubKey = loadPubKey(); //04 PubKey[128]
            if (StringUtils.isEmpty(pubKey)) {
                logger.warn("Warning: QueryAccount failed, no wallet address !!");
                return null;
            }
            byte[] pubKeyAsc = pubKey.getBytes();
            byte[] pubKeyHex = Hex.decode(pubKeyAsc);
            ecKey = ECKey.fromPublicOnly(pubKeyHex);
        }
        return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
    }

    public static byte[] getAddress(ECKey ecKey) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);

        return ecKey.getAddress();
    }

    public static String loadPubKey() {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        char[] buf = new char[0x100];
        return String.valueOf(buf, 32, 130);
    }

    /**
     * constructor.
     */

    public static Protocol.Account grpcQueryAccount(byte[] address,
                                                    WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString addressBs = ByteString.copyFrom(address);
        Protocol.Account request = Protocol.Account.newBuilder().setAddress(addressBs).build();
        return blockingStubFull.getAccount(request);
    }

    /**
     * constructor.
     */

    public static Protocol.Block getBlock(long blockNum,
                                          WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
        builder.setNum(blockNum);
        return blockingStubFull.getBlockByNum(builder.build());
    }

    /**
     * constructor.
     */

    public static Protocol.Transaction signTransaction(ECKey ecKey,
                                                       Protocol.Transaction transaction) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        if (ecKey == null || ecKey.getPrivKey() == null) {
            //logger.warn("Warning: Can't sign,there is no private key !!");
            return null;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        logger.info("Txid in sign is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        return TransactionUtils.sign(transaction, ecKey);
    }

    /**
     * constructor.
     */

    public static boolean participateAssetIssue(byte[] to, byte[] assertName, long amount,
                                                byte[] from, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
                .newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(from);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);
        Contract.ParticipateAssetIssueContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.participateAssetIssue(contract);
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static Return participateAssetIssue2(byte[] to, byte[] assertName, long amount,
                                                byte[] from, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
                .newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(from);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);
        Contract.ParticipateAssetIssueContract contract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.participateAssetIssue2(contract);
        if (transactionExtention == null) {
            return transactionExtention.getResult();
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return ret;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return transactionExtention.getResult();
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

        //Protocol.Transaction transaction = blockingStubFull.participateAssetIssue(contract);

        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return response;
        } else {
            return ret;
        }
    }

    /**
     * constructor.
     */

    public static Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
                                        String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        byte[] address = addRess;
        long frozenBalance = freezeBalance;
        long frozenDuration = freezeDuration;
        //String priKey = testKey002;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI
                .EmptyMessage.newBuilder().build());
        final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
        Protocol.Account beforeFronzen = queryAccount(priKey, blockingStubFull);
        Long beforeFrozenBalance = 0L;
        //Long beforeNet     = beforeFronzen.getNet();
        if (beforeFronzen.getFrozenCount() != 0) {
            beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
            //beforeNet    = beforeFronzen.getNet();
            //logger.info(Long.toString(beforeFronzen.getNet()));
            logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
        }

        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
                .setFrozenDuration(frozenDuration);

        Contract.FreezeBalanceContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.freezeBalance(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction = null");
            return false;
        }

        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        return response.getResult();
    }

    /**
     * constructor.
     */

    public static Return freezeBalance2(byte[] addRess, long freezeBalance, long freezeDuration,
                                        String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        byte[] address = addRess;
        long frozenBalance = freezeBalance;
        long frozenDuration = freezeDuration;
        //String priKey = testKey002;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI
                .EmptyMessage.newBuilder().build());
        final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
        Protocol.Account beforeFronzen = queryAccount(priKey, blockingStubFull);
        Long beforeFrozenBalance = 0L;
        //Long beforeNet     = beforeFronzen.getNet();
        if (beforeFronzen.getFrozenCount() != 0) {
            beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
            //beforeNet     = beforeFronzen.getNet();
            //logger.info(Long.toString(beforeFronzen.getNet()));
            logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
        }

        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
                .setFrozenDuration(frozenDuration);

        Contract.FreezeBalanceContract contract = builder.build();

        GrpcAPI.TransactionExtention transactionExtention = blockingStubFull.freezeBalance2(contract);
        if (transactionExtention == null) {
            return transactionExtention.getResult();
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return ret;
        } else {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return transactionExtention.getResult();
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        if (response.getResult() == false) {
            return response;
        }

        Long afterBlockNum = 0L;

        while (afterBlockNum < beforeBlockNum) {
            Protocol.Block currentBlock1 = blockingStubFull.getNowBlock(GrpcAPI
                    .EmptyMessage.newBuilder().build());
            afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
        }

        Protocol.Account afterFronzen = queryAccount(priKey, blockingStubFull);
        Long afterFrozenBalance = afterFronzen.getFrozen(0).getFrozenBalance();
        logger.info(Long.toString(afterFronzen.getFrozen(0).getFrozenBalance()));
        logger.info("beforefronen" + beforeFrozenBalance.toString() + "    afterfronzen"
                + afterFrozenBalance.toString());
        Assert.assertTrue(afterFrozenBalance - beforeFrozenBalance == freezeBalance);
        return ret;
    }

    /**
     * constructor.
     */

    public static Boolean unFreezeBalance(byte[] address, String priKey, int resourceCode,
                                          byte[] receiverAddress, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess).setResourceValue(resourceCode);
        if (receiverAddress != null) {
            ByteString receiverAddressBytes = ByteString.copyFrom(receiverAddress);
            builder.setReceiverAddress(receiverAddressBytes);
        }

        Contract.UnfreezeBalanceContract contract = builder.build();
        Transaction transaction = blockingStubFull.unfreezeBalance(contract);
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        return response.getResult();
    }

    /**
     * constructor.
     */

    public static Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey,
                                   WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        //String priKey = testKey002;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Integer times = 0;
        while (times++ <= 2) {

            Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
            ByteString bsTo = ByteString.copyFrom(to);
            ByteString bsOwner = ByteString.copyFrom(owner);
            builder.setToAddress(bsTo);
            builder.setOwnerAddress(bsOwner);
            builder.setAmount(amount);

            Contract.TransferContract contract = builder.build();
            Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
            if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                logger.info("transaction ==null");
                continue;
            }
            transaction = signTransaction(ecKey, transaction);
            GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
            return response.getResult();
        }
        return false;
    }

    /**
     * constructor.
     */

    public static Boolean cancelDeferredTransactionById(String txid, byte[] owner, String priKey,
                                                        WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        return null;
    }

    /**
     * constructor.
     */

    public static String cancelDeferredTransactionByIdGetTxid(String txid, byte[] owner, String priKey,
                                                              WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
    /* Contract.CancelDeferredTransactionContract.Builder builder = Contract
      .CancelDeferredTransactionContract.newBuilder();
    builder.setTransactionId(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    Contract.CancelDeferredTransactionContract contract = builder.build();
   TransactionExtention transactionExtention = blockingStubFull
     .createCancelDeferredTransactionContract(contract);

    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    System.out.println(
        "Cancel transaction before sign txid = " + ByteArray.toHexString(
        transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "Cancel transaction txid = " + ByteArray.toHexString(transactionExtention
        .getTxid().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));*/
        return null;
    }


    /**
     * constructor.
     */

    public static Boolean sendcoinDelayed(byte[] to, long amount, long delaySeconds, byte[] owner,
                                          String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);

//        transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction ==null");
            return false;
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    public static String createAccountDelayGetTxid(byte[] ownerAddress, byte[] newAddress,
                                                   Long delaySeconds, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setAccountAddress(ByteString.copyFrom(newAddress));
        Contract.AccountCreateContract contract = builder.build();
        Transaction transaction = blockingStubFull.createAccount(contract);
        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction == null");
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));

    }

    /**
     * constructor.
     */

    public static String updateAccountDelayGetTxid(byte[] addressBytes, byte[] accountNameBytes,
                                                   Long delaySeconds, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(addressBytes);
        ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

        builder.setAccountName(bsAccountName);
        builder.setOwnerAddress(basAddreess);

        Contract.AccountUpdateContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.updateAccount(contract);
        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("Please check!!! transaction == null");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    }

    /**
     * constructor.
     */

    public static String unfreezeAssetDelayGetTxid(byte[] address, Long delaySeconds, String priKey,
                                                   WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess);

        Contract.UnfreezeAssetContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.unfreezeAsset(contract);
        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("Please check!!! transaction == null");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));


    }


    public static boolean transferAssetDelay(byte[] to, byte[] assertName, long amount,
                                             long delaySeconds, byte[] address, String priKey,
                                             WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(address);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferAssetContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.transferAsset(contract);
        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            if (transaction == null) {
                logger.info("transaction == null");
            } else {
                logger.info("transaction.getRawData().getContractCount() == 0");
            }
            return false;
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    public static String transferAssetDelayGetTxid(byte[] to, byte[] assertName, long amount,
                                                   long delaySeconds, byte[] address, String priKey,
                                                   WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(address);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferAssetContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.transferAsset(contract);
        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            if (transaction == null) {
                logger.info("transaction == null");
            } else {
                logger.info("transaction.getRawData().getContractCount() == 0");
            }
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    }


    /**
     * constructor.
     */

    public static String sendcoinDelayedGetTxid(byte[] to, long amount, long delaySeconds,
                                                byte[] owner, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);

        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction ==null");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    }

    /**
     * constructor.
     */

    public static String setAccountIdDelayGetTxid(byte[] accountIdBytes, long delaySeconds,
                                                  byte[] ownerAddress, String priKey,
                                                  WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.SetAccountIdContract.Builder builder = Contract.SetAccountIdContract.newBuilder();
        ByteString bsAddress = ByteString.copyFrom(owner);
        ByteString bsAccountId = ByteString.copyFrom(accountIdBytes);
        builder.setAccountId(bsAccountId);
        builder.setOwnerAddress(bsAddress);
        Contract.SetAccountIdContract contract = builder.build();
        Transaction transaction = blockingStubFull.setAccountId(contract);
        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction == null");
        }
        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    }


    /**
     * constructor.
     */

    public static String updateAssetDelay(byte[] address, byte[] description, byte[] url,
                                          long newLimit, long newPublicLimit, long delaySeconds, String priKey,
                                          WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        Contract.UpdateAssetContract.Builder builder =
                Contract.UpdateAssetContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        builder.setDescription(ByteString.copyFrom(description));
        builder.setUrl(ByteString.copyFrom(url));
        builder.setNewLimit(newLimit);
        builder.setNewPublicLimit(newPublicLimit);
        builder.setOwnerAddress(basAddreess);

        Contract.UpdateAssetContract contract
                = builder.build();
        Protocol.Transaction transaction = blockingStubFull.updateAsset(contract);
        //transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return null;
        }

        transaction = signTransaction(ecKey, transaction);
        logger.info("Txid is " + ByteArray.toHexString(Sha256Hash.hash(transaction
                .getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    }


    /**
     * constructor.
     */

    public static Return sendcoin2(byte[] to, long amount, byte[] owner, String priKey,
                                   WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        //Protocol.Account search = queryAccount(priKey, blockingStubFull);

        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.createTransaction2(contract);
        if (transactionExtention == null) {
            return transactionExtention.getResult();
        }

        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return ret;
        } else {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
        }

        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return transactionExtention.getResult();
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            //      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return response;
        }
        return ret;
    }

    /**
     * constructor.
     */

    public static String sendcoinGetTransactionId(byte[] to, long amount, byte[] owner, String priKey,
                                                  WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        //String priKey = testKey002;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        //Protocol.Account search = queryAccount(priKey, blockingStubFull);

        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction ==null");
            return null;
        }
        //Test raw data
    /*    Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
    builder1.setData(ByteString.copyFromUtf8("12345678"));
    Transaction.Builder builder2 = transaction.toBuilder();
    builder2.setRawData(builder1);
    transaction = builder2.build();*/

        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            //logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return null;
        } else {
            return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        }
    }

    /**
     * constructor.
     */

    public static Optional<Transaction> getTransactionById(String txId,
                                                           WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubFull) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Transaction transaction = blockingStubFull.getTransactionById(request);

        return Optional.ofNullable(transaction);
    }

    /**
     * constructor.
     */

    public static Optional<Transaction> getTransactionById(String txId,
                                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Transaction transaction = blockingStubFull.getTransactionById(request);
        return Optional.ofNullable(transaction);
    }

    /**
     * constructor.
     */
    public static Long getAssetBalanceByAssetId(ByteString assetId, String priKey,
                                                WalletGrpc.WalletBlockingStub blockingStubFull) {
        Account assetOwnerAccount = queryAccount(priKey, blockingStubFull);
        Long assetOwnerAssetBalance = 0L;
        for (String id : assetOwnerAccount.getAssetV2Map().keySet()) {
            if (assetId.toStringUtf8().equalsIgnoreCase(id)) {
                assetOwnerAssetBalance = assetOwnerAccount.getAssetV2Map().get(id);
            }
        }
        logger.info("asset balance is " + assetOwnerAssetBalance);
        return assetOwnerAssetBalance;
    }

    /**
     * constructor.
     */

    public static Optional<Transaction> getTransactionByIdConfirmed(String txId,
                                                                    WalletGrpc.WalletBlockingStub blockingStubConfirmed) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Transaction transaction = blockingStubConfirmed.getTransactionById(request);
        return Optional.ofNullable(transaction);
    }

    /**
     * constructor.
     */

    public static String printTransaction(Transaction transaction) {
        String result = "";
        result += "hash: ";
        result += "\n";
        result += ByteArray.toHexString(Sha256Hash.hash(transaction.toByteArray()));
        result += "\n";
        result += "txid: ";
        result += "\n";
        result += ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        result += "\n";

        if (transaction.getRawData() != null) {
            result += "raw_data: ";
            result += "\n";
            result += "{";
            result += "\n";
            result += printTransactionRow(transaction.getRawData());
            result += "}";
            result += "\n";
        }
        return result;
    }

    public static long printTransactionRow(Transaction.raw raw) {
        long timestamp = raw.getTimestamp();

        return timestamp;
    }

    public static boolean updateAsset(byte[] address, byte[] description, byte[] url, long newLimit,
                                      long newPublicLimit, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        Contract.UpdateAssetContract.Builder builder =
                Contract.UpdateAssetContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        builder.setDescription(ByteString.copyFrom(description));
        builder.setUrl(ByteString.copyFrom(url));
        builder.setNewLimit(newLimit);
        builder.setNewPublicLimit(newPublicLimit);
        builder.setOwnerAddress(basAddreess);

        Contract.UpdateAssetContract contract
                = builder.build();
        Protocol.Transaction transaction = blockingStubFull.updateAsset(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static Return updateAsset2(byte[] address, byte[] description, byte[] url, long newLimit,
                                      long newPublicLimit, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        Contract.UpdateAssetContract.Builder builder =
                Contract.UpdateAssetContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        builder.setDescription(ByteString.copyFrom(description));
        builder.setUrl(ByteString.copyFrom(url));
        builder.setNewLimit(newLimit);
        builder.setNewPublicLimit(newPublicLimit);
        builder.setOwnerAddress(basAddreess);

        Contract.UpdateAssetContract contract
                = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.updateAsset2(contract);
        if (transactionExtention == null) {
            return transactionExtention.getResult();
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return ret;
        } else {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return transactionExtention.getResult();
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            //logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return response;
        }
        return ret;
    }

    /**
     * constructor.
     */

    public static boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
                                        String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(address);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferAssetContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.transferAsset(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            if (transaction == null) {
                logger.info("transaction == null");
            } else {
                logger.info("transaction.getRawData().getContractCount() == 0");
            }
            return false;
        }
        transaction = signTransaction(ecKey, transaction);

        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    public static boolean updateAccount(byte[] addressBytes, byte[] accountNameBytes, String priKey,
                                        WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(addressBytes);
        ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

        builder.setAccountName(bsAccountName);
        builder.setOwnerAddress(basAddreess);

        Contract.AccountUpdateContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.updateAccount(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("Please check!!! transaction == null");
            return false;
        }
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    public static boolean waitProduceNextBlock(WalletGrpc.WalletBlockingStub
                                                       blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        final Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();

        Block nextBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        Long nextNum = nextBlock.getBlockHeader().getRawData().getNumber();

        Integer wait = 0;
        logger.info("Block num is " + Long.toString(currentBlock
                .getBlockHeader().getRawData().getNumber()));
        while (nextNum <= currentNum + 1 && wait <= 15) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Wait to produce next block");
            nextBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
            nextNum = nextBlock.getBlockHeader().getRawData().getNumber();
            if (wait == 15) {
                logger.info("These 45 second didn't produce a block,please check.");
                return false;
            }
            wait++;
        }
        logger.info("quit normally");
        return true;
    }

    public static boolean waitConfirmedNodeSynFullNodeData(WalletGrpc.WalletBlockingStub
                                                                   blockingStubFull, WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        Block confirmedCurrentBlock = blockingStubConfirmed.getNowBlock(GrpcAPI.EmptyMessage
                .newBuilder().build());
        Integer wait = 0;
        logger.info("Fullnode block num is " + Long.toString(currentBlock
                .getBlockHeader().getRawData().getNumber()));
        while (confirmedCurrentBlock.getBlockHeader().getRawData().getNumber()
                < currentBlock.getBlockHeader().getRawData().getNumber() + 1 && wait <= 10) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Confirmednode num is " + Long.toString(confirmedCurrentBlock
                    .getBlockHeader().getRawData().getNumber()));
            confirmedCurrentBlock = blockingStubConfirmed.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
                    .build());
            if (wait == 10) {
                logger.info("Didn't syn,skip to next case.");
                return false;
            }
            wait++;
        }
        return true;
    }

    public static byte[] getFinalAddress(String priKey) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        WalletClient walletClient;
        walletClient = new WalletClient(priKey);
        //walletClient.init(0);
        return walletClient.getAddress();
    }

    public static AccountNetMessage getAccountNet(byte[] address, WalletGrpc.WalletBlockingStub
            blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString addressBs = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBs).build();
        return blockingStubFull.getAccountNet(request);
    }

    public static boolean createAccount(byte[] ownerAddress, byte[] newAddress, String priKey,
                                        WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setAccountAddress(ByteString.copyFrom(newAddress));
        Contract.AccountCreateContract contract = builder.build();
        Transaction transaction = blockingStubFull.createAccount(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction == null");
        }
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    public static Return createAccount2(byte[] ownerAddress, byte[] newAddress, String priKey,
                                        WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setAccountAddress(ByteString.copyFrom(newAddress));
        Contract.AccountCreateContract contract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.createAccount2(contract);

        if (transactionExtention == null) {
            return transactionExtention.getResult();
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return ret;
        } else {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return transactionExtention.getResult();
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            //logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return response;
        }
        return ret;
    }

    public static boolean createProposal(byte[] ownerAddress, String priKey,
                                         HashMap<Long, Long> parametersMap, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.ProposalCreateContract.Builder builder = Contract.ProposalCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.putAllParameters(parametersMap);

        Contract.ProposalCreateContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.proposalCreate(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        return response.getResult();
    }

    public static boolean approveProposal(byte[] ownerAddress, String priKey, long id,
                                          boolean isAddApproval, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.ProposalApproveContract.Builder builder = Contract.ProposalApproveContract
                .newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setProposalId(id);
        builder.setIsAddApproval(isAddApproval);
        Contract.ProposalApproveContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.proposalApprove(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    public static boolean deleteProposal(byte[] ownerAddress, String priKey, long id,
                                         WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.ProposalDeleteContract.Builder builder = Contract.ProposalDeleteContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setProposalId(id);

        Contract.ProposalDeleteContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.proposalDelete(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    public static String getAddressString(String key) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        return Base58.encode58Check(getFinalAddress(key));
    }

    public static boolean printAddress(String key) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        logger.info(key);
        logger.info(ByteArray.toHexString(getFinalAddress(key)));
        logger.info(Base58.encode58Check(getFinalAddress(key)));
        return true;
    }

    public static ArrayList<String> getAddressInfo(String key) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ArrayList<String> accountList = new ArrayList<String>();
        accountList.add(key);
        accountList.add(ByteArray.toHexString(getFinalAddress(key)));
        accountList.add(Base58.encode58Check(getFinalAddress(key)));
        return accountList;
    }

    public static AccountResourceMessage getAccountResource(byte[] address,
                                                            WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString addressBs = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBs).build();
        return blockingStubFull.getAccountResource(request);
    }

    /**
     * constructor.
     */

    public static boolean setAccountId(byte[] accountIdBytes, byte[] ownerAddress, String priKey,
                                       WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.SetAccountIdContract.Builder builder = Contract.SetAccountIdContract.newBuilder();
        ByteString bsAddress = ByteString.copyFrom(owner);
        ByteString bsAccountId = ByteString.copyFrom(accountIdBytes);
        builder.setAccountId(bsAccountId);
        builder.setOwnerAddress(bsAddress);
        Contract.SetAccountIdContract contract = builder.build();
        Transaction transaction = blockingStubFull.setAccountId(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction == null");
        }
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static Boolean freezeBalanceGetCpu(byte[] addRess, long freezeBalance,
                                              long freezeDuration,
                                              int resourceCode, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        byte[] address = addRess;
        long frozenBalance = freezeBalance;
        long frozenDuration = freezeDuration;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
                .setFrozenDuration(frozenDuration).setResourceValue(resourceCode);

        Contract.FreezeBalanceContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.freezeBalance(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction = null");
            return false;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static boolean buyStorage(long quantity, byte[] address,
                                     String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.BuyStorageContract.Builder builder = Contract.BuyStorageContract.newBuilder();
        ByteString byteAddress = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddress).setQuant(quantity);
        Contract.BuyStorageContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.buyStorage(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static boolean sellStorage(long quantity, byte[] address,
                                      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.SellStorageContract.Builder builder = Contract.SellStorageContract.newBuilder();
        ByteString byteAddress = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddress).setStorageBytes(quantity);
        Contract.SellStorageContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.sellStorage(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */
    public static byte[] deployContract(String contractName, String abiString, String code,
                                        String data, Long feeLimit, long value,
                                        long consumeUserResourcePercent, long originCpuLimit, String tokenId, long tokenValue,
                                        String libraryAddress, String priKey, byte[] ownerAddress,
                                        WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        SmartContract.ABI abi = jsonStr2Abi(abiString);
        if (abi == null) {
            logger.error("abi is null");
            return null;
        }
        //byte[] codeBytes = Hex.decode(code);
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setName(contractName);
        builder.setOriginAddress(ByteString.copyFrom(owner));
        builder.setAbi(abi);
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
        builder.setOriginCpuLimit(originCpuLimit);

        if (value != 0) {

            builder.setCallValue(value);
        }

        byte[] byteCode;
        if (null != libraryAddress) {
            byteCode = replaceLibraryAddress(code, libraryAddress);
        } else {
            byteCode = Hex.decode(code);
        }
        builder.setBytecode(ByteString.copyFrom(byteCode));

        Builder contractBuilder = CreateSmartContract.newBuilder();
        contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
        contractBuilder.setCallTokenValue(tokenValue);
        contractBuilder.setTokenId(Long.parseLong(tokenId));
        CreateSmartContract contractDeployContract = contractBuilder
                .setNewContract(builder.build()).build();

        TransactionExtention transactionExtention = blockingStubFull
                .deployContract(contractDeployContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction(), owner);
        System.out.println(
                "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
        contractAddress = generateContractAddress(transaction, owner);
        System.out.println(
                "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            //logger.info("brodacast succesfully");
            return contractAddress;
        }
    }

    /**
     * constructor.
     */
    public static byte[] deployContract(String contractName, String abiString, String code,
                                        String data, Long feeLimit, long value,
                                        long consumeUserResourcePercent, String libraryAddress, String priKey, byte[] ownerAddress,
                                        WalletGrpc.WalletBlockingStub blockingStubFull) {
        return deployContract(contractName, abiString, code, data, feeLimit, value,
                consumeUserResourcePercent, 1000L, "0", 0L, libraryAddress,
                priKey, ownerAddress, blockingStubFull);
    }

    /**
     * constructor.
     */

    public static byte[] deployContractForLibrary(String contractName, String abiString, String code,
                                                  String data, Long feeLimit, long value,
                                                  long consumeUserResourcePercent, String libraryAddress, String priKey, byte[] ownerAddress,
                                                  String compilerVersion,
                                                  WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        SmartContract.ABI abi = jsonStr2Abi(abiString);
        if (abi == null) {
            logger.error("abi is null");
            return null;
        }
        //byte[] codeBytes = Hex.decode(code);
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setName(contractName);
        builder.setOriginAddress(ByteString.copyFrom(owner));
        builder.setAbi(abi);
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
        builder.setOriginCpuLimit(1000L);

        if (value != 0) {

            builder.setCallValue(value);
        }

        byte[] byteCode;
        if (null != libraryAddress) {
            if (compilerVersion.equals("v5") || compilerVersion.equals("V5")) {
                byteCode = replaceLibraryAddresscompilerVersion(code, libraryAddress, "v5");
            } else {
                //old version
                byteCode = replaceLibraryAddresscompilerVersion(code, libraryAddress, null);
            }

        } else {
            byteCode = Hex.decode(code);
        }
        builder.setBytecode(ByteString.copyFrom(byteCode));

        Builder contractBuilder = CreateSmartContract.newBuilder();
        contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
        contractBuilder.setCallTokenValue(0);
        contractBuilder.setTokenId(Long.parseLong("0"));
        CreateSmartContract contractDeployContract = contractBuilder
                .setNewContract(builder.build()).build();

        TransactionExtention transactionExtention = blockingStubFull
                .deployContract(contractDeployContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction(), owner);
        System.out.println(
                "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
        contractAddress = generateContractAddress(transaction, owner);
        System.out.println(
                "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            //logger.info("brodacast succesfully");
            return contractAddress;
        }

    }

    /**
     * constructor.
     */

    public static String deployContractAndGetTransactionInfoById(String contractName,
                                                                 String abiString, String code, String data, Long feeLimit, long value,
                                                                 long consumeUserResourcePercent, String libraryAddress, String priKey, byte[] ownerAddress,
                                                                 WalletGrpc.WalletBlockingStub blockingStubFull) {
        return deployContractAndGetTransactionInfoById(contractName, abiString, code, data, feeLimit,
                value, consumeUserResourcePercent, 1000L, "0", 0L, libraryAddress,
                priKey, ownerAddress, blockingStubFull);
    }

    /**
     * constructor.
     */

    public static String deployContractAndGetTransactionInfoById(String contractName,
                                                                 String abiString, String code, String data, Long feeLimit, long value,
                                                                 long consumeUserResourcePercent, long originCpuLimit, String tokenId, long tokenValue,
                                                                 String libraryAddress, String priKey, byte[] ownerAddress,
                                                                 WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        SmartContract.ABI abi = jsonStr2Abi(abiString);
        if (abi == null) {
            logger.error("abi is null");
            return null;
        }
        //byte[] codeBytes = Hex.decode(code);
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setName(contractName);
        builder.setOriginAddress(ByteString.copyFrom(owner));
        builder.setAbi(abi);
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
        builder.setOriginCpuLimit(originCpuLimit);

        if (value != 0) {

            builder.setCallValue(value);
        }

        byte[] byteCode;
        if (null != libraryAddress) {
            byteCode = replaceLibraryAddress(code, libraryAddress);
        } else {
            byteCode = Hex.decode(code);
        }
        builder.setBytecode(ByteString.copyFrom(byteCode));

        Builder contractBuilder = CreateSmartContract.newBuilder();
        contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
        contractBuilder.setCallTokenValue(tokenValue);
        contractBuilder.setTokenId(Long.parseLong(tokenId));
        CreateSmartContract contractDeployContract = contractBuilder
                .setNewContract(builder.build()).build();

        TransactionExtention transactionExtention = blockingStubFull
                .deployContract(contractDeployContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();


        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
        byte[] contractAddress = generateContractAddress(transaction, owner);
        System.out.println(
                "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            //logger.info("brodacast succesfully");
            return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        }
    }

    /**
     * constructor.
     */

    public static SmartContract.ABI jsonStr2Abi(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
        JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
        SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
        for (int index = 0; index < jsonRoot.size(); index++) {
            JsonElement abiItem = jsonRoot.get(index);
            boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null
                    ? abiItem.getAsJsonObject().get("anonymous").getAsBoolean() : false;
            final boolean constant = abiItem.getAsJsonObject().get("constant") != null
                    ? abiItem.getAsJsonObject().get("constant").getAsBoolean() : false;
            final String name = abiItem.getAsJsonObject().get("name") != null
                    ? abiItem.getAsJsonObject().get("name").getAsString() : null;
            JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null
                    ? abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
            final JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null
                    ? abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
            String type = abiItem.getAsJsonObject().get("type") != null
                    ? abiItem.getAsJsonObject().get("type").getAsString() : null;
            final boolean payable = abiItem.getAsJsonObject().get("payable") != null
                    ? abiItem.getAsJsonObject().get("payable").getAsBoolean() : false;
            final String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null
                    ? abiItem.getAsJsonObject().get("stateMutability").getAsString() : null;
            if (type == null) {
                logger.error("No type!");
                return null;
            }
            if (!type.equalsIgnoreCase("fallback") && null == inputs) {
                logger.error("No inputs!");
                return null;
            }

            SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
            entryBuilder.setAnonymous(anonymous);
            entryBuilder.setConstant(constant);
            if (name != null) {
                entryBuilder.setName(name);
            }

            /* { inputs : optional } since fallback function not requires inputs*/
            if (inputs != null) {
                for (int j = 0; j < inputs.size(); j++) {
                    JsonElement inputItem = inputs.get(j);
                    if (inputItem.getAsJsonObject().get("name") == null
                            || inputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Input argument invalid due to no name or no type!");
                        return null;
                    }
                    String inputName = inputItem.getAsJsonObject().get("name").getAsString();
                    String inputType = inputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    JsonElement indexed = inputItem.getAsJsonObject().get("indexed");

                    paramBuilder.setIndexed((indexed == null) ? false : indexed.getAsBoolean());
                    paramBuilder.setName(inputName);
                    paramBuilder.setType(inputType);
                    entryBuilder.addInputs(paramBuilder.build());
                }
            }

            /* { outputs : optional } */
            if (outputs != null) {
                for (int k = 0; k < outputs.size(); k++) {
                    JsonElement outputItem = outputs.get(k);
                    if (outputItem.getAsJsonObject().get("name") == null
                            || outputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Output argument invalid due to no name or no type!");
                        return null;
                    }
                    String outputName = outputItem.getAsJsonObject().get("name").getAsString();
                    String outputType = outputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    JsonElement indexed = outputItem.getAsJsonObject().get("indexed");

                    paramBuilder.setIndexed((indexed == null) ? false : indexed.getAsBoolean());
                    paramBuilder.setName(outputName);
                    paramBuilder.setType(outputType);
                    entryBuilder.addOutputs(paramBuilder.build());
                }
            }

            entryBuilder.setType(getEntryType(type));
            entryBuilder.setPayable(payable);
            if (stateMutability != null) {
                entryBuilder.setStateMutability(getStateMutability(stateMutability));
            }

            abiBuilder.addEntrys(entryBuilder.build());
        }

        return abiBuilder.build();
    }

    /**
     * constructor.
     */

    public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
            String stateMutability) {
        switch (stateMutability) {
            case "pure":
                return SmartContract.ABI.Entry.StateMutabilityType.Pure;
            case "view":
                return SmartContract.ABI.Entry.StateMutabilityType.View;
            case "nonpayable":
                return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
            case "payable":
                return SmartContract.ABI.Entry.StateMutabilityType.Payable;
            default:
                return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
        }
    }

    /**
     * constructor.
     */

    public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
        switch (type) {
            case "constructor":
                return SmartContract.ABI.Entry.EntryType.Constructor;
            case "function":
                return SmartContract.ABI.Entry.EntryType.Function;
            case "event":
                return SmartContract.ABI.Entry.EntryType.Event;
            case "fallback":
                return SmartContract.ABI.Entry.EntryType.Fallback;
            default:
                return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
        }
    }

    /**
     * constructor.
     */

    public static byte[] generateContractAddress(Transaction trx, byte[] owneraddress) {

        // get owner address
        // this address should be as same as the onweraddress in trx, DONNOT modify it
        byte[] ownerAddress = owneraddress;

        // get tx hash
        byte[] txRawDataHash = Sha256Hash.of(trx.getRawData().toByteArray()).getBytes();

        // combine
        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

        return Hash.sha3omit12(combined);

    }

    /**
     * constructor.
     */

    public static SmartContract getContract(byte[] address, WalletGrpc
            .WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString byteString = ByteString.copyFrom(address);
        BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
        Integer i = 0;
        while (blockingStubFull.getContract(bytesMessage).getName().isEmpty() && i++ < 4) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("contract name is " + blockingStubFull.getContract(bytesMessage).getName());
        logger.info("contract address is " + WalletClient.encode58Check(address));
        return blockingStubFull.getContract(bytesMessage);
    }

    private static byte[] replaceLibraryAddress(String code, String libraryAddressPair) {

        String[] libraryAddressList = libraryAddressPair.split("[,]");

        for (int i = 0; i < libraryAddressList.length; i++) {
            String cur = libraryAddressList[i];

            int lastPosition = cur.lastIndexOf(":");
            if (-1 == lastPosition) {
                throw new RuntimeException("libraryAddress delimit by ':'");
            }
            String libraryName = cur.substring(0, lastPosition);
            String addr = cur.substring(lastPosition + 1);
            String libraryAddressHex = ByteArray.toHexString(Wallet.decodeFromBase58Check(addr))
                    .substring(2);

            String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
            String beReplaced = "__" + libraryName + repeated;
            Matcher m = Pattern.compile(beReplaced).matcher(code);
            code = m.replaceAll(libraryAddressHex);
        }

        return Hex.decode(code);
    }

    private static byte[] replaceLibraryAddress_1(String code, byte[] libraryAddress) {

        String libraryAddressHex = ByteArray.toHexString(libraryAddress).substring(2);

        Matcher m = Pattern.compile("__.*__").matcher(code);
        code = m.replaceAll(libraryAddressHex);
        return Hex.decode(code);
    }

    private static byte[] replaceLibraryAddresscompilerVersion(String code, String libraryAddressPair,
                                                               String compilerVersion) {

        String[] libraryAddressList = libraryAddressPair.split("[,]");

        for (int i = 0; i < libraryAddressList.length; i++) {
            String cur = libraryAddressList[i];

            int lastPosition = cur.lastIndexOf(":");
            if (-1 == lastPosition) {
                throw new RuntimeException("libraryAddress delimit by ':'");
            }
            String libraryName = cur.substring(0, lastPosition);
            String addr = cur.substring(lastPosition + 1);
            String libraryAddressHex;
            try {
                libraryAddressHex = (new String(Hex.encode(Wallet.decodeFromBase58Check(addr)),
                        "US-ASCII")).substring(2);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);  // now ignore
            }

            String beReplaced;
            if (compilerVersion == null) {
                //old version
                String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
                beReplaced = "__" + libraryName + repeated;
            } else if (compilerVersion.equalsIgnoreCase("v5")) {
                //0.5.4 version
                String libraryNameKeccak256 = ByteArray
                        .toHexString(Hash.sha3(ByteArray.fromString(libraryName))).substring(0, 34);
                beReplaced = "__\\$" + libraryNameKeccak256 + "\\$__";
            } else {
                throw new RuntimeException("unknown compiler version.");
            }

            Matcher m = Pattern.compile(beReplaced).matcher(code);
            code = m.replaceAll(libraryAddressHex);
        }

        return Hex.decode(code);
    }

    /**
     * constructor.
     */

    public static boolean updateSetting(byte[] contractAddress, long consumeUserResourcePercent,
                                        String priKey, byte[] ownerAddress, WalletGrpc
                                                .WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.UpdateSettingContract.Builder builder = Contract.UpdateSettingContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

        UpdateSettingContract updateSettingContract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull
                .updateSetting(updateSettingContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static boolean updateSettingDelay(byte[] contractAddress,
                                             long consumeUserResourcePercent, long delaySeconds, String priKey, byte[] ownerAddress,
                                             WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.UpdateSettingContract.Builder builder = Contract.UpdateSettingContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

        UpdateSettingContract updateSettingContract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull
                .updateSetting(updateSettingContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }
        return false;
    }

    /**
     * constructor.
     */

    public static String updateSettingDelayGetTxid(byte[] contractAddress,
                                                   long consumeUserResourcePercent, long delaySeconds,
                                                   String priKey, byte[] ownerAddress, WalletGrpc
                                                           .WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.UpdateSettingContract.Builder builder = Contract.UpdateSettingContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

        UpdateSettingContract updateSettingContract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull
                .updateSetting(updateSettingContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return null;
        }
        return null;
    }

    /**
     * constructor.
     */
    public static String updateCpuLimitDelayGetTxid(byte[] contractAddress,
                                                    long originCpuLimit, long delaySeconds, String priKey, byte[] ownerAddress,
                                                    WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.UpdateCpuLimitContract.Builder builder = Contract.UpdateCpuLimitContract
                .newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setOriginCpuLimit(originCpuLimit);

        Contract.UpdateCpuLimitContract updateCpuLimitContract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull
                .updateCpuLimit(updateCpuLimitContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return null;
        }
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    }


    /**
     * 61 constructor.
     */

    public static Optional<TransactionInfo> getTransactionInfoById(String txId, WalletGrpc
            .WalletBlockingStub blockingStubFull) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        TransactionInfo transactionInfo;
        transactionInfo = blockingStubFull.getTransactionInfoById(request);
        return Optional.ofNullable(transactionInfo);
    }

    public static String triggerContract(byte[] contractAddress, String method, String argsStr,
                                         Boolean isHex, long callValue, long feeLimit, byte[] ownerAddress,
                                         String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        return triggerContract(contractAddress, method, argsStr, isHex, callValue, feeLimit,
                "0", 0, ownerAddress, priKey, blockingStubFull);
    }

    /**
     * constructor.
     */
    public static String triggerContract(byte[] contractAddress, String method, String argsStr,
                                         Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
                                         byte[] ownerAddress,
                                         String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        if (argsStr.equalsIgnoreCase("#")) {
            logger.info("argsstr is #");
            argsStr = "";
        }

        byte[] owner = ownerAddress;
        byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));
        System.out.println("input: " + Hex.toHexString(input));

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(input));
        builder.setCallValue(callValue);
        builder.setTokenId(Long.parseLong(tokenId));
        builder.setCallTokenValue(tokenValue);
        Contract.TriggerSmartContract triggerContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create call trx failed!");
            System.out.println("Code = " + transactionExtention.getResult().getCode());
            System.out
                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0
                && transactionExtention.getConstantResult(0) != null
                && transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            System.out.println("message:" + transaction.getRet(0).getRet());
            System.out.println(":" + ByteArray
                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            System.out.println("Result:" + Hex.toHexString(result));
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "trigger txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData()
                        .toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        }
    }

    /**
     * constructor.
     */

    public static String triggerParamListContract(byte[] contractAddress, String method,
                                                  List<Object> params,
                                                  Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
                                                  byte[] ownerAddress,
                                                  String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        byte[] input = Hex.decode(AbiUtil.parseMethod(method, params));

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(input));
        builder.setCallValue(callValue);
        builder.setTokenId(Long.parseLong(tokenId));
        builder.setCallTokenValue(tokenValue);
        Contract.TriggerSmartContract triggerContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create call trx failed!");
            System.out.println("Code = " + transactionExtention.getResult().getCode());
            System.out
                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0
                && transactionExtention.getConstantResult(0) != null
                && transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            System.out.println("message:" + transaction.getRet(0).getRet());
            System.out.println(":" + ByteArray
                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            System.out.println("Result:" + Hex.toHexString(result));
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "trigger txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData()
                        .toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        }


    }

    /**
     * constructor.
     */

    public static Boolean exchangeCreate(byte[] firstTokenId, long firstTokenBalance,
                                         byte[] secondTokenId, long secondTokenBalance, byte[] ownerAddress,
                                         String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;

        Contract.ExchangeCreateContract.Builder builder = Contract.ExchangeCreateContract.newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setFirstTokenId(ByteString.copyFrom(firstTokenId))
                .setFirstTokenBalance(firstTokenBalance)
                .setSecondTokenId(ByteString.copyFrom(secondTokenId))
                .setSecondTokenBalance(secondTokenBalance);
        Contract.ExchangeCreateContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.exchangeCreate(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));

        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        return response.getResult();
    }

    /**
     * constructor.
     */

    public static Boolean injectExchange(long exchangeId, byte[] tokenId, long quant,
                                         byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;

        Contract.ExchangeInjectContract.Builder builder = Contract.ExchangeInjectContract.newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setExchangeId(exchangeId)
                .setTokenId(ByteString.copyFrom(tokenId))
                .setQuant(quant);
        Contract.ExchangeInjectContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.exchangeInject(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        return response.getResult();
    }

    public static Optional<ExchangeList> getExchangeList(WalletGrpc.WalletBlockingStub
                                                                 blockingStubFull) {
        ExchangeList exchangeList = blockingStubFull.listExchanges(EmptyMessage.newBuilder().build());
        return Optional.ofNullable(exchangeList);
    }

    /**
     * constructor.
     */

    public static Optional<ExchangeList> getExchangeList(WalletConfirmedGrpc
                                                                 .WalletConfirmedBlockingStub blockingStubConfirmed) {
        ExchangeList exchangeList = blockingStubConfirmed.listExchanges(EmptyMessage.newBuilder()
                .build());
        return Optional.ofNullable(exchangeList);
    }

    /**
     * constructor.
     */

    public static Optional<Exchange> getExchange(String id, WalletGrpc.WalletBlockingStub
            blockingStubFull) {
        BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
                ByteArray.fromLong(Long.parseLong(id))))
                .build();
        Exchange exchange = blockingStubFull.getExchangeById(request);
        return Optional.ofNullable(exchange);
    }

    /**
     * constructor.
     */

    public static Optional<Exchange> getExchange(String id, WalletConfirmedGrpc
            .WalletConfirmedBlockingStub blockingStubConfirmed) {
        BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
                ByteArray.fromLong(Long.parseLong(id))))
                .build();
        Exchange exchange = blockingStubConfirmed.getExchangeById(request);
        return Optional.ofNullable(exchange);
    }

    /**
     * constructor.
     */

    public static boolean exchangeWithdraw(long exchangeId, byte[] tokenId, long quant,
                                           byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        byte[] owner = ownerAddress;

        Contract.ExchangeWithdrawContract.Builder builder = Contract.ExchangeWithdrawContract
                .newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setExchangeId(exchangeId)
                .setTokenId(ByteString.copyFrom(tokenId))
                .setQuant(quant);
        Contract.ExchangeWithdrawContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.exchangeWithdraw(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));

        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static boolean exchangeTransaction(long exchangeId, byte[] tokenId, long quant,
                                              long expected, byte[] ownerAddress, String priKey,
                                              WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        byte[] owner = ownerAddress;

        Contract.ExchangeTransactionContract.Builder builder = Contract.ExchangeTransactionContract
                .newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setExchangeId(exchangeId)
                .setTokenId(ByteString.copyFrom(tokenId))
                .setQuant(quant)
                .setExpected(expected);
        Contract.ExchangeTransactionContract contract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull.exchangeTransaction(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));

        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static String deployContractWithConstantParame(String contractName, String abiString,
                                                          String code, String constructorStr, String argsStr, String data, Long feeLimit, long value,
                                                          long consumeUserResourcePercent, String libraryAddress, String priKey, byte[] ownerAddress,
                                                          WalletGrpc.WalletBlockingStub blockingStubFull) {
        return deployContractWithConstantParame(contractName, abiString, code, constructorStr, argsStr,
                data, feeLimit, value, consumeUserResourcePercent, 1000L, "0", 0L,
                libraryAddress, priKey, ownerAddress, blockingStubFull);
    }

    /**
     * constructor.
     */

    public static String deployContractWithConstantParame(String contractName, String abiString,
                                                          String code, String constructorStr, String argsStr, String data, Long feeLimit, long value,
                                                          long consumeUserResourcePercent, long originCpuLimit, String tokenId, long tokenValue,
                                                          String libraryAddress, String priKey, byte[] ownerAddress,
                                                          WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        SmartContract.ABI abi = jsonStr2Abi(abiString);
        if (abi == null) {
            logger.error("abi is null");
            return null;
        }

        code += Hex.toHexString(AbiUtil.encodeInput(constructorStr, argsStr));
        byte[] owner = ownerAddress;
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setName(contractName);
        builder.setOriginAddress(ByteString.copyFrom(owner));
        builder.setAbi(abi);
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
        builder.setOriginCpuLimit(originCpuLimit);

        if (value != 0) {

            builder.setCallValue(value);
        }

        byte[] byteCode;
        if (null != libraryAddress) {
            byteCode = replaceLibraryAddress(code, libraryAddress);
        } else {
            byteCode = Hex.decode(code);
        }
        builder.setBytecode(ByteString.copyFrom(byteCode));

        Builder contractBuilder = CreateSmartContract.newBuilder();
        contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
        contractBuilder.setCallTokenValue(tokenValue);
        contractBuilder.setTokenId(Long.parseLong(tokenId));
        CreateSmartContract contractDeployContract = contractBuilder.setNewContract(builder.build())
                .build();

        TransactionExtention transactionExtention = blockingStubFull
                .deployContract(contractDeployContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
        byte[] contractAddress = generateContractAddress(transaction, owner);
        System.out.println(
                "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            //logger.info("brodacast succesfully");
            return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        }
    }

    /**
     * constructor.
     */

    public static Boolean freezeBalanceForReceiver(byte[] addRess, long freezeBalance,
                                                   long freezeDuration, int resourceCode, ByteString receiverAddressBytes, String priKey,
                                                   WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        byte[] address = addRess;
        long frozenBalance = freezeBalance;
        long frozenDuration = freezeDuration;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
                .setFrozenDuration(frozenDuration).setResourceValue(resourceCode);
        builder.setReceiverAddress(receiverAddressBytes);
        Contract.FreezeBalanceContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.freezeBalance(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction = null");
            return false;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */

    public static Optional<DelegatedResourceList> getDelegatedResource(byte[] fromAddress,
                                                                       byte[] toAddress, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString fromAddressBs = ByteString.copyFrom(fromAddress);
        ByteString toAddressBs = ByteString.copyFrom(toAddress);

        DelegatedResourceMessage request = DelegatedResourceMessage.newBuilder()
                .setFromAddress(fromAddressBs)
                .setToAddress(toAddressBs)
                .build();
        DelegatedResourceList delegatedResource = blockingStubFull.getDelegatedResource(request);
        return Optional.ofNullable(delegatedResource);
    }

    /**
     * constructor.
     */

    public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(
            byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);

        ByteString addressBs = ByteString.copyFrom(address);

        BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();

        DelegatedResourceAccountIndex accountIndex = blockingStubFull
                .getDelegatedResourceAccountIndex(bytesMessage);
        return Optional.ofNullable(accountIndex);
    }

    /**
     * constructor.
     */

    public static Contract.AssetIssueContract getAssetIssueByName(String assetName,
                                                                  WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
        BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
        return blockingStubFull.getAssetIssueByName(request);
    }

    /**
     * constructor.
     */

    public static Optional<AssetIssueList> getAssetIssueListByName(String assetName,
                                                                   WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
        BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
        AssetIssueList assetIssueList = blockingStubFull.getAssetIssueListByName(request);
        return Optional.ofNullable(assetIssueList);
    }

    /**
     * constructor.
     */

    public static Contract.AssetIssueContract getAssetIssueById(String assetId,
                                                                WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ByteString assetIdBs = ByteString.copyFrom(assetId.getBytes());
        BytesMessage request = BytesMessage.newBuilder().setValue(assetIdBs).build();
        return blockingStubFull.getAssetIssueById(request);
    }

    private static Permission json2Permission(JSONObject json) {
        Permission.Builder permissionBuilder = Permission.newBuilder();
        if (json.containsKey("type")) {
            int type = json.getInteger("type");
            permissionBuilder.setTypeValue(type);
        }
        if (json.containsKey("permission_name")) {
            String permissionName = json.getString("permission_name");
            permissionBuilder.setPermissionName(permissionName);
        }
        if (json.containsKey("threshold")) {
            //long threshold = json.getLong("threshold");
            long threshold = Long.parseLong(json.getString("threshold"));
            permissionBuilder.setThreshold(threshold);
        }
        if (json.containsKey("parent_id")) {
            int parentId = json.getInteger("parent_id");
            permissionBuilder.setParentId(parentId);
        }
        if (json.containsKey("operations")) {
            byte[] operations = ByteArray.fromHexString(json.getString("operations"));
            permissionBuilder.setOperations(ByteString.copyFrom(operations));
        }
        if (json.containsKey("keys")) {
            JSONArray keys = json.getJSONArray("keys");
            List<Key> keyList = new ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                Key.Builder keyBuilder = Key.newBuilder();
                JSONObject key = keys.getJSONObject(i);
                String address = key.getString("address");
                long weight = Long.parseLong(key.getString("weight"));
                //long weight = key.getLong("weight");
                //keyBuilder.setAddress(ByteString.copyFrom(address.getBytes()));
                keyBuilder.setAddress(ByteString.copyFrom(WalletClient.decodeFromBase58Check(address)));
                keyBuilder.setWeight(weight);
                keyList.add(keyBuilder.build());
            }
            permissionBuilder.addAllKeys(keyList);
        }
        return permissionBuilder.build();
    }

    /**
     * constructor.
     */
    public static boolean accountPermissionUpdate(String permissionJson, byte[] owner, String priKey,
                                                  WalletGrpc.WalletBlockingStub blockingStubFull, String[] priKeys) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.AccountPermissionUpdateContract.Builder builder =
                Contract.AccountPermissionUpdateContract.newBuilder();

        JSONObject permissions = JSONObject.parseObject(permissionJson);
        JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
        JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
        JSONArray activepermissions = permissions.getJSONArray("active_permissions");

        if (ownerpermission != null) {
            Permission ownerPermission = json2Permission(ownerpermission);
            builder.setOwner(ownerPermission);
        }
        if (witnesspermission != null) {
            Permission witnessPermission = json2Permission(witnesspermission);
            builder.setWitness(witnessPermission);
        }
        if (activepermissions != null) {
            List<Permission> activePermissionList = new ArrayList<>();
            for (int j = 0; j < activepermissions.size(); j++) {
                JSONObject permission = activepermissions.getJSONObject(j);
                activePermissionList.add(json2Permission(permission));
            }
            builder.addAllActives(activePermissionList);
        }
        builder.setOwnerAddress(ByteString.copyFrom(owner));

        Contract.AccountPermissionUpdateContract contract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }


    /**
     * constructor.
     */
    public static long getFreezeBalanceCount(byte[] accountAddress, String ecKey, Long targetCpu,
                                             WalletGrpc.WalletBlockingStub blockingStubFull) {
        //Precision change as the entire network freezes
        AccountResourceMessage resourceInfo = getAccountResource(accountAddress,
                blockingStubFull);

        Account info = queryAccount(accountAddress, blockingStubFull);

        Account getAccount = queryAccount(ecKey, blockingStubFull);

        long balance = info.getBalance();
        long frozenBalance = info.getAccountResource().getFrozenBalanceForCpu().getFrozenBalance();
        long totalCpuLimit = resourceInfo.getTotalCpuLimit();
        long totalCpuWeight = resourceInfo.getTotalCpuWeight();
        long cpuUsed = resourceInfo.getCpuUsed();
        long cpuLimit = resourceInfo.getCpuLimit();

        if (cpuUsed > cpuLimit) {
            targetCpu = cpuUsed - cpuLimit + targetCpu;
        }

        if (totalCpuWeight == 0) {
            return 1000_000L;
        }

        // totalCpuLimit / (totalCpuWeight + needBalance) = needCpu / needBalance
        final BigInteger totalCpuWeightBi = BigInteger.valueOf(totalCpuWeight);
        long needBalance = totalCpuWeightBi.multiply(BigInteger.valueOf(1_000_000))
                .multiply(BigInteger.valueOf(targetCpu))
                .divide(BigInteger.valueOf(totalCpuLimit - targetCpu)).longValue();

        logger.info("getFreezeBalanceCount, needBalance: " + needBalance);

        if (needBalance < 1000000L) {
            needBalance = 1000000L;
            logger.info(
                    "getFreezeBalanceCount, needBalance less than 1 GSC, modify to: " + needBalance);
        }
        return needBalance;
    }

    /**
     * constructor.
     */
    public static Long getAssetIssueValue(byte[] accountAddress, ByteString assetIssueId,
                                          WalletGrpc.WalletBlockingStub blockingStubFull) {
        Long assetIssueCount = 0L;
        Account contractAccount = queryAccount(accountAddress, blockingStubFull);
        Map<String, Long> createAssetIssueMap = contractAccount.getAssetV2Map();
        for (Map.Entry<String, Long> entry : createAssetIssueMap.entrySet()) {
            if (assetIssueId.toStringUtf8().equals(entry.getKey())) {
                assetIssueCount = entry.getValue();
            }
        }
        return assetIssueCount;
    }

    /**
     * constructor.
     */
    public static List<String> getStrings(byte[] data) {
        int index = 0;
        List<String> ret = new ArrayList<>();
        while (index < data.length) {
            ret.add(byte2HexStr(data, index, 32));
            index += 32;
        }
        return ret;
    }

    /**
     * constructor.
     */
    public static String byte2HexStr(byte[] b, int offset, int length) {
        StringBuilder ssBuilder = new StringBuilder();
        for (int n = offset; n < offset + length && n < b.length; n++) {
            String stmp = Integer.toHexString(b[n] & 0xFF);
            ssBuilder.append((stmp.length() == 1) ? "0" + stmp : stmp);
        }
        return ssBuilder.toString().toUpperCase().trim();
    }


    /**
     * constructor.
     */
    public static Transaction addTransactionSign(Transaction transaction, String priKey,
                                                 WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey = temKey;

        Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
        byte[] hash = Sha256Hash.hash(transaction.getRawData().toByteArray());

        ECDSASignature signature = ecKey.sign(hash);
        ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
        transactionBuilderSigned.addSignature(bsSign);
        transaction = transactionBuilderSigned.build();
        return transaction;
    }

    /**
     * constructor.
     */
    public static GrpcAPI.Return deployContractAndGetResponse(String contractName,
                                                              String abiString, String code, String data, Long feeLimit, long value,
                                                              long consumeUserResourcePercent, long originCpuLimit, String tokenId, long tokenValue,
                                                              String libraryAddress, String priKey, byte[] ownerAddress,
                                                              WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        SmartContract.ABI abi = jsonStr2Abi(abiString);
        if (abi == null) {
            logger.error("abi is null");
            return null;
        }
        //byte[] codeBytes = Hex.decode(code);
        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setName(contractName);
        builder.setOriginAddress(ByteString.copyFrom(owner));
        builder.setAbi(abi);
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
        builder.setOriginCpuLimit(originCpuLimit);

        if (value != 0) {

            builder.setCallValue(value);
        }

        byte[] byteCode;
        if (null != libraryAddress) {
            byteCode = replaceLibraryAddress(code, libraryAddress);
        } else {
            byteCode = Hex.decode(code);
        }
        builder.setBytecode(ByteString.copyFrom(byteCode));

        Builder contractBuilder = CreateSmartContract.newBuilder();
        contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
        contractBuilder.setCallTokenValue(tokenValue);
        contractBuilder.setTokenId(Long.parseLong(tokenId));
        CreateSmartContract contractDeployContract = contractBuilder
                .setNewContract(builder.build()).build();

        TransactionExtention transactionExtention = blockingStubFull
                .deployContract(contractDeployContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
        byte[] contractAddress = generateContractAddress(transaction, owner);
        System.out.println(
                "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        return response;
    }

    /**
     * constructor.
     */
    public static GrpcAPI.Return triggerContractAndGetResponse(byte[] contractAddress, String method,
                                                               String argsStr,
                                                               Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
                                                               byte[] ownerAddress,
                                                               String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        if (argsStr.equalsIgnoreCase("#")) {
            logger.info("argsstr is #");
            argsStr = "";
        }

        byte[] owner = ownerAddress;
        byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(input));
        builder.setCallValue(callValue);
        builder.setTokenId(Long.parseLong(tokenId));
        builder.setCallTokenValue(tokenValue);
        Contract.TriggerSmartContract triggerContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create call trx failed!");
            System.out.println("Code = " + transactionExtention.getResult().getCode());
            System.out
                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0
                && transactionExtention.getConstantResult(0) != null
                && transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            System.out.println("message:" + transaction.getRet(0).getRet());
            System.out.println(":" + ByteArray
                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            System.out.println("Result:" + Hex.toHexString(result));
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "trigger txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData()
                        .toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response;
    }

    /**
     * constructor.
     */
    public static boolean updateCpuLimit(byte[] contractAddress, long originCpuLimit,
                                         String priKey, byte[] ownerAddress, WalletGrpc
                                                 .WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;
        Contract.UpdateCpuLimitContract.Builder builder = Contract.UpdateCpuLimitContract
                .newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setOriginCpuLimit(originCpuLimit);

        Contract.UpdateCpuLimitContract updateCpuLimitContract = builder.build();
        TransactionExtention transactionExtention = blockingStubFull
                .updateCpuLimit(updateCpuLimitContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create trx failed!");
            if (transactionExtention != null) {
                System.out.println("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return false;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        return response.getResult();
    }

    /**
     * constructor.
     */
    public static GrpcAPI.Return accountPermissionUpdateForResponse(String permissionJson,
                                                                    byte[] owner, String priKey,
                                                                    WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        Contract.AccountPermissionUpdateContract.Builder builder =
                Contract.AccountPermissionUpdateContract.newBuilder();

        JSONObject permissions = JSONObject.parseObject(permissionJson);
        JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
        JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
        JSONArray activepermissions = permissions.getJSONArray("active_permissions");

        if (ownerpermission != null) {
            Permission ownerPermission = json2Permission(ownerpermission);
            builder.setOwner(ownerPermission);
        }
        if (witnesspermission != null) {
            Permission witnessPermission = json2Permission(witnesspermission);
            builder.setWitness(witnessPermission);
        }
        if (activepermissions != null) {
            List<Permission> activePermissionList = new ArrayList<>();
            for (int j = 0; j < activepermissions.size(); j++) {
                JSONObject permission = activepermissions.getJSONObject(j);
                activePermissionList.add(json2Permission(permission));
            }
            builder.addAllActives(activePermissionList);
        }
        builder.setOwnerAddress(ByteString.copyFrom(owner));

        Contract.AccountPermissionUpdateContract contract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return ret;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return ret;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(ecKey, transaction);
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

        return response;
    }

    public static TransactionApprovedList getTransactionApprovedList(Transaction transaction,
                                                                     WalletGrpc
                                                                             .WalletBlockingStub blockingStubFull) {
        return blockingStubFull.getTransactionApprovedList(transaction);
    }

    /**
     * constructor.
     */
    public static long getFreezeBalanceNetCount(byte[] accountAddress, String ecKey, Long targetNet,
                                                WalletGrpc.WalletBlockingStub blockingStubFull) {
        //Precision change as the entire network freezes
        AccountResourceMessage resourceInfo = getAccountResource(accountAddress,
                blockingStubFull);

        Account info = queryAccount(accountAddress, blockingStubFull);

        Account getAccount = queryAccount(ecKey, blockingStubFull);

        long balance = info.getBalance();
        long totalNetLimit = resourceInfo.getTotalNetLimit();
        long totalNetWeight = resourceInfo.getTotalNetWeight();
        long netUsed = resourceInfo.getNetUsed();
        long netLimit = resourceInfo.getNetLimit();

        if (netUsed > netLimit) {
            targetNet = netUsed - netLimit + targetNet;
        }

        if (totalNetWeight == 0) {
            return 1000_000L;
        }

        // totalNetLimit / (totalNetWeight + needBalance) = needNet / needBalance
        final BigInteger totalNetWeightBi = BigInteger.valueOf(totalNetWeight);
        long needBalance = totalNetWeightBi.multiply(BigInteger.valueOf(1_000_000))
                .multiply(BigInteger.valueOf(targetNet))
                .divide(BigInteger.valueOf(totalNetLimit - targetNet)).longValue();

        logger.info("getFreezeBalanceNetCount, needBalance: " + needBalance);

        if (needBalance < 1000000L) {
            needBalance = 1000000L;
            logger.info(
                    "getFreezeBalanceNetCount, needBalance less than 1 TRX, modify to: " + needBalance);
        }
        return needBalance;
    }

    /**
     * constructor.
     */
    public static GrpcAPI.Return broadcastTransaction(Transaction transaction,
                                                      WalletGrpc.WalletBlockingStub blockingStubFull) {
        int i = 10;
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        while (response.getResult() == false && response.getCode() == response_code.SERVER_BUSY
                && i > 0) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i--;
            response = blockingStubFull.broadcastTransaction(transaction);
            logger.info("repeate times = " + (10 - i));
        }

        if (response.getResult() == false) {
            logger.info("Code = " + response.getCode());
            logger.info("Message = " + response.getMessage().toStringUtf8());
        }
        return response;
    }

    /**
     * constructor.
     */
    public static String exec(String command) throws InterruptedException {
        String returnString = "";
        Process pro = null;
        Runtime runTime = Runtime.getRuntime();
        if (runTime == null) {
            logger.error("Create runtime false!");
        }
        try {
            pro = runTime.exec(command);
            BufferedReader input = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(pro.getOutputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                returnString = returnString + line + "\n";
            }
            input.close();
            output.close();
            pro.destroy();
        } catch (IOException ex) {
            logger.error(null, ex);
        }
        return returnString;
    }

    public static void main(String[] args) {
        getByCodeAbi("./src/test/resources/soliditycode_v0.4.25/contractGrcToken001.sol", "tokenTest");
    }
    /**
     * constructor.
     */
    public static HashMap<String, String> getByCodeAbi(String solFile, String contractName) {
        final String compile = Configuration.getByPath("testng.conf")
                    .getString("defaultParameter.solidityCompile");

        System.out.println(compile);

        String dirPath = solFile.substring(solFile.lastIndexOf("/"), solFile.lastIndexOf("."));
        String outputPath = "src/test/resources/soliditycode/output" + dirPath;

        System.out.println(outputPath);
        File binFile = new File(outputPath + "/" + contractName + ".bin");
        File abiFile = new File(outputPath + "/" + contractName + ".abi");
        if (binFile.exists()) {
            binFile.delete();
        }
        if (abiFile.exists()) {
            abiFile.delete();
        }

        HashMap<String, String> retMap = new HashMap<>();
        String absolutePath = System.getProperty("user.dir");
        logger.debug("absolutePath: " + absolutePath);
        logger.debug("solFile: " + solFile);
        logger.debug("outputPath: " + outputPath);
        String cmd =
                compile + " --optimize --bin --abi --overwrite " + solFile + " -o "
                        + absolutePath + "/" + outputPath;
        logger.debug("cmd: " + cmd);

        String byteCode = null;
        String abi = null;

        // compile confirmed file
        try {
            exec(cmd);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // get byteCode and ABI
        try {
            byteCode = fileRead(outputPath + "/" + contractName + ".bin", false);
            retMap.put("byteCode", byteCode);
            logger.debug("byteCode: " + byteCode);
            abi = fileRead(outputPath + "/" + contractName + ".abi", false);
            retMap.put("abi", abi);
            logger.debug("abi: " + abi);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retMap;
    }

    /**
     * constructor.
     */
    public static String fileRead(String filePath, boolean isLibrary) throws Exception {
        File file = new File(filePath);
        FileReader reader = new FileReader(file);
        BufferedReader breader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String s = "";
        if (!isLibrary) {
            if ((s = breader.readLine()) != null) {
                sb.append(s);
            }
            breader.close();
        } else {
            String fistLine = breader.readLine();
            breader.readLine();
            if ((s = breader.readLine()) != null && !s.equals("")) {
                s = s.substring(s.indexOf("-> ") + 3);
                sb.append(s + ":");
            } else {
                s = fistLine.substring(fistLine.indexOf("__") + 2, fistLine.lastIndexOf("__"));
                sb.append(s + ":");
            }
            breader.close();
        }
        return sb.toString();
    }

    /**
     * constructor.
     */
    public static HashMap<String, String> getBycodeAbiForLibrary(String solFile,
                                                                 String contractName) {
        HashMap retMap = null;
        String dirPath = solFile.substring(solFile.lastIndexOf("/"), solFile.lastIndexOf("."));
        String outputPath = "src/test/resources/soliditycode/output" + dirPath;
        try {
            retMap = PublicMethed.getByCodeAbi(solFile, contractName);
            String library = fileRead(outputPath + "/" + contractName + ".bin", true);
            retMap.put("library", library);
            logger.debug("library: " + library);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retMap;
    }

    /**
     * constructor.
     */
    public static String triggerConstantContract(byte[] contractAddress, String method,
                                                 String argsStr,
                                                 Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
                                                 byte[] ownerAddress,
                                                 String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        if (argsStr.equalsIgnoreCase("#")) {
            logger.info("argsstr is #");
            argsStr = "";
        }

        byte[] owner = ownerAddress;
        byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(input));
        builder.setCallValue(callValue);
        builder.setTokenId(Long.parseLong(tokenId));
        builder.setCallTokenValue(tokenValue);
        Contract.TriggerSmartContract triggerContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull
                .triggerConstantContract(triggerContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create call trx failed!");
            System.out.println("Code = " + transactionExtention.getResult().getCode());
            System.out
                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0
                && transactionExtention.getConstantResult(0) != null
                && transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            System.out.println("message:" + transaction.getRet(0).getRet());
            System.out.println(":" + ByteArray
                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            System.out.println("Result:" + Hex.toHexString(result));
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "trigger txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData()
                        .toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        }
    }

    /**
     * constructor.
     */
    public static String clearContractAbi(byte[] contractAddress,
                                          byte[] ownerAddress,
                                          String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;

        Contract.ClearABIContract.Builder builder = Contract.ClearABIContract
                .newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));

        Contract.ClearABIContract clearAbiContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull
                .clearContractABI(clearAbiContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create call trx failed!");
            System.out.println("Code = " + transactionExtention.getResult().getCode());
            System.out
                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return null;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0
                && transactionExtention.getConstantResult(0) != null
                && transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            System.out.println("message:" + transaction.getRet(0).getRet());
            System.out.println(":" + ByteArray
                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            System.out.println("Result:" + Hex.toHexString(result));
            return null;
        }

        final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();
        if (transactionExtention == null) {
            return null;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            return null;
        }
        transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            return null;
        }
        transaction = signTransaction(ecKey, transaction);
        System.out.println(
                "trigger txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData()
                        .toByteArray())));
        GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
        if (response.getResult() == false) {
            return null;
        } else {
            return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        }
    }

    /**
     * constructor.
     */
    public static TransactionExtention clearContractAbiForExtention(byte[] contractAddress,
                                                                    byte[] ownerAddress,
                                                                    String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;

        byte[] owner = ownerAddress;

        Contract.ClearABIContract.Builder builder = Contract.ClearABIContract
                .newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));

        Contract.ClearABIContract clearAbiContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull
                .clearContractABI(clearAbiContract);
        return transactionExtention;

    }

    /**
     * constructor.
     */
    public static TransactionExtention triggerConstantContractForExtention(byte[] contractAddress,
                                                                           String method,
                                                                           String argsStr,
                                                                           Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
                                                                           byte[] ownerAddress,
                                                                           String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        if (argsStr.equalsIgnoreCase("#")) {
            logger.info("argsstr is #");
            argsStr = "";
        }

        byte[] owner = ownerAddress;
        byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(input));
        builder.setCallValue(callValue);
        builder.setTokenId(Long.parseLong(tokenId));
        builder.setCallTokenValue(tokenValue);
        Contract.TriggerSmartContract triggerContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull
                .triggerConstantContract(triggerContract);
        return transactionExtention;

    }

    /**
     * constructor.
     */

    public static TransactionExtention triggerContractForExtention(byte[] contractAddress,
                                                                   String method, String argsStr,
                                                                   Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
                                                                   byte[] ownerAddress,
                                                                   String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ECKey ecKey = temKey;
        if (argsStr.equalsIgnoreCase("#")) {
            logger.info("argsstr is #");
            argsStr = "";
        }

        byte[] owner = ownerAddress;
        byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(input));
        builder.setCallValue(callValue);
        builder.setTokenId(Long.parseLong(tokenId));
        builder.setCallTokenValue(tokenValue);
        Contract.TriggerSmartContract triggerContract = builder.build();

        TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
        return transactionExtention;

    }

    /**
     * constructor.
     */
    public static String create2(String[] parameters) {
        if (parameters == null || parameters.length != 3) {
            logger.error("create2 needs 3 parameter:\ncreate2 address code salt");
            return null;
        }

        byte[] address = WalletClient.decodeFromBase58Check(parameters[0]);
        if (!WalletClient.addressValid(address)) {
            logger.error("length of address must be 23 bytes.");
            return null;
        }

        byte[] code = Hex.decode(parameters[1]);
        byte[] temp = Longs.toByteArray(Long.parseLong(parameters[2]));
        if (temp.length != 8) {
            logger.error("Invalid salt!");
            return null;
        }
        byte[] salt = new byte[32];
        System.arraycopy(temp, 0, salt, 24, 8);

        byte[] mergedData = ByteUtil.merge(address, salt, Hash.sha3(code));
        String create2Address = Base58.encode58Check(Hash.sha3omit12(mergedData));

        logger.info("create2 Address: " + create2Address);

        return create2Address;
    }

}