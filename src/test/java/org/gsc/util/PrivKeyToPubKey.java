package org.gsc.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.gsc.api.GrpcAPI;
import org.gsc.api.WalletGrpc;
import org.gsc.common.overlay.Parameter;
import org.gsc.common.overlay.client.RPCUtils;
import org.gsc.common.overlay.client.WalletGrpcClient;
import org.gsc.common.overlay.discover.table.NodeEntry;
import org.gsc.common.utils.Utils;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.crypto.ECKey;
import org.gsc.db.Manager;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.services.http.JsonFormat;
import org.gsc.services.http.Util;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.CollectionUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Slf4j
public class PrivKeyToPubKey {



    @Test
    public void privKeyToPubKey() {
        String privStr = "F603197654386A796418913AB29BAC9198DE871AE04FE47D185F716D2145F9CC";
        BigInteger privKey = new BigInteger(privStr, 16);

        Wallet.setAddressPreFixByte((byte) 0x26);
        final ECKey ecKey = ECKey.fromPrivate(privKey);
        byte[] address = ecKey.getAddress();

        String pubkey = Wallet.encode58Check(address);
        byte[] decodeAddr = Wallet.decodeFromBase58Check(pubkey);

        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Private Key: " + privStr);
        System.out.println("Address(Base58): " + Hex.toHexString(address));
        System.out.println("Public  Key: " + Hex.toHexString(ecKey.getPubKey()));
        System.out.println("Public  Key(Base58): " + pubkey);
        System.out.println();
        logger.info("---------------------------------------------");

        // String b="26cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
        // String b="307830303030303030303030303030303030303030
        // String b="3078303030303030303030303030303030303030303030";
        String g="3078666666666666666666666666666666666666666666";
       // String Base58Address = "GNPhKboo7ez2MDH88qnwLGM5Vwr5vYSwb6";
        //byte[] Baddress = Wallet.decodeFromBase58Check(Base58Address);
        String Gaddress = Wallet.encode58Check(Hex.decode(g));

        System.out.println(Hex.toHexString(Wallet.decodeFromBase58Check("GNL185SmY7Bj14af1wi7QefPJeuLn3rydg")));
        logger.info("Baddress Key: " + Hex.toHexString(address));
        logger.info("Gaddress Key: " + Gaddress);
    }

    @Test
    public void aAddress(){
        String w="3078666666666666666666666666666666666666666666";
        logger.info(ByteString.copyFrom( "0xfffffffffffffffffffff".getBytes()).toStringUtf8());
        logger.info(Wallet.encode58Check("0x000000000000000000000".getBytes()));
        logger.info(Wallet.encode58Check(Hex.decode(w)));
        String t="0xfffffffffffffffffffff";
        String c="7YxAaK71utTpYJ8u4Zna7muWxd1pQwimpGxy8";
        String g="7YxAaK71utTpYJ8u4Zna7muWxd1pQwimpGxy8";


        String pubkey="41206e65772073797374656d206d75737420616c6c6f77206578697374696e672073797374656d7320746f206265206c696e6b656420746f67657468657220776974686f757420726571756972696e6720616e792063656e7472616c20636f6e74726f6c206f7220636f6f7264696e6174696f6e";

        ECKey eck = ECKey.fromPublicOnly(pubkey.getBytes());
        System.out.println(Wallet.encode58Check(eck.getAddress()));


    }

    @Test
    public void generateAddress(){
        ECKey ecKey = new ECKey(Utils.getRandom());
        byte[] priKey = ecKey.getPrivKeyBytes();
        byte[] pubKey = ecKey.getPubKey();
        byte[] address = ecKey.getAddress();
        logger.info("Private Key: " + Hex.toHexString(priKey));
        logger.info("Public Key: " + Hex.toHexString(pubKey));
        logger.info("GSC Address: " + Wallet.encode58Check(address));
    }

    @Test
    public void createAccount(){
        String ownerPriKey = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        byte[] ownerAddress = Hex.decode("266145c6b6ebb0a7a87a8ce1ef9ae8f21a7d5b24e7");

        byte[] newAccountAddress = Hex.decode("26582f10257c25bd4066f3cbec769643cbf12456d0");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "127.0.0.1:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Contract.AccountCreateContract.Builder accountCreateContract = Contract.AccountCreateContract.newBuilder();
        accountCreateContract.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        accountCreateContract.setAccountAddress(ByteString.copyFrom(newAccountAddress));

        Protocol.Transaction transaction = blockingStub.createAccount(accountCreateContract.build());

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
    public void trasfer(){
        String ownerPriKey = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        byte[] ownerAddress = Hex.decode("266145c6b6ebb0a7a87a8ce1ef9ae8f21a7d5b24e7");

        byte[] toAddress = Hex.decode("26582f10257c25bd4066f3cbec769643cbf12456d0");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

        Wallet.setAddressPreFixByte(Byte.decode("0x26"));
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "127.0.0.1:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Contract.TransferContract.Builder transferContract = Contract.TransferContract.newBuilder();
        transferContract.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        transferContract.setToAddress(ByteString.copyFrom(toAddress));
        transferContract.setAmount(1000000000000000L);

        Protocol.Transaction transaction = blockingStub.createTransaction(transferContract.build());

        TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
        System.out.println(transactionWrapper.getTransactionId());

        System.out.println( transaction.toString());
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
    public void getVotes2(){
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "127.0.0.1:50051";
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
    public void getToken(){
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "47.254.71.98:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        GrpcAPI.AssetIssueList voteStatistics = blockingStub.getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
        voteStatistics.getAssetIssueList().forEach(assetIssueContract -> {
           // System.out.println(assetIssueContract.getDescription().toStringUtf8());
        });
        //System.out.println(voteStatistics.getAssetIssueList().toString());

        Protocol.Account account = blockingStub.getAccount(Protocol.Account.newBuilder().setAddress(ByteString.copyFrom("GRwZdfiZDBQZFuuYvjTW7KgcDkfyt9otDL".getBytes())).build());
        System.out.println(account.toString());
        voteStatistics.getAssetIssueList().forEach(assetIssueContract -> {
            //System.out.println(assetIssueContract.getDescription().toStringUtf8());
        });
        //System.out.println(voteStatistics.getAssetIssueList().toString());
    }

    @Test
    public void unfreezeAsset(){
        String ownerPriKey = "fd146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d1";
        byte[] ownerAddress = Hex.decode("268398f1c16a0cdb2dd3fd2feab1ae6fe149c86b59");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

        Wallet.setAddressPreFixByte(Byte.decode("0x26"));
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "127.0.0.1:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Protocol.Transaction transaction = blockingStub.unfreezeAsset(Contract.UnfreezeAssetContract.newBuilder().setOwnerAddress(ByteString.copyFrom(ownerAddress)).build());
        System.out.println( transaction.toString());
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
    public void getAccount(){
        //byte[] ownerAddress = Hex.decode("268398f1c16a0cdb2dd3fd2feab1ae6fe149c86b59");
        //String node = "127.0.0.1:50051";

        byte[] ownerAddress = Hex.decode("262daebb11f20b68a2035519a8553b597bb7dbbfa4");
        String node = "47.254.71.98:50051";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Protocol.Account account = blockingStub.getAccount(Protocol.Account.newBuilder().setAddress(ByteString.copyFrom(Hex.decode("263130141690d953233e1ed308b5a02f1fef7a565a"))).build());
        System.out.println("Account: \n" + account.toString());

    }

    @Test
    public void wiki(){
        byte[] ownerAddress = Hex.decode("266145c6b6ebb0a7a87a8ce1ef9ae8f21a7d5b24e7");
        byte[] accountName = Hex.decode("kay");
        String node = "47.254.71.98:50051";

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        builder.setAccountName(ByteString.copyFrom(accountName));

        Protocol.Transaction transaction = blockingStub.updateAccount(builder.build());
        System.out.println("Account: \n" + transaction.toString());
    }

    @Test
    public void getWitness(){
        byte[] ownerAddress = Hex.decode("262daebb11f20b68a2035519a8553b597bb7dbbfa4");
        String node = "47.254.71.98:50051";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        GrpcAPI.WitnessList witnessList = blockingStub.listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
        //System.out.println("WitnessList: \n" + witnessList.toString());

        Protocol.Block block = blockingStub.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        System.out.println("WitnessList: \n" + block.toString());

    }

    @Test
    public void getVotes(){
        String host = "39.105.18.104";
        int port = 50051;
        WalletGrpcClient walletGrpcClient = new WalletGrpcClient(host, port);

        Optional<GrpcAPI.AssetIssueList> statistics = walletGrpcClient.getAssetIssueByAccount(Hex.decode("264a168943a7e07924e402b76db9a3d0cd51359b55".getBytes()));
        System.out.println(JsonFormat.printToString(statistics.get()));

        Optional<GrpcAPI.NodeList> nodelist = walletGrpcClient.listNodes();
        System.out.println(JsonFormat.printToString(nodelist.get()));

        Optional<GrpcAPI.VoteStatistics> voteStatistics = walletGrpcClient.getWitnessVoteStatistics();
        System.out.println(voteStatistics.toString());
    }

    @Test
    public void addressTest() {
        String ownerAddress = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
        byte[] address = Hex.decode(ownerAddress);
        logger.info("---------------------------------------------");
        System.out.println();
        //System.out.println("Address: " + ByteString.copyFrom(address.toString());
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void toHexString(){
        String str = "http://Mercury.org";
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Hex String: " + Hex.toHexString(str.getBytes()));
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void distanceOfTwoNode(){
        String node1 = "fa1b803793aba64c3ca784e21a604f9a4f94a0d002ba3bb7dfc8d2243bbaff32c34f6ea5b434cb5ceab66d6bce08f93733463ab0e722e6d693814bf070733196";
        String node2 = "3d07f0563743eecba23793b35d987b4f9079f7e4b658465b842a59ee0b9b35f11205f94ddbd7d66d26e9baea3b01ab139b1469ef0e30206222caaea22aeed3f7";
        String node3 = "ca39c31146a20f445a2bff58a8d42e5f24b2860105a8205e279cdcc256656021b4fdf0a366adc10e0c187560c5022456e8543982e912468cb6ad055edec29497";


        int distance12 = NodeEntry.distance(Hex.decode(node1), Hex.decode(node2));
        int distance23 = NodeEntry.distance(Hex.decode(node2), Hex.decode(node3));
        int distance13 = NodeEntry.distance(Hex.decode(node1), Hex.decode(node3));
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Node distance distance12: " + distance12);
        System.out.println("Node distance distance23: " + distance23);
        System.out.println("Node distance distance13: " + distance13);
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void ByteToString(){
        String str = "475162";
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Hex String: " + hexStr2Str(str));
        System.out.println();
        logger.info("---------------------------------------------");
    }

    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length;i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    @Test
    public void sign(){

        Manager dbManager = null; // @Autowired

        String privStr = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        BigInteger privKey = new BigInteger(privStr, 16);

        ECKey ecKey = ECKey.fromPrivate(privKey);

        Contract.TransferContract.Builder transferContract = Contract.TransferContract.newBuilder();
        transferContract.setOwnerAddress(ByteString.copyFrom(ecKey.getAddress()));
        transferContract.setToAddress(ByteString.copyFrom(new ECKey(Utils.getRandom()).getAddress()));
        transferContract.setAmount(1000000000000000L);

        TransactionWrapper transactionWrapper =  new TransactionWrapper(transferContract.build(),
                Protocol.Transaction.Contract.ContractType.TransferContract);

        try {
            BlockWrapper headBlock = null;
            List<BlockWrapper> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
            if (CollectionUtils.isEmpty(blockList)) {
                logger.error("latest block not found");
            } else {
                headBlock = blockList.get(0);
            }
            transactionWrapper.setReference(headBlock.getNum(), headBlock.getBlockId().getBytes());
            long expiration = headBlock.getTimeStamp() + Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;
            transactionWrapper.setExpiration(expiration);
            transactionWrapper.setTimestamp();
        } catch (Exception e) {
            logger.error("Header not found.");
            e.printStackTrace();
        }

        Protocol.Transaction transaction = transactionWrapper.getInstance();

        Protocol.Transaction.Builder txSigned = transaction.toBuilder();
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }

        logger.info("\n Transcation: " + Util.printTransaction(txSigned.build()));
    }

    @Test
    public void deployContract(){
        String originAddress = "262daebb11f20b68a2035519a8553b597bb7dbbfa4";
        String node = "47.254.71.98:50051";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub walletBlockingStub = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        walletBlockingStub = WalletGrpc.newBlockingStub(channel);

        String contractName = "barContract";
        String abiStr = "[{\"constant\":false,\"inputs\":[],\"name\":\"getName\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],"
                + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],"
                + "\"name\":\"getId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
                + "\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"id\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],"
                + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getNumber\","
                + "\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
        String byteCode = "6080604052600a60005534801561001557600080fd5b506101f9806100256000396000f300608060405260043610610062576000357c01"
                + "00000000000000000000000000000000000000000000000000000000900463ffffffff16806317d7de7c146100675780635d1ca631146100f757806"
                + "3af640d0f14610122578063f2c9ecd81461014d575b600080fd5b34801561007357600080fd5b5061007c610178565b6040518080602001828103825"
                + "283818151815260200191508051906020019080838360005b838110156100bc5780820151818401526020810190506100a1565b505050509050908101"
                + "90601f1680156100e95780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561010357600080fd"
                + "5b5061010c6101b5565b6040518082815260200191505060405180910390f35b34801561012e57600080fd5b506101376101be565b60405180828152602"
                + "00191505060405180910390f35b34801561015957600080fd5b506101626101c4565b6040518082815260200191505060405180910390f35b6060604080"
                + "5190810160405280600381526020017f6261720000000000000000000000000000000000000000000000000000000000815250905090565b60008054905"
                + "090565b60005481565b600060649050905600a165627a7a72305820dfe79cf7f4a8a342b754cad8895b13f85de7daa11803925cf392263397653e7f0029";
        long callValue = 0;
        long fee = 100000000;
        long consumeUserResourcePercent = 0;

        Protocol.SmartContract.ABI abi = RPCUtils.jsonStr2ABI(abiStr);

        Protocol.SmartContract.Builder smartContract = Protocol.SmartContract.newBuilder();
        smartContract.setOriginAddress(ByteString.copyFrom(Hex.decode(originAddress)));
        // smartContract.setContractAddress();
        smartContract.setName(contractName);
        smartContract.setAbi(abi);
        smartContract.setCallValue(callValue);
        smartContract.setBytecode(ByteString.copyFrom(Hex.decode(byteCode)));
        smartContract.setConsumeUserResourcePercent(consumeUserResourcePercent);
        Contract.CreateSmartContract request = Contract.CreateSmartContract.newBuilder()
                .setNewContract(smartContract).setOwnerAddress(ByteString.copyFrom(Hex.decode(originAddress))).build();
        GrpcAPI.TransactionExtention response = walletBlockingStub.deployContract(request);
        if (response.getTransaction() != null){
            System.out.println(Util.printTransaction(response.getTransaction()));
            System.out.println(response.getResult().getResult());
        }
    }
}
