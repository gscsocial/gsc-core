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

package org.gsc.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.gsc.api.DatabaseGrpc;
import org.gsc.api.GrpcAPI;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Slf4j
public class PrivKeyToPubKey {

    @Test
    public void common(){
//        System.out.println(System.nanoTime());

        System.out.println(Hex.toHexString(Wallet.decodeFromBase58Check("GSCgH5ZAQZqFgE9dnoFxRLHjwx6u7uWxkavN")));
    }

    @Test
    public void privKeyToPubKey() {
        String privStr = "a284c5935e33ec2c363913b6cf628da5c81defc2f96afb64690ae7a2f5535620";
        BigInteger privKey = new BigInteger(privStr, 16);

        // Wallet.setAddressPreFixByte((byte) 0x26);
        final ECKey ecKey = ECKey.fromPrivate(privKey);
        byte[] address = ecKey.getAddress();
        String pubkey = Wallet.encode58Check(address);

        logger.info("------------ kayfhan ---------------------------------");
        System.out.println();
        System.out.println("Private Key: " + privStr);
        System.out.println(Hex.toHexString(address));
        System.out.println(pubkey);
        System.out.println("01 f8 0c 6145c6b6ebb0a7a87a8ce1ef9ae8f21a7d5b24e7".length());
        // GSC d6iDrNvqg1kiorwwhoVvarXeucpfbrPYe
        logger.info("------------------------------------------------------");
        //System.out.println(Wallet.encode58Check(Hex.decode("4119edb1b352e40c855eb28681c2b488b7ed8fb5aa")));
    }

    @Test
    public void testGSCAddress() {
        System.out.println(Hex.toHexString(Wallet.decodeFromBase58Check("GSCTwspG3R4rDpArUu29WBNP6AqrAkZfP3yt")));
//        System.out.println(Hex.toHexString(
//                Wallet.decodeFromBase58Check("GSCfPVfutRDeB5T3xaDsxYxASH6P8u7Ydsc3")));
//        System.out.println("GSCd6iDrNvqg1kiorwwhoVvarXeucpfbrPYe".length());
        for (int i = 0; i < 1; i++) {
            ECKey ecKey = new ECKey(new SecureRandom());

            System.out.println(Hex.toHexString(ecKey.getPrivKeyBytes()));
//            System.out.println(Hex.toHexString(ecKey.getAddress()));
            String address = Wallet.encode58Check(ecKey.getAddress());
            System.out.println(address);
//            System.out.println(Hex.toHexString(Wallet.decodeFromBase58Check(address)));
        }

//        ECKey ecKey = new ECKey(new SecureRandom());
//        System.out.println(Hex.toHexString(ecKey.getPrivKeyBytes()));
//        System.out.println(Wallet.encode58Check(ecKey.getAddress()));
    }

    @Test
    public void listnode() {
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        channel = ManagedChannelBuilder.forTarget("39.105.202.12:5021").usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        GrpcAPI.AssetIssueList block1 = blockingStub.getAssetIssueByAccount(Protocol.Account.newBuilder().build());
    }

    @Test
    public void nowBlock(){
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        channel = ManagedChannelBuilder.forTarget("39.105.202.12:5021").usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Protocol.Block block1 = blockingStub.getBlockByNum(GrpcAPI.NumberMessage.newBuilder().setNum(91692L).build());
        System.out.println(block1.toString());

        Protocol.Block block2 = blockingStub.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        System.out.println(block2.toString());



//        byte[] numBytes = Longs.toByteArray(Long.MAX_VALUE);
//        System.out.println("numBytes: " + Hex.toHexString(numBytes));
//        System.out.println(Long.MAX_VALUE);


        // 92233 7203 6854 775 807
        GrpcAPI.BlockExtention block = blockingStub.getBlockByNum2(GrpcAPI.NumberMessage.newBuilder().setNum(91692L).build());
        System.out.println(Hex.toHexString(block.getBlockid().toByteArray()));

        // 0000000000016e003fb0e03b96f1ba11cc092caecd061b6c1d50e91f741dedc3
        String blockHash = Hex.toHexString(new Sha256Hash(block.getBlockHeader().getRawData().getNumber(),
                Sha256Hash.of(block.getBlockHeader().getRawData().toByteArray())).getBytes());
        System.out.println(blockHash);
        System.out.println(ByteArray.toHexString(Sha256Hash.hash(block.getBlockHeader().getRawData().toByteArray())));
    }

    @Test
    public void freezeBalanceForReceiver() {
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

//        String startNode = "101.200.39.156:50051";
        String startNode = "39.105.194.199:5021";
//          String startNode = "127.0.0.1:5021";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        byte[] address = Hex.decode("");
        String priKey = "";
        byte[] receiverAddress = Hex.decode("");
        long frozenBalance = 100000000;
        long frozenDuration = 5;
        int resourceCode = 1;
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
        ByteString byteReceiverAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
                .setFrozenDuration(frozenDuration).setResourceValue(resourceCode);
        builder.setReceiverAddress(byteReceiverAddreess);
        Contract.FreezeBalanceContract contract = builder.build();
        Protocol.Transaction transaction = blockingStub.freezeBalance(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction = null");
            return ;
        }
        Protocol.Transaction.Builder txSigned = transaction.toBuilder();

        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }
        Message message = blockingStub.broadcastTransaction(txSigned.build());
        logger.info("---------------" + message.toString());
    }

    @Test
    public void transferAccount() {
        String ownerPriKey = "a284c5935e33ec2c363913b6cf628da5c81defc2f96afb64690ae7a2f5535620";
        byte[] ownerAddress = Hex.decode("01f80cf6b1ed20174eb4de7202a34bf1975935c1a50432");

        // 26f740d0a396a324b15d68aa8ffeaf2057d937d745
        // 26f740d0a396a324b15d68aa8ffeaf2057d937d745
        byte[] toAddress = Hex.decode("01f80c96cc8cd2dc6f60f6551728b8204ee65a4f31b39b");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

//        String startNode = "101.200.39.156:50051";
        String startNode = "39.105.194.199:5021";
//          String startNode = "127.0.0.1:5021";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

//        for (int k = 0; k < 1; k++) {

            ECKey toEcKey = new ECKey(new SecureRandom());
            System.out.println(Hex.toHexString(toEcKey.getPrivKeyBytes()));

            Contract.TransferContract.Builder transferContract = Contract.TransferContract.newBuilder();
            transferContract.setOwnerAddress(ByteString.copyFrom(ownerAddress));
            transferContract.setToAddress(ByteString.copyFrom(toAddress));
//            transferContract.setToAddress(ByteString.copyFrom(toEcKey.getAddress()));
            transferContract.setAmount(10000_000_000L);

            Protocol.Transaction transaction = blockingStub.createTransaction(transferContract.build());

            Protocol.Transaction.Builder txSigned = transaction.toBuilder();

            byte[] rawData = transaction.getRawData().toByteArray();
            byte[] hash = sha256(rawData);
            List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
            for (int i = 0; i < contractList.size(); i++) {
                ECKey.ECDSASignature signature = ecKey.sign(hash);
                ByteString byteString = ByteString.copyFrom(signature.toByteArray());
                txSigned.addSignature(byteString);
            }
//        System.out.println(txSigned.build().toString());
            Message message = blockingStub.broadcastTransaction(txSigned.build());
            logger.info("---------------" + message.toString());
//        }
    }

    @Test
    public void ddd() {

        ManagedChannel channel = null;
        DatabaseGrpc.DatabaseBlockingStub databaseStub = null;

//        String startNode = "101.200.39.156:50051";
        String startNode = "39.105.194.199:5021";
        //  String startNode = "127.0.0.1:5021";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        databaseStub = DatabaseGrpc.newBlockingStub(channel);



    }

    @Test
    public void voteStatistics() {
        /**
         * 7.22 20.26
         * Witness: 2603fd0fd06b10768f120cc5911194e35ed4d60195 vote count: 3000
         * Witness: 26ebcd9b60d930b603a4fec7d188275c61e1676602 vote count: 1000
         * Witness: 2663ab67ead97e4f48de58f76e3d32a72d5b774251 vote count: 2000
         * Witness: 26458beb1e3e46c0574c1eba16b4f7081e8bafe98b vote count: 4000
         * 103025
         * 200000
         */
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "127.0.0.1:5021";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        GrpcAPI.VoteStatistics voteStatistics = blockingStub.getWitnessVoteStatistics(GrpcAPI.EmptyMessage.newBuilder().build());
        voteStatistics.getVotesList().forEach(vote -> {
            ByteString address = vote.getVoteAddress();
            long count = vote.getVoteCount();
            System.out.println("Witness: " + Hex.toHexString(address.toByteArray()) + " vote count: " + count);
        });
    }

    @Test
    public void triggerContract() {
        String priKey = "374f8288a296d5267bc8157b2eead62fb7a882bfbd2fa132bebe0dd5e9332925";
        BigInteger key = new BigInteger(priKey, 16);
        ECKey ecKey = ECKey.fromPrivate(key);
        byte[] address = ecKey.getAddress();

        System.out.println(Hex.toHexString(address));

//        String node = "39.105.135.130:5021";
        String node = "47.74.14.144:5021";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStubFull = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channel);

        /**
         * triggerContract(byte[] contractAddress, String method, String argsStr,
         *       Boolean isHex, long callValue, long feeLimit, byte[] ownerAddress,
         *       String priKey, WalletGrpc.WalletBlockingStub blockingStubFull)
         */
        byte[] contractAddress = Hex.decode("01f80c69a08f85ec0152df249f6b1d7ffd3df7169b8e89");
        Protocol.SmartContract smartContract = blockingStubFull.getContract(GrpcAPI.BytesMessage.newBuilder().setValue(ByteString.copyFrom(contractAddress)).build());
        // System.out.println("Smart contract: " + smartContract.toString());

        String method = "TransferTokenTo(address,grcToken,uint256)";
        String argsStr = "\"GSCVh64dLR83eZRX4hR9zoLHUFd1jVzwT5h5\", 1000001, 9";
        Boolean isHex = false;
        long callValue = 0L;
        long feeLimit = 100000000L;
        byte[] ownerAddress = address;
        String tokenId = "1000001";
        long tokenValue = 8;
        String trxId = PublicMethed.triggerContract(contractAddress, method, argsStr, isHex, callValue, feeLimit, tokenId, tokenValue,ownerAddress, priKey, blockingStubFull);
        System.out.println(trxId);
    }

    @Test
    public void triggerContract2() {
        String priKey = "374f8288a296d5267bc8157b2eead62fb7a882bfbd2fa132bebe0dd5e9332925";
        BigInteger key = new BigInteger(priKey, 16);
        ECKey ecKey = ECKey.fromPrivate(key);
        byte[] address = ecKey.getAddress();
//        String node = "39.105.135.130:5021";
        String node = "47.74.14.144:5021";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStubFull = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channel);

        byte[] contractAddress = Hex.decode("01f80c992fb573850cd88e7cc80cde58dcaf1afb1e9dde");
        Protocol.SmartContract smartContract = blockingStubFull.getContract(GrpcAPI.BytesMessage.newBuilder().setValue(ByteString.copyFrom(contractAddress)).build());
        // System.out.println("Smart contract: " + smartContract.toString());

        String method = "slotting(uint256,uint256,bool)";
        String argsStr = "20000000,90,true";
        Boolean isHex = false;
        long callValue = 20000000L;
        long feeLimit = 100000000L;
        byte[] ownerAddress = address;
        String tokenId = "0";
        long tokenValue = 0;
        String trxId = PublicMethed.triggerContract(contractAddress, method, argsStr, isHex, callValue, feeLimit, tokenId, tokenValue,ownerAddress, priKey, blockingStubFull);
        System.out.println(trxId);
    }

    @Test
    public void deployContract() {
        String priKey = "374f8288a296d5267bc8157b2eead62fb7a882bfbd2fa132bebe0dd5e9332925";
        BigInteger key = new BigInteger(priKey, 16);
        ECKey ecKey = ECKey.fromPrivate(key);
        byte[] address = ecKey.getAddress();

        System.out.println(Hex.toHexString(address));

//         String node = "39.105.194.199:5021";
        String node = "47.74.14.144:5021";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStubFull = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channel);

        String name = "Dice";
        String abi = "[\n" +
                "\t{\n" +
                "\t\t\"constant\": false,\n" +
                "\t\t\"inputs\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"name\": \"amount\",\n" +
                "\t\t\t\t\"type\": \"uint256\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"name\": \"withdraw\",\n" +
                "\t\t\"outputs\": [],\n" +
                "\t\t\"payable\": false,\n" +
                "\t\t\"stateMutability\": \"nonpayable\",\n" +
                "\t\t\"type\": \"function\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"constant\": true,\n" +
                "\t\t\"inputs\": [],\n" +
                "\t\t\"name\": \"owner\",\n" +
                "\t\t\"outputs\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"name\": \"\",\n" +
                "\t\t\t\t\"type\": \"address\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"payable\": false,\n" +
                "\t\t\"stateMutability\": \"view\",\n" +
                "\t\t\"type\": \"function\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"constant\": false,\n" +
                "\t\t\"inputs\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"name\": \"_amount\",\n" +
                "\t\t\t\t\"type\": \"uint256\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"name\": \"_prediction\",\n" +
                "\t\t\t\t\"type\": \"uint256\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"name\": \"_isOver\",\n" +
                "\t\t\t\t\"type\": \"bool\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"name\": \"slotting\",\n" +
                "\t\t\"outputs\": [],\n" +
                "\t\t\"payable\": true,\n" +
                "\t\t\"stateMutability\": \"payable\",\n" +
                "\t\t\"type\": \"function\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"constant\": false,\n" +
                "\t\t\"inputs\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"name\": \"newOwner\",\n" +
                "\t\t\t\t\"type\": \"address\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"name\": \"transferOwnership\",\n" +
                "\t\t\"outputs\": [],\n" +
                "\t\t\"payable\": false,\n" +
                "\t\t\"stateMutability\": \"nonpayable\",\n" +
                "\t\t\"type\": \"function\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"inputs\": [],\n" +
                "\t\t\"payable\": false,\n" +
                "\t\t\"stateMutability\": \"nonpayable\",\n" +
                "\t\t\"type\": \"constructor\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"anonymous\": false,\n" +
                "\t\t\"inputs\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"indexed\": false,\n" +
                "\t\t\t\t\"name\": \"random\",\n" +
                "\t\t\t\t\"type\": \"uint256\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"indexed\": false,\n" +
                "\t\t\t\t\"name\": \"mintToken\",\n" +
                "\t\t\t\t\"type\": \"uint256\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"indexed\": false,\n" +
                "\t\t\t\t\"name\": \"reward\",\n" +
                "\t\t\t\t\"type\": \"uint256\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"name\": \"DiceEvent\",\n" +
                "\t\t\"type\": \"event\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"anonymous\": false,\n" +
                "\t\t\"inputs\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"indexed\": true,\n" +
                "\t\t\t\t\"name\": \"previousOwner\",\n" +
                "\t\t\t\t\"type\": \"address\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"indexed\": true,\n" +
                "\t\t\t\t\"name\": \"newOwner\",\n" +
                "\t\t\t\t\"type\": \"address\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"name\": \"OwnershipTransferred\",\n" +
                "\t\t\"type\": \"event\"\n" +
                "\t}\n" +
                "]";
        String code = "60806040526001600060146101000a81548160ff02191690831515021790555034801561002b57600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550610bac8061007b6000396000f300608060405260043610610062576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632e1a7d4d146100675780638da5cb5b14610094578063dfad6b33146100eb578063f2fde38b14610121575b600080fd5b34801561007357600080fd5b5061009260048036038101908080359060200190929190505050610164565b005b3480156100a057600080fd5b506100a9610209565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b61011f600480360381019080803590602001909291908035906020019092919080351515906020019092919050505061022e565b005b34801561012d57600080fd5b50610162600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506109b6565b005b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161415156101bf57600080fd5b3373ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f19350505050158015610205573d6000803e3d6000fd5b5050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b600080600080600060011515600060149054906101000a900460ff1615151415156102c1576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601a8152602001807f736f7272792c2067616d65206973206e6f74206163746976652e00000000000081525060200191505060405180910390fd5b6000803391503273ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614151561030157600080fd5b813b905060008114151561037d576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260138152602001807f736f7272792c206f6e6c792068756d616e73210000000000000000000000000081525060200191505060405180910390fd5b6001891015801561038f575060628911155b1515610429576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260288152602001807f70726564696374696f6e206973206d6f7265207468616e20312c206c6573732081526020017f7468616e2039382e00000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b89965088955034871415156104cc576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260328152602001807f736c6f7420616d6f756e74204f6620747278206d75737420626520657175616c81526020017f20746f2070617961626c652076616c75652e000000000000000000000000000081525060400191505060405180910390fd5b6301312d0087101580156104e557506402540be4008711155b151561057f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603d8152602001807f736c6f7420616d6f756e74204f6620747278206d757374206265206d6f72652081526020017f7468616e2032302c20616e6420736d617274207468616e20353030302e00000081525060400191505060405180910390fd5b6064428860405160200180838152602001828152602001925050506040516020818303038152906040526040518082805190602001908083835b6020831015156105de57805182526020820191506020810190506020830392506105b9565b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910390206001900481151561061857fe5b06945087156108025760038610158015610633575060628611155b15156106cd576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603d8152602001807f736c6f7420616d6f756e74204f6620747278206d757374206265206d6f72652081526020017f7468616e2032302c20616e6420736d617274207468616e20353030302e00000081525060400191505060405180910390fd5b858511156107fd57610724612710610716896107086106f68b6063610b0b90919063ffffffff16565b620ecd10610b2790919063ffffffff16565b610b4290919063ffffffff16565b610b2790919063ffffffff16565b9350833073ffffffffffffffffffffffffffffffffffffffff1631101515156107b5576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601e8152602001807f636f6e74726163742062616c616e636520696e73756666696369656e742e000081525060200191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff166108fc859081150290604051600060405180830381858888f193505050501580156107fb573d6000803e3d6000fd5b505b610963565b60018610158015610814575060608611155b15156108ae576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603d8152602001807f736c6f7420616d6f756e74204f6620747278206d757374206265206d6f72652081526020017f7468616e2032302c20616e6420736d617274207468616e20353030302e00000081525060400191505060405180910390fd5b85851015610962576108f26127106108e4896108d68a620ecd10610b2790919063ffffffff16565b610b4290919063ffffffff16565b610b2790919063ffffffff16565b9350833073ffffffffffffffffffffffffffffffffffffffff16311015151561091a57600080fd5b3373ffffffffffffffffffffffffffffffffffffffff166108fc859081150290604051600060405180830381858888f19350505050158015610960573d6000803e3d6000fd5b505b5b7f819522fb1fea853c488e19d40d027510a0b7de452725c9de716d64a981957ddb85848660405180848152602001838152602001828152602001935050505060405180910390a150505050505050505050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16141515610a1157600080fd5b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614151515610a4d57600080fd5b8073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b6000828211151515610b1c57600080fd5b818303905092915050565b6000808284811515610b3557fe5b0490508091505092915050565b6000806000841415610b575760009150610b79565b8284029050828482811515610b6857fe5b04141515610b7557600080fd5b8091505b50929150505600a165627a7a72305820c52eebfb5e26c1acdc21cf66da60112f3927b04f644e4537cb55f7d8caa67c660029";
        long callValue = 0L;
        long feeLimit = 1000000000L;
        long consumeUserResourcePercent = 0;

        byte[] contractAddress = PublicMethed.deployContract(name, abi, code, "",
                feeLimit, callValue, consumeUserResourcePercent, 1000L ,"0", 0,null, priKey, address, blockingStubFull);
        logger.info("Contract address: " + Hex.toHexString(contractAddress));

        GrpcAPI.AccountResourceMessage accountResource = PublicMethed.getAccountResource(address, blockingStubFull);
        long freeNet = accountResource.getFreeNetUsed();
        System.out.println("freeNet" + freeNet);
        long cpuUsage = accountResource.getCpuUsed();
        System.out.println("cpuUsage" + cpuUsage);
        Protocol.Account account = PublicMethed.queryAccount(priKey, blockingStubFull);
        System.out.println("after balance is " + account.getBalance());
        long netUsed = account.getNetUsage();
        System.out.println("after net used is " + Long.toString(netUsed));
    }

    @Test
    public void witness() {
        String ownerPriKey = "4b4bf6766644bc9e0352a494037f67b4b971ae2f7bb9bd0a495194c7e822f7a4";
        byte[] ownerAddress = Hex.decode("01f80c458beb1e3e46c0574c1eba16b4f7081e8bafe98b");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

//        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

//        String startNode = "101.200.39.156:50051";
        String startNode = "39.105.194.199:5021";
        //String startNode = "192.168.130.9:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Contract.WitnessCreateContract.Builder witness = Contract.WitnessCreateContract.newBuilder();
        witness.setOwnerAddress(ByteString.copyFrom(ecKey.getAddress()));
        witness.setUrl(ByteString.copyFromUtf8("https://kay.com"));

        Protocol.Transaction transaction = blockingStub.createWitness(witness.build());

        Protocol.Transaction.Builder txSigned = transaction.toBuilder();

        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }

        Message message = blockingStub.broadcastTransaction(txSigned.build());
        logger.info(message.toString());
    }

    @Test
    public void updateSettingContract() {
        /**
         * SoltRandom:
         *     (base58) TSw3hKyjHMvrqEYZWXFWi6UzhyyHBiejYS
         *     (hex) 41ba1189d3cd3b2a3f2d46ebd12bf5f72f0efd32f1
         */
        String updateSettingPriKey = "58385633f1be9acf6e18e74aab87d5597b23f38745577f7ecd9f8ddc6c3783c5";
        byte[] contractAddress = Hex.decode("41452924d5d180b1564f8e2b5da2cd604e42600e8d");

        BigInteger privKey = new BigInteger(updateSettingPriKey, 16);

//        Wallet.setAddressPreFixByte(Byte.decode("0x41"));
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "47.254.77.146:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Contract.UpdateSettingContract.Builder updateSettingContract = Contract.UpdateSettingContract.newBuilder();
        updateSettingContract.setOwnerAddress(ByteString.copyFrom(ecKey.getAddress()));
        updateSettingContract.setContractAddress(ByteString.copyFrom(contractAddress));
        updateSettingContract.setConsumeUserResourcePercent(0);


        GrpcAPI.TransactionExtention transactionExtention = blockingStub.updateSetting(updateSettingContract.build());
        //GrpcAPI.TransactionExtention transactionExtentions = blockingStub.triggerContract();

        Protocol.Transaction transaction = transactionExtention.getTransaction();

        Protocol.Transaction.Builder txSigned = transaction.toBuilder();
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }

        Message message = blockingStub.broadcastTransaction(txSigned.build());

        logger.info(message.toString());
    }

    @Test
    public void getTransactionInfoById() {
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "192.168.130.9:50061";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Protocol.TransactionInfo transactionInfo = blockingStub.getTransactionInfoById(GrpcAPI.BytesMessage.
                newBuilder().setValue(ByteString.copyFrom(Hex.decode("db2fce157b3024e1c150cda6aeea56b8af87d80e2368feb14843f95fa2eaa0bf"))).build());
        System.out.println("Transaction Info: " + transactionInfo.toString());
    }

    @Test
    public void listwitnesses() {
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "39.105.194.199:5021";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        GrpcAPI.WitnessList witnessList = blockingStub.listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
        System.out.println("Transaction Info: \n" + witnessList.toString());
        for (Protocol.Witness witness : witnessList.getWitnessesList()) {
            System.out.println(Hex.toHexString(witness.getAddress().toByteArray()));
        }
    }

}