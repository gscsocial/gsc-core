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

package org.gsc.services.interfaceOnConfirmed;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.gsc.core.wrapper.BlockWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.gsc.api.DatabaseGrpc.DatabaseImplBase;
import org.gsc.api.GrpcAPI.AddressPrKeyPairMessage;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BlockExtention;
import org.gsc.api.GrpcAPI.BlockReference;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.api.GrpcAPI.DelegatedResourceList;
import org.gsc.api.GrpcAPI.DelegatedResourceMessage;
import org.gsc.api.GrpcAPI.EmptyMessage;
import org.gsc.api.GrpcAPI.ExchangeList;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.PaginatedMessage;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.api.GrpcAPI.WitnessList;
import org.gsc.api.WalletConfirmedGrpc.WalletConfirmedImplBase;
import org.gsc.application.Service;
import org.gsc.crypto.ECKey;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.config.args.Args;
import org.gsc.services.RpcApiService;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.DynamicProperties;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j(topic = "API")
public class ConfirmedRpcApiService implements Service {

    private int port = Args.getInstance().getRpcOnConfirmedPort();
    private Server apiServer;

    @Autowired
    private WalletOnConfirmed walletOnConfirmed;

    @Autowired
    private RpcApiService rpcApiService;

    @Override
    public void init() {
    }

    @Override
    public void init(Args args) {
    }

    @Override
    public void start() {
        try {
            NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(port)
                    .addService(new DatabaseApi());

            Args args = Args.getInstance();

            if (args.getRpcThreadNum() > 0) {
                serverBuilder = serverBuilder
                        .executor(Executors.newFixedThreadPool(args.getRpcThreadNum()));
            }

            serverBuilder = serverBuilder.addService(new WalletConfirmedApi());

            // Set configs from config.conf or default value
            serverBuilder
                    .maxConcurrentCallsPerConnection(args.getMaxConcurrentCallsPerConnection())
                    .flowControlWindow(args.getFlowControlWindow())
                    .maxConnectionIdle(args.getMaxConnectionIdleInMillis(), TimeUnit.MILLISECONDS)
                    .maxConnectionAge(args.getMaxConnectionAgeInMillis(), TimeUnit.MILLISECONDS)
                    .maxMessageSize(args.getMaxMessageSize())
                    .maxHeaderListSize(args.getMaxHeaderListSize());

            apiServer = serverBuilder.build().start();
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }

        logger.info("ConfirmedRpcApiService started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server on confirmed since JVM is shutting down");
            //server.this.stop();
            System.err.println("*** server on confirmed shut down");
        }));
    }

    private TransactionExtention transaction2Extention(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
        Return.Builder retBuilder = Return.newBuilder();
        trxExtBuilder.setTransaction(transaction);
        trxExtBuilder.setTxid(Sha256Hash.of(transaction.getRawData().toByteArray()).getByteString());
        retBuilder.setResult(true).setCode(response_code.SUCCESS);
        trxExtBuilder.setResult(retBuilder);
        return trxExtBuilder.build();
    }

    private BlockExtention block2Extention(Block block) {
        if (block == null) {
            return null;
        }
        BlockExtention.Builder builder = BlockExtention.newBuilder();
        BlockWrapper blockWrapper = new BlockWrapper(block);
        builder.setBlockHeader(block.getBlockHeader());
        builder.setBlockid(ByteString.copyFrom(blockWrapper.getBlockId().getBytes()));
        for (int i = 0; i < block.getTransactionsCount(); i++) {
            Transaction transaction = block.getTransactions(i);
            builder.addTransactions(transaction2Extention(transaction));
        }
        return builder.build();
    }

    /**
     * DatabaseApi.
     */
    private class DatabaseApi extends DatabaseImplBase {

        @Override
        public void getBlockReference(EmptyMessage request,
                                      StreamObserver<BlockReference> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getDatabaseApi().getBlockReference(request, responseObserver)
            );
        }

        @Override
        public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getDatabaseApi().getNowBlock(request, responseObserver));
        }

        @Override
        public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getDatabaseApi().getBlockByNum(request, responseObserver)
            );
        }

        @Override
        public void getDynamicProperties(EmptyMessage request,
                                         StreamObserver<DynamicProperties> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getDatabaseApi().getDynamicProperties(request, responseObserver)
            );
        }
    }

    /**
     * WalletConfirmedApi.
     */
    private class WalletConfirmedApi extends WalletConfirmedImplBase {

        @Override
        public void getAccount(Account request, StreamObserver<Account> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getAccount(request, responseObserver)
            );
        }

        @Override
        public void getAccountById(Account request, StreamObserver<Account> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getAccountById(request, responseObserver)
            );
        }

        @Override
        public void listWitnesses(EmptyMessage request, StreamObserver<WitnessList> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().listWitnesses(request, responseObserver)
            );
        }

        @Override
        public void getAssetIssueById(BytesMessage request,
                                      StreamObserver<AssetIssueContract> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getAssetIssueById(request, responseObserver)
            );
        }

        @Override
        public void getAssetIssueByName(BytesMessage request,
                                        StreamObserver<AssetIssueContract> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getAssetIssueByName(request, responseObserver)
            );
        }

        @Override
        public void getAssetIssueList(EmptyMessage request,
                                      StreamObserver<AssetIssueList> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getAssetIssueList(request, responseObserver)
            );
        }

        @Override
        public void getAssetIssueListByName(BytesMessage request,
                                            StreamObserver<AssetIssueList> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi()
                            .getAssetIssueListByName(request, responseObserver)
            );
        }

        @Override
        public void getPaginatedAssetIssueList(PaginatedMessage request,
                                               StreamObserver<AssetIssueList> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi()
                            .getPaginatedAssetIssueList(request, responseObserver)
            );
        }

        @Override
        public void getExchangeById(BytesMessage request,
                                    StreamObserver<Exchange> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getExchangeById(
                            request, responseObserver
                    )
            );
        }

        @Override
        public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getNowBlock(request, responseObserver)
            );
        }

        @Override
        public void getNowBlock2(EmptyMessage request,
                                 StreamObserver<BlockExtention> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getNowBlock2(request, responseObserver)
            );

        }

        @Override
        public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getBlockByNum(request, responseObserver)
            );
        }

        @Override
        public void getBlockByNum2(NumberMessage request,
                                   StreamObserver<BlockExtention> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getBlockByNum2(request, responseObserver)
            );
        }

        @Override
        public void getDelegatedResource(DelegatedResourceMessage request,
                                         StreamObserver<DelegatedResourceList> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getDelegatedResource(request, responseObserver)
            );
        }

        @Override
        public void getDelegatedResourceAccountIndex(BytesMessage request,
                                                     StreamObserver<org.gsc.protos.Protocol.DelegatedResourceAccountIndex> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi()
                            .getDelegatedResourceAccountIndex(request, responseObserver)
            );
        }

        @Override
        public void getTransactionCountByBlockNum(NumberMessage request,
                                                  StreamObserver<NumberMessage> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi()
                            .getTransactionCountByBlockNum(request, responseObserver)
            );
        }

        @Override
        public void getTransactionById(BytesMessage request,
                                       StreamObserver<Transaction> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().getTransactionById(request, responseObserver)
            );

        }

        @Override
        public void getTransactionInfoById(BytesMessage request,
                                           StreamObserver<TransactionInfo> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi()
                            .getTransactionInfoById(request, responseObserver)
            );

        }

        @Override
        public void listExchanges(EmptyMessage request,
                                  StreamObserver<ExchangeList> responseObserver) {
            walletOnConfirmed.futureGet(
                    () -> rpcApiService.getWalletConfirmedApi().listExchanges(request, responseObserver)
            );
        }

        @Override
        public void generateAddress(EmptyMessage request,
                                    StreamObserver<AddressPrKeyPairMessage> responseObserver) {
            ECKey ecKey = new ECKey(Utils.getRandom());
            byte[] priKey = ecKey.getPrivKeyBytes();
            byte[] address = ecKey.getAddress();
            String addressStr = Wallet.encode58Check(address);
            String priKeyStr = Hex.encodeHexString(priKey);
            AddressPrKeyPairMessage.Builder builder = AddressPrKeyPairMessage.newBuilder();
            builder.setAddress(addressStr);
            builder.setPrivateKey(priKeyStr);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void stop() {
        if (apiServer != null) {
            apiServer.shutdown();
        }
    }
}
