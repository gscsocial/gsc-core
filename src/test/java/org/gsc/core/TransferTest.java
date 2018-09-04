package org.gsc.core;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletSolidityGrpc;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.StringUtil;
import org.gsc.crypto.ECKey;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class TransferTest {

    private final ManagedChannel channel;
    private final WalletSolidityGrpc.WalletSolidityBlockingStub walletSolidityBlockingStub;
    private final WalletGrpc.WalletBlockingStub walletBlockingStub;

    public TransferTest(String host,int port){
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        walletBlockingStub = WalletGrpc.newBlockingStub(channel);
        walletSolidityBlockingStub = WalletSolidityGrpc.newBlockingStub(channel);
    }


    public static  void  main(String [] args){
        TransferTest grpcClient = new TransferTest("47.74.225.234",50051);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File("/Users/mico/transfer/transfer-gsc.log")));//读取交易文件
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                try{
                    String []  addressAmoutArray = tempString.trim().split(" ");
                    String toAddress = addressAmoutArray[0];
                    String amount = addressAmoutArray[1].trim();
                    System.out.println("to addr:"+toAddress+" amount:"+amount);
                    Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
                    builder.setAmount(123);
                    builder.setToAddress(StringUtil.hexString2ByteString(toAddress));
                    builder.setOwnerAddress(StringUtil.hexString2ByteString(""));
                    Contract.TransferContract contract = builder.build();
                    grpcClient.walletBlockingStub.createTransaction(contract);
                    Protocol.Transaction transaction = grpcClient.walletBlockingStub.createTransaction(contract);
                    ECKey ecKey = ECKey.fromPrivate("493bd932efb11f3c87cf0a9cf08170378d44e60e139a7db4d044405ce76e54dc".getBytes());
                    Protocol.Transaction.Builder builder1 = transaction.toBuilder();
                    byte [] hash = Sha256Hash.hash(transaction.getRawData().toByteArray());
                    List<Protocol.Transaction.Contract> contracts = transaction.getRawData().getContractList();
                    for(int i = 0 ;i<contracts.size();i++){
                        ECKey.ECDSASignature signature = ecKey.sign(hash);
                        ByteString bytes = ByteString.copyFrom(signature.toByteArray());
                        builder1.addSignature(bytes);
                    }
                    transaction = builder1.build();
                    grpcClient.walletBlockingStub.broadcastTransaction(transaction);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }
}
