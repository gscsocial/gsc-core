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

package org.gsc.services;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.gsc.core.wrapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.DatabaseGrpc.DatabaseImplBase;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.AccountNetMessage;
import org.gsc.api.GrpcAPI.AccountPaginated;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.GrpcAPI.Address;
import org.gsc.api.GrpcAPI.AddressPrKeyPairMessage;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BlockExtention;
import org.gsc.api.GrpcAPI.BlockLimit;
import org.gsc.api.GrpcAPI.BlockList;
import org.gsc.api.GrpcAPI.BlockListExtention;
import org.gsc.api.GrpcAPI.BlockReference;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.api.GrpcAPI.DelegatedResourceList;
import org.gsc.api.GrpcAPI.DelegatedResourceMessage;
import org.gsc.api.GrpcAPI.EasyTransferAssetByPrivateMessage;
import org.gsc.api.GrpcAPI.EasyTransferAssetMessage;
import org.gsc.api.GrpcAPI.EasyTransferByPrivateMessage;
import org.gsc.api.GrpcAPI.EasyTransferMessage;
import org.gsc.api.GrpcAPI.EasyTransferResponse;
import org.gsc.api.GrpcAPI.EmptyMessage;
import org.gsc.api.GrpcAPI.ExchangeList;
import org.gsc.api.GrpcAPI.Node;
import org.gsc.api.GrpcAPI.NodeList;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.PaginatedMessage;
import org.gsc.api.GrpcAPI.ProposalList;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.api.GrpcAPI.TransactionApprovedList;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.api.GrpcAPI.TransactionList;
import org.gsc.api.GrpcAPI.TransactionListExtention;
import org.gsc.api.GrpcAPI.TransactionSignWeight;
import org.gsc.api.GrpcAPI.WitnessList;
import org.gsc.api.WalletExtensionGrpc;
import org.gsc.api.WalletGrpc.WalletImplBase;
import org.gsc.api.WalletConfirmedGrpc.WalletConfirmedImplBase;
import org.gsc.application.Service;
import org.gsc.crypto.ECKey;
import org.gsc.net.node.NodeHandler;
import org.gsc.net.node.NodeManager;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.StringUtil;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.core.WalletConfirmed;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.NonUniqueObjectException;
import org.gsc.core.exception.StoreException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AccountCreateContract;
import org.gsc.protos.Contract.AccountPermissionUpdateContract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.ClearABIContract;
import org.gsc.protos.Contract.ParticipateAssetIssueContract;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Contract.UnfreezeAssetContract;
import org.gsc.protos.Contract.UpdateCpuLimitContract;
import org.gsc.protos.Contract.UpdateSettingContract;
import org.gsc.protos.Contract.VoteWitnessContract;
import org.gsc.protos.Contract.WitnessCreateContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.DynamicProperties;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.protos.Protocol.NodeInfo;
import org.gsc.protos.Protocol.Proposal;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.protos.Protocol.TransactionSign;

@Component
@Slf4j(topic = "API")
public class RpcApiService implements Service {

    private int port = Args.getInstance().getRpcPort();
    private Server apiServer;

    @Autowired
    private Manager dbManager;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private WalletConfirmed walletConfirmed;
    @Autowired
    private Wallet wallet;

    @Autowired
    private NodeInfoService nodeInfoService;

    @Getter
    private DatabaseApi databaseApi = new DatabaseApi();
    private WalletApi walletApi = new WalletApi();
    @Getter
    private WalletConfirmedApi walletConfirmedApi = new WalletConfirmedApi();

    private static final long BLOCK_LIMIT_NUM = 100;
    private static final long TRANSACTION_LIMIT_NUM = 1000;

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
                    .addService(databaseApi);

            Args args = Args.getInstance();

            if (args.getRpcThreadNum() > 0) {
                serverBuilder = serverBuilder
                        .executor(Executors.newFixedThreadPool(args.getRpcThreadNum()));
            }

            if (args.isConfirmedNode()) {
                serverBuilder = serverBuilder.addService(walletConfirmedApi);
                if (args.isWalletExtensionApi()) {
                    serverBuilder = serverBuilder.addService(new WalletExtensionApi());
                }
            } else {
                serverBuilder = serverBuilder.addService(walletApi);
            }

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

        logger.info("RpcApiService started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            //server.this.stop();
            System.err.println("*** server shut down");
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
    public class DatabaseApi extends DatabaseImplBase {

        @Override
        public void getBlockReference(org.gsc.api.GrpcAPI.EmptyMessage request,
                                      io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockReference> responseObserver) {
            long headBlockNum = dbManager.getDynamicPropertiesStore()
                    .getLatestBlockHeaderNumber();
            byte[] blockHeaderHash = dbManager.getDynamicPropertiesStore()
                    .getLatestBlockHeaderHash().getBytes();
            BlockReference ref = BlockReference.newBuilder()
                    .setBlockHash(ByteString.copyFrom(blockHeaderHash))
                    .setBlockNum(headBlockNum)
                    .build();
            responseObserver.onNext(ref);
            responseObserver.onCompleted();
        }

        @Override
        public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
            Block block = null;
            try {
                block = dbManager.getHead().getInstance();
            } catch (StoreException e) {
                logger.error(e.getMessage());
            }
            responseObserver.onNext(block);
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
            Block block = null;
            try {
                block = dbManager.getBlockByNum(request.getNum()).getInstance();
            } catch (StoreException e) {
                logger.error(e.getMessage());
            }
            responseObserver.onNext(block);
            responseObserver.onCompleted();
        }

        @Override
        public void getDynamicProperties(EmptyMessage request,
                                         StreamObserver<DynamicProperties> responseObserver) {
            DynamicProperties.Builder builder = DynamicProperties.newBuilder();
            builder.setLastConfirmedBlockNum(
                    dbManager.getDynamicPropertiesStore().getLatestConfirmedBlockNum());
            DynamicProperties dynamicProperties = builder.build();
            responseObserver.onNext(dynamicProperties);
            responseObserver.onCompleted();
        }
    }

    /**
     * WalletConfirmedApi.
     */
    public class WalletConfirmedApi extends WalletConfirmedImplBase {

        @Override
        public void getAccount(Account request, StreamObserver<Account> responseObserver) {
            ByteString addressBs = request.getAddress();
            if (addressBs != null) {
                Account reply = wallet.getAccount(request);
                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAccountById(Account request, StreamObserver<Account> responseObserver) {
            ByteString id = request.getAccountId();
            if (id != null) {
                Account reply = wallet.getAccountById(request);
                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void listWitnesses(EmptyMessage request, StreamObserver<WitnessList> responseObserver) {
            responseObserver.onNext(wallet.getWitnessList());
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueList(EmptyMessage request,
                                      StreamObserver<AssetIssueList> responseObserver) {
            responseObserver.onNext(wallet.getAssetIssueList());
            responseObserver.onCompleted();
        }

        @Override
        public void getPaginatedAssetIssueList(PaginatedMessage request,
                                               StreamObserver<AssetIssueList> responseObserver) {
            responseObserver.onNext(wallet.getAssetIssueList(request.getOffset(), request.getLimit()));
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueByName(BytesMessage request,
                                        StreamObserver<AssetIssueContract> responseObserver) {
            ByteString assetName = request.getValue();
            if (assetName != null) {
                try {
                    responseObserver.onNext(wallet.getAssetIssueByName(assetName));
                } catch (NonUniqueObjectException e) {
                    responseObserver.onNext(null);
                    logger.error("Confirmed NonUniqueObjectException: {}", e.getMessage());
                }
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueListByName(BytesMessage request,
                                            StreamObserver<AssetIssueList> responseObserver) {
            ByteString assetName = request.getValue();

            if (assetName != null) {
                responseObserver.onNext(wallet.getAssetIssueListByName(assetName));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueById(BytesMessage request,
                                      StreamObserver<AssetIssueContract> responseObserver) {
            ByteString assetId = request.getValue();

            if (assetId != null) {
                responseObserver.onNext(wallet.getAssetIssueById(assetId.toStringUtf8()));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
            responseObserver.onNext(wallet.getNowBlock());
            responseObserver.onCompleted();
        }

        @Override
        public void getNowBlock2(EmptyMessage request,
                                 StreamObserver<BlockExtention> responseObserver) {
            responseObserver.onNext(block2Extention(wallet.getNowBlock()));
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
            long num = request.getNum();
            if (num >= 0) {
                Block reply = wallet.getBlockByNum(num);
                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByNum2(NumberMessage request,
                                   StreamObserver<BlockExtention> responseObserver) {
            long num = request.getNum();
            if (num >= 0) {
                Block reply = wallet.getBlockByNum(num);
                responseObserver.onNext(block2Extention(reply));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }


        @Override
        public void getDelegatedResource(DelegatedResourceMessage request,
                                         StreamObserver<DelegatedResourceList> responseObserver) {
            responseObserver
                    .onNext(wallet.getDelegatedResource(request.getFromAddress(), request.getToAddress()));
            responseObserver.onCompleted();
        }

        @Override
        public void getDelegatedResourceAccountIndex(BytesMessage request,
                                                     StreamObserver<org.gsc.protos.Protocol.DelegatedResourceAccountIndex> responseObserver) {
            responseObserver
                    .onNext(wallet.getDelegatedResourceAccountIndex(request.getValue()));
            responseObserver.onCompleted();
        }

        @Override
        public void listExchanges(EmptyMessage request,
                                  StreamObserver<ExchangeList> responseObserver) {
            responseObserver.onNext(wallet.getExchangeList());
            responseObserver.onCompleted();
        }

        @Override
        public void getExchangeById(BytesMessage request,
                                    StreamObserver<Exchange> responseObserver) {
            ByteString exchangeId = request.getValue();

            if (Objects.nonNull(exchangeId)) {
                responseObserver.onNext(wallet.getExchangeById(exchangeId));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionCountByBlockNum(NumberMessage request,
                                                  StreamObserver<NumberMessage> responseObserver) {
            NumberMessage.Builder builder = NumberMessage.newBuilder();
            try {
                Block block = dbManager.getBlockByNum(request.getNum()).getInstance();
                builder.setNum(block.getTransactionsCount());
            } catch (StoreException e) {
                logger.error(e.getMessage());
                builder.setNum(-1);
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionById(BytesMessage request,
                                       StreamObserver<Transaction> responseObserver) {
            ByteString id = request.getValue();
            if (null != id) {
                Transaction reply = wallet.getTransactionById(id);

                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionInfoById(BytesMessage request,
                                           StreamObserver<TransactionInfo> responseObserver) {
            ByteString id = request.getValue();
            if (null != id) {
                TransactionInfo reply = wallet.getTransactionInfoById(id);

                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void generateAddress(EmptyMessage request,
                                    StreamObserver<GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
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

    /**
     * WalletExtensionApi.
     */
    public class WalletExtensionApi extends WalletExtensionGrpc.WalletExtensionImplBase {

        private TransactionListExtention transactionList2Extention(TransactionList transactionList) {
            if (transactionList == null) {
                return null;
            }
            TransactionListExtention.Builder builder = TransactionListExtention.newBuilder();
            for (Transaction transaction : transactionList.getTransactionList()) {
                builder.addTransaction(transaction2Extention(transaction));
            }
            return builder.build();
        }

        @Override
        public void getTransactionsFromThis(AccountPaginated request,
                                            StreamObserver<TransactionList> responseObserver) {
            ByteString thisAddress = request.getAccount().getAddress();
            long offset = request.getOffset();
            long limit = request.getLimit();
            if (null != thisAddress && offset >= 0 && limit >= 0) {
                TransactionList reply = walletConfirmed
                        .getTransactionsFromThis(thisAddress, offset, limit);
                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionsFromThis2(AccountPaginated request,
                                             StreamObserver<TransactionListExtention> responseObserver) {
            ByteString thisAddress = request.getAccount().getAddress();
            long offset = request.getOffset();
            long limit = request.getLimit();
            if (null != thisAddress && offset >= 0 && limit >= 0) {
                TransactionList reply = walletConfirmed
                        .getTransactionsFromThis(thisAddress, offset, limit);
                responseObserver.onNext(transactionList2Extention(reply));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionsToThis(AccountPaginated request,
                                          StreamObserver<TransactionList> responseObserver) {
            ByteString toAddress = request.getAccount().getAddress();
            long offset = request.getOffset();
            long limit = request.getLimit();
            if (null != toAddress && offset >= 0 && limit >= 0) {
                TransactionList reply = walletConfirmed
                        .getTransactionsToThis(toAddress, offset, limit);
                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionsToThis2(AccountPaginated request,
                                           StreamObserver<TransactionListExtention> responseObserver) {
            ByteString toAddress = request.getAccount().getAddress();
            long offset = request.getOffset();
            long limit = request.getLimit();
            if (null != toAddress && offset >= 0 && limit >= 0) {
                TransactionList reply = walletConfirmed
                        .getTransactionsToThis(toAddress, offset, limit);
                responseObserver.onNext(transactionList2Extention(reply));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }
    }

    /**
     * WalletApi.
     */
    public class WalletApi extends WalletImplBase {

        private BlockListExtention blocklist2Extention(BlockList blockList) {
            if (blockList == null) {
                return null;
            }
            BlockListExtention.Builder builder = BlockListExtention.newBuilder();
            for (Block block : blockList.getBlockList()) {
                builder.addBlock(block2Extention(block));
            }
            return builder.build();
        }

        @Override
        public void getAccount(Account req, StreamObserver<Account> responseObserver) {
            ByteString addressBs = req.getAddress();
            if (addressBs != null) {
                Account reply = wallet.getAccount(req);
                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAccountById(Account req, StreamObserver<Account> responseObserver) {
            ByteString accountId = req.getAccountId();
            if (accountId != null) {
                Account reply = wallet.getAccountById(req);
                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void createTransaction(TransferContract request,
                                      StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver
                        .onNext(
                                createTransactionWrapper(request, ContractType.TransferContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void createTransaction2(TransferContract request,
                                       StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.TransferContract, responseObserver);
        }

        private void createTransactionExtention(Message request, ContractType contractType,
                                                StreamObserver<TransactionExtention> responseObserver) {
            TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
            Return.Builder retBuilder = Return.newBuilder();
            try {
                TransactionWrapper trx = createTransactionWrapper(request, contractType);
                trxExtBuilder.setTransaction(trx.getInstance());
                trxExtBuilder.setTxid(trx.getTransactionId().getByteString());
                retBuilder.setResult(true).setCode(response_code.SUCCESS);
            } catch (ContractValidateException e) {
                retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                        .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()));
                logger.debug("ContractValidateException: {}", e.getMessage());
            } catch (Exception e) {
                retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
                logger.info("exception caught" + e.getMessage());
            }
            trxExtBuilder.setResult(retBuilder);
            responseObserver.onNext(trxExtBuilder.build());
            responseObserver.onCompleted();
        }

        private TransactionWrapper createTransactionWrapper(com.google.protobuf.Message message,
                                                            ContractType contractType) throws ContractValidateException {
            return wallet.createTransactionWrapper(message, contractType);
        }

        @Override
        public void getTransactionSign(TransactionSign req,
                                       StreamObserver<Transaction> responseObserver) {
            TransactionWrapper retur = wallet.getTransactionSign(req);
            responseObserver.onNext(retur.getInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionSign2(TransactionSign req,
                                        StreamObserver<TransactionExtention> responseObserver) {
            TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
            Return.Builder retBuilder = Return.newBuilder();
            try {
                TransactionWrapper trx = wallet.getTransactionSign(req);
                trxExtBuilder.setTransaction(trx.getInstance());
                trxExtBuilder.setTxid(trx.getTransactionId().getByteString());
                retBuilder.setResult(true).setCode(response_code.SUCCESS);
            } catch (Exception e) {
                retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
                logger.info("exception caught" + e.getMessage());
            }
            trxExtBuilder.setResult(retBuilder);
            responseObserver.onNext(trxExtBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void addSign(TransactionSign req,
                            StreamObserver<TransactionExtention> responseObserver) {
            TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
            Return.Builder retBuilder = Return.newBuilder();
            try {
                TransactionWrapper trx = wallet.addSign(req);
                trxExtBuilder.setTransaction(trx.getInstance());
                trxExtBuilder.setTxid(trx.getTransactionId().getByteString());
                retBuilder.setResult(true).setCode(response_code.SUCCESS);
            } catch (Exception e) {
                retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
                logger.info("exception caught" + e.getMessage());
            }
            trxExtBuilder.setResult(retBuilder);
            responseObserver.onNext(trxExtBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionSignWeight(Transaction req,
                                             StreamObserver<TransactionSignWeight> responseObserver) {
            TransactionSignWeight tsw = wallet.getTransactionSignWeight(req);
            responseObserver.onNext(tsw);
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionApprovedList(Transaction req,
                                               StreamObserver<TransactionApprovedList> responseObserver) {
            TransactionApprovedList tal = wallet.getTransactionApprovedList(req);
            responseObserver.onNext(tal);
            responseObserver.onCompleted();
        }

        @Override
        public void createAddress(BytesMessage req,
                                  StreamObserver<BytesMessage> responseObserver) {
            byte[] address = wallet.createAdresss(req.getValue().toByteArray());
            BytesMessage.Builder builder = BytesMessage.newBuilder();
            builder.setValue(ByteString.copyFrom(address));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        private EasyTransferResponse easyTransfer(byte[] privateKey, ByteString toAddress,
                                                  long amount) {
            TransactionWrapper transactionWrapper;
            GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
            EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
            try {
                ECKey ecKey = ECKey.fromPrivate(privateKey);
                byte[] owner = ecKey.getAddress();
                TransferContract.Builder builder = TransferContract.newBuilder();
                builder.setOwnerAddress(ByteString.copyFrom(owner));
                builder.setToAddress(toAddress);
                builder.setAmount(amount);
                transactionWrapper = createTransactionWrapper(builder.build(),
                        ContractType.TransferContract);
                transactionWrapper.sign(privateKey);
                GrpcAPI.Return retur = wallet.broadcastTransaction(transactionWrapper.getInstance());
                responseBuild.setTransaction(transactionWrapper.getInstance());
                responseBuild.setTxid(transactionWrapper.getTransactionId().getByteString());
                responseBuild.setResult(retur);
            } catch (ContractValidateException e) {
                returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getMessage()));
                responseBuild.setResult(returnBuilder.build());
            } catch (Exception e) {
                returnBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
                responseBuild.setResult(returnBuilder.build());
            }

            return responseBuild.build();
        }

        private EasyTransferResponse easyTransferAsset(byte[] privateKey, ByteString toAddress,
                                                       String assetId, long amount) {
            TransactionWrapper transactionWrapper;
            GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
            EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
            try {
                ECKey ecKey = ECKey.fromPrivate(privateKey);
                byte[] owner = ecKey.getAddress();
                TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
                builder.setOwnerAddress(ByteString.copyFrom(owner));
                builder.setToAddress(toAddress);
                builder.setAssetName(ByteString.copyFrom(assetId.getBytes()));
                builder.setAmount(amount);
                transactionWrapper = createTransactionWrapper(builder.build(),
                        ContractType.TransferAssetContract);
                transactionWrapper.sign(privateKey);
                GrpcAPI.Return retur = wallet.broadcastTransaction(transactionWrapper.getInstance());
                responseBuild.setTransaction(transactionWrapper.getInstance());
                responseBuild.setTxid(transactionWrapper.getTransactionId().getByteString());
                responseBuild.setResult(retur);
            } catch (ContractValidateException e) {
                returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getMessage()));
                responseBuild.setResult(returnBuilder.build());
            } catch (Exception e) {
                returnBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
                responseBuild.setResult(returnBuilder.build());
            }

            return responseBuild.build();
        }

        @Override
        public void easyTransfer(EasyTransferMessage req,
                                 StreamObserver<EasyTransferResponse> responseObserver) {
            byte[] privateKey = wallet.pass2Key(req.getPassPhrase().toByteArray());
            EasyTransferResponse response = easyTransfer(privateKey, req.getToAddress(), req.getAmount());
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void easyTransferByPrivate(EasyTransferByPrivateMessage req,
                                          StreamObserver<EasyTransferResponse> responseObserver) {
            byte[] privateKey = req.getPrivateKey().toByteArray();
            EasyTransferResponse response = easyTransfer(privateKey, req.getToAddress(), req.getAmount());
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void easyTransferAsset(EasyTransferAssetMessage req,
                                      StreamObserver<EasyTransferResponse> responseObserver) {
            byte[] privateKey = wallet.pass2Key(req.getPassPhrase().toByteArray());
            EasyTransferResponse response = easyTransferAsset(privateKey, req.getToAddress(),
                    req.getAssetId(), req.getAmount());
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void easyTransferAssetByPrivate(EasyTransferAssetByPrivateMessage req,
                                               StreamObserver<EasyTransferResponse> responseObserver) {
            byte[] privateKey = req.getPrivateKey().toByteArray();
            EasyTransferResponse response = easyTransferAsset(privateKey, req.getToAddress(),
                    req.getAssetId(), req.getAmount());
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void broadcastTransaction(Transaction req,
                                         StreamObserver<GrpcAPI.Return> responseObserver) {
            GrpcAPI.Return retur = wallet.broadcastTransaction(req);
            responseObserver.onNext(retur);
            responseObserver.onCompleted();
        }

        @Override
        public void createAssetIssue(AssetIssueContract request,
                                     StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.AssetIssueContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver.onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void createAssetIssue2(AssetIssueContract request,
                                      StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.AssetIssueContract, responseObserver);
        }

        @Override
        public void unfreezeAsset(UnfreezeAssetContract request,
                                  StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.UnfreezeAssetContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver.onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void unfreezeAsset2(UnfreezeAssetContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.UnfreezeAssetContract, responseObserver);
        }

        //refactor、test later
        private void checkVoteWitnessAccount(VoteWitnessContract req) {
            //send back to cli
            ByteString ownerAddress = req.getOwnerAddress();
            Preconditions.checkNotNull(ownerAddress, "OwnerAddress is null");

            AccountWrapper account = dbManager.getAccountStore().get(ownerAddress.toByteArray());
            Preconditions.checkNotNull(account,
                    "OwnerAddress[" + StringUtil.createReadableString(ownerAddress) + "] not exists");

            int votesCount = req.getVotesCount();
            Preconditions.checkArgument(votesCount <= 0, "VotesCount[" + votesCount + "] <= 0");
            Preconditions.checkArgument(account.getGSCPower() < votesCount,
                    "gsc power[" + account.getGSCPower() + "] <  VotesCount[" + votesCount + "]");

            req.getVotesList().forEach(vote -> {
                ByteString voteAddress = vote.getVoteAddress();
                WitnessWrapper witness = dbManager.getWitnessStore()
                        .get(voteAddress.toByteArray());
                String readableWitnessAddress = StringUtil.createReadableString(voteAddress);

                Preconditions.checkNotNull(witness, "witness[" + readableWitnessAddress + "] not exists");
                Preconditions.checkArgument(vote.getVoteCount() <= 0,
                        "VoteAddress[" + readableWitnessAddress + "],VotesCount[" + vote
                                .getVoteCount() + "] <= 0");
            });
        }

        @Override
        public void voteWitnessAccount(VoteWitnessContract request,
                                       StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.VoteWitnessContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void voteWitnessAccount2(VoteWitnessContract request,
                                        StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.VoteWitnessContract, responseObserver);
        }

        @Override
        public void updateSetting(UpdateSettingContract request,
                                  StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.UpdateSettingContract,
                    responseObserver);
        }

        @Override
        public void updateCpuLimit(UpdateCpuLimitContract request,
                                      StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.UpdateCpuLimitContract,
                    responseObserver);
        }

        @Override
        public void clearContractABI(ClearABIContract request,
                                     StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ClearABIContract,
                    responseObserver);
        }

        @Override
        public void createWitness(WitnessCreateContract request,
                                  StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.WitnessCreateContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void createWitness2(WitnessCreateContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.WitnessCreateContract, responseObserver);
        }

        @Override
        public void createAccount(AccountCreateContract request,
                                  StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.AccountCreateContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void createAccount2(AccountCreateContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.AccountCreateContract, responseObserver);
        }

        @Override
        public void updateWitness(Contract.WitnessUpdateContract request,
                                  StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.WitnessUpdateContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void updateWitness2(Contract.WitnessUpdateContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.WitnessUpdateContract, responseObserver);
        }

        @Override
        public void setAccountId(Contract.SetAccountIdContract request,
                                 StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.SetAccountIdContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void updateAccount(Contract.AccountUpdateContract request,
                                  StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.AccountUpdateContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void updateAccount2(Contract.AccountUpdateContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.AccountUpdateContract, responseObserver);
        }

        @Override
        public void updateAsset(Contract.UpdateAssetContract request,
                                StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request,
                                ContractType.UpdateAssetContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void updateAsset2(Contract.UpdateAssetContract request,
                                 StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.UpdateAssetContract, responseObserver);
        }

        @Override
        public void freezeBalance(Contract.FreezeBalanceContract request,
                                  StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.FreezeBalanceContract).getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void freezeBalance2(Contract.FreezeBalanceContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.FreezeBalanceContract, responseObserver);
        }

        @Override
        public void unfreezeBalance(Contract.UnfreezeBalanceContract request,
                                    StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.UnfreezeBalanceContract)
                                .getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void unfreezeBalance2(Contract.UnfreezeBalanceContract request,
                                     StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.UnfreezeBalanceContract, responseObserver);
        }

        @Override
        public void withdrawBalance(Contract.WithdrawBalanceContract request,
                                    StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver.onNext(
                        createTransactionWrapper(request, ContractType.WithdrawBalanceContract)
                                .getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void withdrawBalance2(Contract.WithdrawBalanceContract request,
                                     StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.WithdrawBalanceContract, responseObserver);
        }

        @Override
        public void proposalCreate(Contract.ProposalCreateContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ProposalCreateContract, responseObserver);
        }


        @Override
        public void proposalApprove(Contract.ProposalApproveContract request,
                                    StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ProposalApproveContract, responseObserver);
        }

        @Override
        public void proposalDelete(Contract.ProposalDeleteContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ProposalDeleteContract, responseObserver);
        }

//    @Override
//    public void buyStorage(Contract.BuyStorageContract request,
//        StreamObserver<TransactionExtention> responseObserver) {
//      createTransactionExtention(request, ContractType.BuyStorageContract, responseObserver);
//    }
//
//    @Override
//    public void buyStorageBytes(Contract.BuyStorageBytesContract request,
//        StreamObserver<TransactionExtention> responseObserver) {
//      createTransactionExtention(request, ContractType.BuyStorageBytesContract, responseObserver);
//    }
//
//    @Override
//    public void sellStorage(Contract.SellStorageContract request,
//        StreamObserver<TransactionExtention> responseObserver) {
//      createTransactionExtention(request, ContractType.SellStorageContract, responseObserver);
//    }

        @Override
        public void exchangeCreate(Contract.ExchangeCreateContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ExchangeCreateContract, responseObserver);
        }


        @Override
        public void exchangeInject(Contract.ExchangeInjectContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ExchangeInjectContract, responseObserver);
        }

        @Override
        public void exchangeTransaction(Contract.ExchangeTransactionContract request,
                                        StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ExchangeTransactionContract,
                    responseObserver);
        }

        @Override
        public void exchangeWithdraw(Contract.ExchangeWithdrawContract request,
                                     StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ExchangeWithdrawContract, responseObserver);
        }

        @Override
        public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
            responseObserver.onNext(wallet.getNowBlock());
            responseObserver.onCompleted();
        }

        @Override
        public void getNowBlock2(EmptyMessage request,
                                 StreamObserver<BlockExtention> responseObserver) {
            Block block = wallet.getNowBlock();
            responseObserver.onNext(block2Extention(block));
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
            responseObserver.onNext(wallet.getBlockByNum(request.getNum()));
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByNum2(NumberMessage request,
                                   StreamObserver<BlockExtention> responseObserver) {
            Block block = wallet.getBlockByNum(request.getNum());
            responseObserver.onNext(block2Extention(block));
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionCountByBlockNum(NumberMessage request,
                                                  StreamObserver<NumberMessage> responseObserver) {
            NumberMessage.Builder builder = NumberMessage.newBuilder();
            try {
                Block block = dbManager.getBlockByNum(request.getNum()).getInstance();
                builder.setNum(block.getTransactionsCount());
            } catch (StoreException e) {
                logger.error(e.getMessage());
                builder.setNum(-1);
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void listNodes(EmptyMessage request, StreamObserver<NodeList> responseObserver) {
            List<NodeHandler> handlerList = nodeManager.dumpActiveNodes();

            Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
            for (NodeHandler handler : handlerList) {
                String key = handler.getNode().getHexId() + handler.getNode().getHost();
                nodeHandlerMap.put(key, handler);
            }

            NodeList.Builder nodeListBuilder = NodeList.newBuilder();

            nodeHandlerMap.entrySet().stream()
                    .forEach(v -> {
                        org.gsc.net.node.Node node = v.getValue().getNode();
                        nodeListBuilder.addNodes(Node.newBuilder().setAddress(
                                Address.newBuilder()
                                        .setHost(ByteString.copyFrom(ByteArray.fromString(node.getHost())))
                                        .setPort(node.getPort())));
                    });

            responseObserver.onNext(nodeListBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void transferAsset(TransferAssetContract request,
                                  StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver
                        .onNext(createTransactionWrapper(request, ContractType.TransferAssetContract)
                                .getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void transferAsset2(TransferAssetContract request,
                                   StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.TransferAssetContract, responseObserver);
        }

        @Override
        public void participateAssetIssue(ParticipateAssetIssueContract request,
                                          StreamObserver<Transaction> responseObserver) {
            try {
                responseObserver
                        .onNext(createTransactionWrapper(request, ContractType.ParticipateAssetIssueContract)
                                .getInstance());
            } catch (ContractValidateException e) {
                responseObserver
                        .onNext(null);
                logger.debug("ContractValidateException: {}", e.getMessage());
            }
            responseObserver.onCompleted();
        }

        @Override
        public void participateAssetIssue2(ParticipateAssetIssueContract request,
                                           StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.ParticipateAssetIssueContract,
                    responseObserver);
        }

        @Override
        public void getAssetIssueByAccount(Account request,
                                           StreamObserver<AssetIssueList> responseObserver) {
            ByteString fromBs = request.getAddress();

            if (fromBs != null) {
                responseObserver.onNext(wallet.getAssetIssueByAccount(fromBs));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAccountNet(Account request,
                                  StreamObserver<AccountNetMessage> responseObserver) {
            ByteString fromBs = request.getAddress();

            if (fromBs != null) {
                responseObserver.onNext(wallet.getAccountNet(fromBs));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getWitnessVoteStatistics(EmptyMessage request,
                                             StreamObserver<GrpcAPI.VoteStatistics> responseObserver) {
            responseObserver.onNext(wallet.getWitnessVoteStatistics());
            responseObserver.onCompleted();
        }

        @Override
        public void getAccountResource(Account request,
                                       StreamObserver<AccountResourceMessage> responseObserver) {
            ByteString fromBs = request.getAddress();

            if (fromBs != null) {
                responseObserver.onNext(wallet.getAccountResource(fromBs));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueByName(BytesMessage request,
                                        StreamObserver<AssetIssueContract> responseObserver) {
            ByteString assetName = request.getValue();
            if (assetName != null) {
                try {
                    responseObserver.onNext(wallet.getAssetIssueByName(assetName));
                } catch (NonUniqueObjectException e) {
                    responseObserver.onNext(null);
                    logger.debug("Start NonUniqueObjectException: {}", e.getMessage());
                }
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueListByName(BytesMessage request,
                                            StreamObserver<AssetIssueList> responseObserver) {
            ByteString assetName = request.getValue();

            if (assetName != null) {
                responseObserver.onNext(wallet.getAssetIssueListByName(assetName));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueById(BytesMessage request,
                                      StreamObserver<AssetIssueContract> responseObserver) {
            ByteString assetId = request.getValue();

            if (assetId != null) {
                responseObserver.onNext(wallet.getAssetIssueById(assetId.toStringUtf8()));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockById(BytesMessage request, StreamObserver<Block> responseObserver) {
            ByteString blockId = request.getValue();

            if (Objects.nonNull(blockId)) {
                responseObserver.onNext(wallet.getBlockById(blockId));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getProposalById(BytesMessage request,
                                    StreamObserver<Proposal> responseObserver) {
            ByteString proposalId = request.getValue();

            if (Objects.nonNull(proposalId)) {
                responseObserver.onNext(wallet.getProposalById(proposalId));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getExchangeById(BytesMessage request,
                                    StreamObserver<Exchange> responseObserver) {
            ByteString exchangeId = request.getValue();

            if (Objects.nonNull(exchangeId)) {
                responseObserver.onNext(wallet.getExchangeById(exchangeId));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByLimitNext(BlockLimit request,
                                        StreamObserver<BlockList> responseObserver) {
            long startNum = request.getStartNum();
            long endNum = request.getEndNum();

            if (endNum > 0 && endNum > startNum && endNum - startNum <= BLOCK_LIMIT_NUM) {
                responseObserver.onNext(wallet.getBlocksByLimitNext(startNum, endNum - startNum));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByLimitNext2(BlockLimit request,
                                         StreamObserver<BlockListExtention> responseObserver) {
            long startNum = request.getStartNum();
            long endNum = request.getEndNum();

            if (endNum > 0 && endNum > startNum && endNum - startNum <= BLOCK_LIMIT_NUM) {
                responseObserver
                        .onNext(blocklist2Extention(wallet.getBlocksByLimitNext(startNum, endNum - startNum)));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByLatestNum(NumberMessage request,
                                        StreamObserver<BlockList> responseObserver) {
            long getNum = request.getNum();

            if (getNum > 0 && getNum < BLOCK_LIMIT_NUM) {
                responseObserver.onNext(wallet.getBlockByLatestNum(getNum));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getBlockByLatestNum2(NumberMessage request,
                                         StreamObserver<BlockListExtention> responseObserver) {
            long getNum = request.getNum();

            if (getNum > 0 && getNum < BLOCK_LIMIT_NUM) {
                responseObserver.onNext(blocklist2Extention(wallet.getBlockByLatestNum(getNum)));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getTransactionById(BytesMessage request,
                                       StreamObserver<Transaction> responseObserver) {
            ByteString transactionId = request.getValue();

            if (Objects.nonNull(transactionId)) {
                responseObserver.onNext(wallet.getTransactionById(transactionId));
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void deployContract(org.gsc.protos.Contract.CreateSmartContract request,
                                   io.grpc.stub.StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.CreateSmartContract, responseObserver);
        }

        public void totalTransaction(EmptyMessage request,
                                     StreamObserver<NumberMessage> responseObserver) {
            responseObserver.onNext(wallet.totalTransaction());
            responseObserver.onCompleted();
        }

        @Override
        public void getNextMaintenanceTime(EmptyMessage request,
                                           StreamObserver<NumberMessage> responseObserver) {
            responseObserver.onNext(wallet.getNextMaintenanceTime());
            responseObserver.onCompleted();
        }

        @Override
        public void getAssetIssueList(EmptyMessage request,
                                      StreamObserver<AssetIssueList> responseObserver) {
            responseObserver.onNext(wallet.getAssetIssueList());
            responseObserver.onCompleted();
        }

        @Override
        public void triggerContract(Contract.TriggerSmartContract request,
                                    StreamObserver<TransactionExtention> responseObserver) {

            callContract(request, responseObserver, false);
        }

        @Override
        public void triggerConstantContract(Contract.TriggerSmartContract request,
                                            StreamObserver<TransactionExtention> responseObserver) {

            callContract(request, responseObserver, true);
        }

        private void callContract(Contract.TriggerSmartContract request,
                                  StreamObserver<TransactionExtention> responseObserver, boolean isConstant) {
            TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
            Return.Builder retBuilder = Return.newBuilder();
            try {
                TransactionWrapper trxCap = createTransactionWrapper(request,
                        ContractType.TriggerSmartContract);
                Transaction trx;
                if (isConstant) {
                    trx = wallet.triggerConstantContract(request, trxCap, trxExtBuilder, retBuilder);
                } else {
                    trx = wallet.triggerContract(request, trxCap, trxExtBuilder, retBuilder);
                }
                trxExtBuilder.setTransaction(trx);
                trxExtBuilder.setTxid(trxCap.getTransactionId().getByteString());
                retBuilder.setResult(true).setCode(response_code.SUCCESS);
                trxExtBuilder.setResult(retBuilder);
            } catch (ContractValidateException | VMIllegalException e) {
                retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                        .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()));
                trxExtBuilder.setResult(retBuilder);
                logger.warn("ContractValidateException: {}", e.getMessage());
            } catch (RuntimeException e) {
                retBuilder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
                trxExtBuilder.setResult(retBuilder);
                logger.warn("When run constant call in VM, have RuntimeException: " + e.getMessage());
            } catch (Exception e) {
                retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                        .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
                trxExtBuilder.setResult(retBuilder);
                logger.warn("unknown exception caught: " + e.getMessage(), e);
            } finally {
                responseObserver.onNext(trxExtBuilder.build());
                responseObserver.onCompleted();
            }
        }

        public void getPaginatedAssetIssueList(PaginatedMessage request,
                                               StreamObserver<AssetIssueList> responseObserver) {
            responseObserver.onNext(wallet.getAssetIssueList(request.getOffset(), request.getLimit()));
            responseObserver.onCompleted();
        }

        @Override
        public void getContract(BytesMessage request,
                                StreamObserver<Protocol.SmartContract> responseObserver) {
            Protocol.SmartContract contract = wallet.getContract(request);
            responseObserver.onNext(contract);
            responseObserver.onCompleted();
        }

        public void listWitnesses(EmptyMessage request,
                                  StreamObserver<WitnessList> responseObserver) {
            responseObserver.onNext(wallet.getWitnessList());
            responseObserver.onCompleted();
        }

        @Override
        public void listProposals(EmptyMessage request,
                                  StreamObserver<ProposalList> responseObserver) {
            responseObserver.onNext(wallet.getProposalList());
            responseObserver.onCompleted();
        }


        @Override
        public void getDelegatedResource(DelegatedResourceMessage request,
                                         StreamObserver<DelegatedResourceList> responseObserver) {
            responseObserver
                    .onNext(wallet.getDelegatedResource(request.getFromAddress(), request.getToAddress()));
            responseObserver.onCompleted();
        }

        public void getDelegatedResourceAccountIndex(BytesMessage request,
                                                     StreamObserver<org.gsc.protos.Protocol.DelegatedResourceAccountIndex> responseObserver) {
            responseObserver
                    .onNext(wallet.getDelegatedResourceAccountIndex(request.getValue()));
            responseObserver.onCompleted();
        }

        @Override
        public void getPaginatedProposalList(PaginatedMessage request,
                                             StreamObserver<ProposalList> responseObserver) {
            responseObserver
                    .onNext(wallet.getPaginatedProposalList(request.getOffset(), request.getLimit()));
            responseObserver.onCompleted();

        }

        @Override
        public void getPaginatedExchangeList(PaginatedMessage request,
                                             StreamObserver<ExchangeList> responseObserver) {
            responseObserver
                    .onNext(wallet.getPaginatedExchangeList(request.getOffset(), request.getLimit()));
            responseObserver.onCompleted();

        }

        @Override
        public void listExchanges(EmptyMessage request,
                                  StreamObserver<ExchangeList> responseObserver) {
            responseObserver.onNext(wallet.getExchangeList());
            responseObserver.onCompleted();
        }

        @Override
        public void getChainParameters(EmptyMessage request,
                                       StreamObserver<Protocol.ChainParameters> responseObserver) {
            responseObserver.onNext(wallet.getChainParameters());
            responseObserver.onCompleted();
        }

        @Override
        public void generateAddress(EmptyMessage request,
                                    StreamObserver<GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
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

        @Override
        public void getTransactionInfoById(BytesMessage request,
                                           StreamObserver<TransactionInfo> responseObserver) {
            ByteString id = request.getValue();
            if (null != id) {
                TransactionInfo reply = wallet.getTransactionInfoById(id);

                responseObserver.onNext(reply);
            } else {
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void getNodeInfo(EmptyMessage request, StreamObserver<NodeInfo> responseObserver) {
            try {
                responseObserver.onNext(nodeInfoService.getNodeInfo().transferToProtoEntity());
            } catch (Exception e) {
                responseObserver.onError(e);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void accountPermissionUpdate(AccountPermissionUpdateContract request,
                                            StreamObserver<TransactionExtention> responseObserver) {
            createTransactionExtention(request, ContractType.AccountPermissionUpdateContract,
                    responseObserver);
        }

    }

    @Override
    public void stop() {
        if (apiServer != null) {
            apiServer.shutdown();
        }
    }

    /**
     * ...
     */
    public void blockUntilShutdown() {
        if (apiServer != null) {
            try {
                apiServer.awaitTermination();
            } catch (InterruptedException e) {
                logger.warn("{}", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
