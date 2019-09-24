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

package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.runtime.vm.LogInfo;
import org.gsc.runtime.vm.program.InternalTransaction;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.config.args.Args;
import org.gsc.db.TransactionTrace;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.protos.Protocol.TransactionInfo.Log;
import org.gsc.protos.Protocol.TransactionInfo.code;

@Slf4j(topic = "wrapper")
public class TransactionInfoWrapper implements ProtoWrapper<TransactionInfo> {

    private TransactionInfo transactionInfo;

    /**
     * constructor TransactionWrapper.
     */
    public TransactionInfoWrapper(TransactionInfo trxRet) {
        this.transactionInfo = trxRet;
    }

    public TransactionInfoWrapper(byte[] data) throws BadItemException {
        try {
            this.transactionInfo = TransactionInfo.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new BadItemException("TransactionInfoWrapper proto data parse exception");
        }
    }

    public TransactionInfoWrapper() {
        this.transactionInfo = TransactionInfo.newBuilder().build();
    }

    public long getFee() {
        return transactionInfo.getFee();
    }

    public void setId(byte[] id) {
        this.transactionInfo = this.transactionInfo.toBuilder()
                .setId(ByteString.copyFrom(id)).build();
    }

    public byte[] getId() {
        return transactionInfo.getId().toByteArray();
    }


    public void setUnfreezeAmount(long amount) {
        this.transactionInfo = this.transactionInfo.toBuilder().setUnfreezeAmount(amount).build();
    }

    public long getUnfreezeAmount() {
        return transactionInfo.getUnfreezeAmount();
    }

    public void setWithdrawAmount(long amount) {
        this.transactionInfo = this.transactionInfo.toBuilder().setWithdrawAmount(amount).build();
    }

    public long getWithdrawAmount() {
        return transactionInfo.getWithdrawAmount();
    }

    public void setFee(long fee) {
        this.transactionInfo = this.transactionInfo.toBuilder().setFee(fee).build();
    }

    public void setResult(code result) {
        this.transactionInfo = this.transactionInfo.toBuilder().setResult(result).build();
    }

    public void setResMessage(String message) {
        this.transactionInfo = this.transactionInfo.toBuilder()
                .setResMessage(ByteString.copyFromUtf8(message)).build();
    }

    public void addFee(long fee) {
        this.transactionInfo = this.transactionInfo.toBuilder()
                .setFee(this.transactionInfo.getFee() + fee).build();
    }

    public long getBlockNumber() {
        return transactionInfo.getBlockNumber();
    }

    public void setBlockNumber(long num) {
        this.transactionInfo = this.transactionInfo.toBuilder().setBlockNumber(num)
                .build();
    }

    public long getBlockTimeStamp() {
        return transactionInfo.getBlockTimeStamp();
    }

    public void setBlockTimeStamp(long time) {
        this.transactionInfo = this.transactionInfo.toBuilder().setBlockTimeStamp(time)
                .build();
    }

    public void setContractResult(byte[] ret) {
        this.transactionInfo = this.transactionInfo.toBuilder()
                .addContractResult(ByteString.copyFrom(ret))
                .build();
    }

    public void setContractAddress(byte[] contractAddress) {
        this.transactionInfo = this.transactionInfo.toBuilder()
                .setContractAddress(ByteString.copyFrom(contractAddress))
                .build();
    }

    public void setReceipt(ReceiptWrapper receipt) {
        this.transactionInfo = this.transactionInfo.toBuilder()
                .setReceipt(receipt.getReceipt())
                .build();
    }


    public void addAllLog(List<Log> logs) {
        this.transactionInfo = this.transactionInfo.toBuilder()
                .addAllLog(logs)
                .build();
    }

    @Override
    public byte[] getData() {
        return this.transactionInfo.toByteArray();
    }

    @Override
    public TransactionInfo getInstance() {
        return this.transactionInfo;
    }

    public static TransactionInfoWrapper buildInstance(TransactionWrapper trxCap, BlockWrapper block,
                                                       TransactionTrace trace) {

        TransactionInfo.Builder builder = TransactionInfo.newBuilder();
        ReceiptWrapper traceReceipt = trace.getReceipt();
        builder.setResult(code.SUCESS);
        if (StringUtils.isNoneEmpty(trace.getRuntimeError()) || Objects
                .nonNull(trace.getRuntimeResult().getException())) {
            builder.setResult(code.FAILED);
            builder.setResMessage(ByteString.copyFromUtf8(trace.getRuntimeError()));
        }
        builder.setId(ByteString.copyFrom(trxCap.getTransactionId().getBytes()));
        ProgramResult programResult = trace.getRuntimeResult();
        long fee =
                programResult.getRet().getFee() + traceReceipt.getCpuFee()
                        + traceReceipt.getNetFee() + traceReceipt.getMultiSignFee();
        ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
        ByteString ContractAddress = ByteString.copyFrom(programResult.getContractAddress());

        builder.setFee(fee);
        builder.addContractResult(contractResult);
        builder.setContractAddress(ContractAddress);
        builder.setUnfreezeAmount(programResult.getRet().getUnfreezeAmount());
        builder.setAssetIssueID(programResult.getRet().getAssetIssueID());
        builder.setExchangeId(programResult.getRet().getExchangeId());
        builder.setWithdrawAmount(programResult.getRet().getWithdrawAmount());
        builder.setExchangeReceivedAmount(programResult.getRet().getExchangeReceivedAmount());
        builder.setExchangeInjectAnotherAmount(programResult.getRet().getExchangeInjectAnotherAmount());
        builder.setExchangeWithdrawAnotherAmount(
                programResult.getRet().getExchangeWithdrawAnotherAmount());

        List<Log> logList = new ArrayList<>();
        programResult.getLogInfoList().forEach(
                logInfo -> {
                    logList.add(LogInfo.buildLog(logInfo));
                }
        );
        builder.addAllLog(logList);

        if (Objects.nonNull(block)) {
            builder.setBlockNumber(block.getInstance().getBlockHeader().getRawData().getNumber());
            builder.setBlockTimeStamp(block.getInstance().getBlockHeader().getRawData().getTimestamp());
        }

        builder.setReceipt(traceReceipt.getReceipt());

        if (Args.getInstance().isSaveInternalTx() && null != programResult.getInternalTransactions()) {
            for (InternalTransaction internalTransaction : programResult
                    .getInternalTransactions()) {
                Protocol.InternalTransaction.Builder internalTrxBuilder = Protocol.InternalTransaction
                        .newBuilder();
                // set hash
                internalTrxBuilder.setHash(ByteString.copyFrom(internalTransaction.getHash()));
                // set caller
                internalTrxBuilder.setCallerAddress(ByteString.copyFrom(internalTransaction.getSender()));
                // set TransferTo
                internalTrxBuilder
                        .setTransferToAddress(ByteString.copyFrom(internalTransaction.getTransferToAddress()));
                //TODO: "for loop" below in future for multiple token case, we only have one for now.
                Protocol.InternalTransaction.CallValueInfo.Builder callValueInfoBuilder =
                        Protocol.InternalTransaction.CallValueInfo.newBuilder();
                // gsc will not be set token name
                callValueInfoBuilder.setCallValue(internalTransaction.getValue());
                // Just one transferBuilder for now.
                internalTrxBuilder.addCallValueInfo(callValueInfoBuilder);
                internalTransaction.getTokenInfo().forEach((tokenId, amount) -> {
                    Protocol.InternalTransaction.CallValueInfo.Builder tokenInfoBuilder =
                            Protocol.InternalTransaction.CallValueInfo.newBuilder();
                    tokenInfoBuilder.setTokenId(tokenId);
                    tokenInfoBuilder.setCallValue(amount);
                    internalTrxBuilder.addCallValueInfo(tokenInfoBuilder);
                });
                // Token for loop end here
                internalTrxBuilder.setNote(ByteString.copyFrom(internalTransaction.getNote().getBytes()));
                internalTrxBuilder.setRejected(internalTransaction.isRejected());
                builder.addInternalTransactions(internalTrxBuilder);
            }
        }

        return new TransactionInfoWrapper(builder.build());
    }
}