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
import java.util.stream.IntStream;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Slf4j
public class PrivKeyToPubKey {

    @Test
    public void common() {
        int[] blockFilledSlots = new int[]{1, 0, 3, 0, 1, 0};
        double[] sum = IntStream.of(blockFilledSlots).asDoubleStream().toArray();
    }

    @Test
    public void privKeyToPubKey() {
        String privStr = "FD5BB82CBCB378740082FD9E8DC3CC3A959FCD8176CFD829D403D1AF3ED0DDA8";
        BigInteger privKey = new BigInteger(privStr, 16);

        ECKey ecKey = new ECKey(new SecureRandom());
        byte[] address = ecKey.getAddress();
        String pubkey = Wallet.encode58Check(address);

        logger.info("------------ kayfhan ---------------------------------");
        System.out.println();
        System.out.println(Hex.toHexString(ecKey.getPrivKeyBytes()));
        System.out.println(Hex.toHexString(address));
        System.out.println(pubkey);
        System.out.println("01f80c6145c6b6ebb0a7a87a8ce1ef9ae8f21a7d5b24e7".length());
        logger.info("------------------------------------------------------");
    }

    @Test
    public void testss() {
        ECKey ecKey = ECKey.fromPrivate(new BigInteger("374f8288a296d5267bc8157b2eead62fb7a882bfbd2fa132bebe0dd5e9332925", 16));
        String address = Hex.toHexString(ecKey.getAddress());
        System.out.println(address);
    }

    @Test
    public void testGSCAddress() {
        System.out.println(Hex.toHexString(Wallet.decodeFromBase58Check("GSCTwspG3R4rDpArUu29WBNP6AqrAkZfP3yt")));
        for (int i = 0; i < 1; i++) {
            ECKey ecKey = new ECKey(new SecureRandom());

            System.out.println(Hex.toHexString(ecKey.getPrivKeyBytes()));
            String address = Wallet.encode58Check(ecKey.getAddress());
            System.out.println(address);
        }
    }

    @Test
    public void listnode() {
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        channel = ManagedChannelBuilder.forTarget("39.105.202.12:5021").usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        GrpcAPI.AssetIssueList block1 = blockingStub.getAssetIssueByAccount(Protocol.Account.newBuilder().build());
        Protocol.Transaction transaction = blockingStub.getTransactionById(GrpcAPI.BytesMessage.newBuilder().setValue(ByteString.copyFrom(Hex.decode(""))).build());
    }

    @Test
    public void nowBlock() {
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        channel = ManagedChannelBuilder.forTarget("39.105.202.12:5021").usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Protocol.Block block1 = blockingStub.getBlockByNum(GrpcAPI.NumberMessage.newBuilder().setNum(91692L).build());
        System.out.println(block1.toString());

        Protocol.Block block2 = blockingStub.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        System.out.println(block2.toString());

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
        String startNode = "39.105.194.199:5021";
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
            return;
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

        byte[] toAddress = Hex.decode("01f80c96cc8cd2dc6f60f6551728b8204ee65a4f31b39b");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "39.105.194.199:5021";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        ECKey toEcKey = new ECKey(new SecureRandom());
        System.out.println(Hex.toHexString(toEcKey.getPrivKeyBytes()));

        Contract.TransferContract.Builder transferContract = Contract.TransferContract.newBuilder();
        transferContract.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        transferContract.setToAddress(ByteString.copyFrom(toAddress));
        transferContract.setAmount(1L);

        Protocol.Transaction transaction = blockingStub.createTransaction(transferContract.build());

        System.out.println(transaction.toString());

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
        String trxId = PublicMethed.triggerContract(contractAddress, method, argsStr, isHex, callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey, blockingStubFull);
        System.out.println(trxId);
    }

    @Test
    public void add() {

        System.out.println(Wallet.encode58Check(Hex.decode("01f80c992fb573850cd88e7cc80cde58dcaf1afb1e9dde")));
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
        String trxId = PublicMethed.triggerContract(contractAddress, method, argsStr, isHex, callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey, blockingStubFull);
        System.out.println(trxId);
    }

    @Test
    public void deployContract() {
        String priKey = "374f8288a296d5267bc8157b2eead62fb7a882bfbd2fa132bebe0dd5e9332925";
        BigInteger key = new BigInteger(priKey, 16);
        ECKey ecKey = ECKey.fromPrivate(key);
        byte[] address = ecKey.getAddress();

        System.out.println(Hex.toHexString(address));

        String node = "47.74.14.144:5021";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStubFull = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channel);

        String name = "FS";
        String abi = "[\n" +
                "    {\n" +
                "      \"constant\": true,\n" +
                "    }\n" +
                "  ]";
        String code = "608060405261030060405190810160405280600560ff168152602001600060ff168152602001600160ff168152602001600660ff168152602001600260ff168152602001600160ff168152602001600060ff168152602001600460ff168152602001600060ff168152602001600260ff168152602001600360ff168152602001600060ff168152602001600560ff168152602001600060ff168152602001600160ff168152602001600360ff168152602001600260ff168152602001600160ff168152602001600060ff168152602001600460ff168152602001600060ff168152602001600260ff168152602001600360ff168152602001600160ff168152506002906018620001119291906200021e565b5060e06040519081016040528061014c61ffff1681526020016101d161ffff16815260200161024661ffff16815260200161030861ffff16815260200161048c61ffff16815260200161048c61ffff16815260200161091861ffff16815250600390600762000182929190620002bf565b506001600a60006101000a81548160ff0219169083151502179055506001600b556001600c556103cb600d55348015620001bb57600080fd5b50d38015620001c957600080fd5b50d28015620001d757600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555062000365565b826018601f01602090048101928215620002ac5791602002820160005b838211156200027b57835183826101000a81548160ff021916908360ff16021790555092602001926001016020816000010492830192600103026200023b565b8015620002aa5782816101000a81549060ff02191690556001016020816000010492830192600103026200027b565b505b509050620002bb91906200030a565b5090565b8260078101928215620002f7579160200282015b82811115620002f6578251829061ffff16905591602001919060010190620002d3565b5b5090506200030691906200033d565b5090565b6200033a91905b808211156200033657600081816101000a81549060ff02191690555060010162000311565b5090565b90565b6200036291905b808211156200035e57600081600090555060010162000344565b5090565b90565b61284b80620003756000396000f3006080604052600436106100e6576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806302fb0c5e146100eb57806325216cf4146101345780632e1a7d4d14610191578063372d6b27146101d85780635c622a0e146102245780638da5cb5b14610283578063a38a1170146102f4578063a8365f6114610345578063accd55a114610376578063be90cb3e1461042b578063c03366291461049c578063cbe2e97e146104a6578063d5a0d76f146104ef578063df1827df1461054c578063f19ad22014610623578063f2fde38b146106a2575b600080fd5b3480156100f757600080fd5b50d3801561010457600080fd5b50d2801561011157600080fd5b5061011a6106ff565b604051808215151515815260200191505060405180910390f35b34801561014057600080fd5b50d3801561014d57600080fd5b50d2801561015a57600080fd5b5061018f600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610712565b005b34801561019d57600080fd5b50d380156101aa57600080fd5b50d280156101b757600080fd5b506101d6600480360381019080803590602001909291905050506107c7565b005b3480156101e457600080fd5b50d380156101f157600080fd5b50d280156101fe57600080fd5b5061020761086c565b604051808381526020018281526020019250505060405180910390f35b34801561023057600080fd5b50d3801561023d57600080fd5b50d2801561024a57600080fd5b50610269600480360381019080803590602001909291905050506108dc565b604051808215151515815260200191505060405180910390f35b34801561028f57600080fd5b50d3801561029c57600080fd5b50d280156102a957600080fd5b506102b2610949565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6103436004803603810190808060e0019060078060200260405190810160405280929190826007602002808284378201915050505050919291929080351515906020019092919050505061096e565b005b34801561035157600080fd5b50d3801561035e57600080fd5b50d2801561036b57600080fd5b506103746118cb565b005b34801561038257600080fd5b50d3801561038f57600080fd5b50d2801561039c57600080fd5b50610411600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050611c7a565b604051808215151515815260200191505060405180910390f35b34801561043757600080fd5b50d3801561044457600080fd5b50d2801561045157600080fd5b50610486600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050611da9565b6040518082815260200191505060405180910390f35b6104a4611df2565b005b3480156104b257600080fd5b50d380156104bf57600080fd5b50d280156104cc57600080fd5b506104ed600480360381019080803515159060200190929190505050611fea565b005b3480156104fb57600080fd5b50d3801561050857600080fd5b50d2801561051557600080fd5b5061054a600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050612066565b005b34801561055857600080fd5b50d3801561056557600080fd5b50d2801561057257600080fd5b5061057b61211c565b604051808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001935050505060405180910390f35b34801561062f57600080fd5b50d3801561063c57600080fd5b50d2801561064957600080fd5b50610688600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506121f4565b604051808215151515815260200191505060405180910390f35b3480156106ae57600080fd5b50d380156106bb57600080fd5b50d280156106c857600080fd5b506106fd600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506122f4565b005b600a60009054906101000a900460ff1681565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561076d57600080fd5b60018060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060006101000a81548160ff02191690831515021790555050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561082257600080fd5b3373ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f19350505050158015610868573d6000803e3d6000fd5b5050565b60008060011515600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff1615151415156108ce57600080fd5b600b54600c54915091509091565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561093957600080fd5b81600d8190555060019050919050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008060008060008060006109816127fc565b600060011515600a60009054906101000a900460ff161515141515610a0e576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601a8152602001807f736f7272792c2067616d65206973206e6f74206163746976652e00000000000081525060200191505060405180910390fd5b6000803391503273ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16141515610a4e57600080fd5b813b9050600081141515610aca576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260138152602001807f736f7272792c206f6e6c792068756d616e73210000000000000000000000000081525060200191505060405180910390fd5b8c9350600092505b60078360ff161015610b3c576000848460ff16600781101515610af157fe5b60200201511415610b055784806001019550505b610b2d848460ff16600781101515610b1957fe5b60200201518961244990919063ffffffff16565b97508280600101935050610ad2565b600085118015610b4c5750600785105b1515610be6576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260328152602001807f626574206f7074696f6e73206d757374206265206d6f7265207468616e20302c81526020017f20616e6420736d617274207468616e20372e000000000000000000000000000081525060400191505060405180910390fd5b610bf133858a61246a565b9a508b15611103573488141515610c96576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260328152602001807f736c6f7420616d6f756e74204f6620747278206d75737420626520657175616c81526020017f20746f2070617961626c652076616c75652e000000000000000000000000000081525060400191505060405180910390fd5b6301312d008810158015610caf575064012a05f2008811155b1515610d49576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603d8152602001807f736c6f7420616d6f756e74204f6620747278206d757374206265206d6f72652081526020017f7468616e2032302c20616e6420736d617274207468616e20353030302e00000081525060400191505060405180910390fd5b610d5e88600b5461244990919063ffffffff16565b600b81905550610e4c620f4240610e3e8a600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166383480cec6040518163ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401602060405180830381600087803b158015610df557600080fd5b505af1158015610e09573d6000803e3d6000fd5b505050506040513d6020811015610e1f57600080fd5b810190808051906020019092919050505061275b90919063ffffffff16565b61279990919063ffffffff16565b9650610ea087600e60003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205461244990919063ffffffff16565b600e60003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000208190555060028b601881101515610ef257fe5b602091828204019190069054906101000a900460ff169850610f326103e8610f2460158b61275b90919063ffffffff16565b61279990919063ffffffff16565b9550601160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166108fc879081150290604051600060405180830381858888f19350505050158015610f9c573d6000803e3d6000fd5b506000848a60ff16600781101515610fb057fe5b60200201511415156110fe5761100a6064610ffc60038c60ff16600781101515610fd657fe5b0154878d60ff16600781101515610fe957fe5b602002015161275b90919063ffffffff16565b61279990919063ffffffff16565b9950893073ffffffffffffffffffffffffffffffffffffffff16311015151561109b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601e8152602001807f636f6e74726163742062616c616e636520696e73756666696369656e742e000081525060200191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff166108fc8b9081150290604051600060405180830381858888f193505050501580156110e1573d6000803e3d6000fd5b506110f78a600c5461244990919063ffffffff16565b600c819055505b611875565b620f4240881015801561111b57506402540be4008811155b15156111b5576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603e8152602001807f736c6f7420616d6f756e74204f66205550206d757374206265206d6f7265207481526020017f68616e203130302c20616e6420736d617274207468616e2031303030302e000081525060400191505060405180910390fd5b87600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166370a08231336040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001915050602060405180830381600087803b15801561127357600080fd5b505af1158015611287573d6000803e3d6000fd5b505050506040513d602081101561129d57600080fd5b8101908080519060200190929190505050101515156112bb57600080fd5b60028b6018811015156112ca57fe5b602091828204019190069054906101000a900460ff16985061130960646112fb60038b61275b90919063ffffffff16565b61279990919063ffffffff16565b9550600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16632944252533886040518363ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b1580156113d057600080fd5b505af11580156113e4573d6000803e3d6000fd5b505050506040513d60208110156113fa57600080fd5b8101908080519060200190929190505050506000848a60ff1660078110151561141f57fe5b602002015114151561170a57611479606461146b60038c60ff1660078110151561144557fe5b0154878d60ff1660078110151561145857fe5b602002015161275b90919063ffffffff16565b61279990919063ffffffff16565b9950878a111561159b57600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166352491d77336114d58b8e6127e090919063ffffffff16565b6040518363ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b15801561155a57600080fd5b505af115801561156e573d6000803e3d6000fd5b505050506040513d602081101561158457600080fd5b810190808051906020019092919050505050611705565b600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663d891706233600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff166116108e8d6127e090919063ffffffff16565b6040518463ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019350505050602060405180830381600087803b1580156116c857600080fd5b505af11580156116dc573d6000803e3d6000fd5b505050506040513d60208110156116f257600080fd5b8101908080519060200190929190505050505b611874565b600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663d891706233600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1661177f8a8d6127e090919063ffffffff16565b6040518463ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019350505050602060405180830381600087803b15801561183757600080fd5b505af115801561184b573d6000803e3d6000fd5b505050506040513d602081101561186157600080fd5b8101908080519060200190929190505050505b5b7fb48b28073e9ee15e4849baa9fa50e7c812c69be8720118d98178dd276e5836438b888c60405180848152602001838152602001828152602001935050505060405180910390a150505050505050505050505050565b60011515600a60009054906101000a900460ff161515141515611956576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601a8152602001807f736f7272792c2067616d65206973206e6f74206163746976652e00000000000081525060200191505060405180910390fd5b6000803391503273ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614151561199657600080fd5b813b9050600081141515611a12576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260138152602001807f736f7272792c206f6e6c792068756d616e73210000000000000000000000000081525060200191505060405180910390fd5b620f4240600e60003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054111515611af1576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260238152602001807f62616c616e6365206f66205550206d757374206265206d6f7265207468616e2081526020017f31302e000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166352491d7733600e60003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020546040518363ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b158015611bf557600080fd5b505af1158015611c09573d6000803e3d6000fd5b505050506040513d6020811015611c1f57600080fd5b8101908080519060200190929190505050506000600e60003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055505050565b600060011515600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff161515141515611cdb57600080fd5b83600f60006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555082601060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555081601160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550600190509392505050565b6000600e60008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020549050919050565b6000806000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16141515611e5057600080fd5b611e6434601e61275b90919063ffffffff16565b9150611eb882600e60003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205461244990919063ffffffff16565b600e60003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550611f226064611f1460033461275b90919063ffffffff16565b61279990919063ffffffff16565b9050601160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f19350505050158015611f8c573d6000803e3d6000fd5b503373ffffffffffffffffffffffffffffffffffffffff166108fc611fba83346127e090919063ffffffff16565b9081150290604051600060405180830381858888f19350505050158015611fe5573d6000803e3d6000fd5b505050565b60011515600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff16151514151561204957600080fd5b80600a60006101000a81548160ff02191690831515021790555050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161415156120c157600080fd5b6000600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060006101000a81548160ff02191690831515021790555050565b600080600060011515600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff16151514151561218057600080fd5b600f60009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16601060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16601160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16925092509250909192565b600060011515600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff16151514151561225557600080fd5b6122a782600e60008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205461244990919063ffffffff16565b600e60008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055506001905092915050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561234f57600080fd5b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff161415151561238b57600080fd5b8073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b600080828401905083811015151561246057600080fd5b8091505092915050565b6000806000806000601060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16631ec71426896040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001915050602060405180830381600087803b15801561252f57600080fd5b505af1158015612543573d6000803e3d6000fd5b505050506040513d602081101561255957600080fd5b81019080805190602001909291905050509350600c54418588604051602001808581526020018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166c010000000000000000000000000281526014018381526020018281526020019450505050506040516020818303038152906040526040518082805190602001908083835b60208310151561261657805182526020820191506020810190506020830392506125f1565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040518091039020600190049350600d54612677600b546126696103e8600c5461275b90919063ffffffff16565b61279990919063ffffffff16565b111561273d5760198481151561268957fe5b069350601884141561273857600091505b60078260ff1610156126dc576000878360ff166007811015156126b957fe5b602002015114156126cf578160ff1692506126dc565b818060010192505061269a565b600090505b60188160ff161015612737578260028260ff1660188110151561270057fe5b602091828204019190069054906101000a900460ff1660ff16141561272a578060ff169350612737565b80806001019150506126e1565b5b61274d565b60188481151561274957fe5b0693505b839450505050509392505050565b60008060008414156127705760009150612792565b828402905082848281151561278157fe5b0414151561278e57600080fd5b8091505b5092915050565b6000806000831115156127ab57600080fd5b82848115156127b657fe5b04905082848115156127c457fe5b0681840201841415156127d657600080fd5b8091505092915050565b60008282111515156127f157600080fd5b818303905092915050565b60e0604051908101604052806007906020820280388339808201915050905050905600a165627a7a72305820c2c57d9850fb7ceaa199cfbcc5147ea413ad34e35ac4a522e2908a62eaf408950029";
        long callValue = 0L;
        long feeLimit = 1000000000L;
        long consumeUserResourcePercent = 0;

        byte[] contractAddress = PublicMethed.deployContract(name, abi, code, "",
                feeLimit, callValue, consumeUserResourcePercent, 100000L, "0", 0, null, priKey, address, blockingStubFull);
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