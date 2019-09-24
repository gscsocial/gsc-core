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

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Result;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;

@Slf4j(topic = "wrapper")
public class TransactionResultWrapper implements ProtoWrapper<Result> {

    private Transaction.Result transactionResult;

    /**
     * constructor TransactionWrapper.
     */
    public TransactionResultWrapper(Transaction.Result trxRet) {
        this.transactionResult = trxRet;
    }

    public TransactionResultWrapper(byte[] data) throws BadItemException {
        try {
            this.transactionResult = Transaction.Result.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new BadItemException("TransactionResult proto data parse exception");
        }
    }

    public TransactionResultWrapper() {
        this.transactionResult = Transaction.Result.newBuilder().build();
    }

    public TransactionResultWrapper(contractResult code) {
        this.transactionResult = Transaction.Result.newBuilder().setContractRet(code).build();
    }

    public TransactionResultWrapper(Transaction.Result.code code, long fee) {
        this.transactionResult = Transaction.Result.newBuilder().setRet(code).setFee(fee).build();
    }

    public void setStatus(long fee, Transaction.Result.code code) {
        long oldValue = transactionResult.getFee();
        this.transactionResult = this.transactionResult.toBuilder()
                .setFee(oldValue + fee)
                .setRet(code).build();
    }

    public long getFee() {
        return transactionResult.getFee();
    }

    public void setUnfreezeAmount(long amount) {
        this.transactionResult = this.transactionResult.toBuilder().setUnfreezeAmount(amount).build();
    }

    public long getUnfreezeAmount() {
        return transactionResult.getUnfreezeAmount();
    }

    public void setAssetIssueID(String id) {
        this.transactionResult = this.transactionResult.toBuilder().setAssetIssueID(id).build();
    }

    public String getAssetIssueID() {
        return transactionResult.getAssetIssueID();
    }

    public void setWithdrawAmount(long amount) {
        this.transactionResult = this.transactionResult.toBuilder().setWithdrawAmount(amount).build();
    }

    public long getWithdrawAmount() {
        return transactionResult.getWithdrawAmount();
    }

    public void setExchangeReceivedAmount(long amount) {
        this.transactionResult = this.transactionResult.toBuilder().setExchangeReceivedAmount(amount)
                .build();
    }

    public long getExchangeReceivedAmount() {
        return transactionResult.getExchangeReceivedAmount();
    }


    public void setExchangeWithdrawAnotherAmount(long amount) {
        this.transactionResult = this.transactionResult.toBuilder()
                .setExchangeWithdrawAnotherAmount(amount)
                .build();
    }

    public long getExchangeWithdrawAnotherAmount() {
        return transactionResult.getExchangeWithdrawAnotherAmount();
    }


    public void setExchangeInjectAnotherAmount(long amount) {
        this.transactionResult = this.transactionResult.toBuilder()
                .setExchangeInjectAnotherAmount(amount)
                .build();
    }

    public long getExchangeId() {
        return transactionResult.getExchangeId();
    }

    public void setExchangeId(long id) {
        this.transactionResult = this.transactionResult.toBuilder()
                .setExchangeId(id)
                .build();
    }

    public long getExchangeInjectAnotherAmount() {
        return transactionResult.getExchangeInjectAnotherAmount();
    }

    public void setFee(long fee) {
        this.transactionResult = this.transactionResult.toBuilder().setFee(fee).build();
    }

    public void addFee(long fee) {
        this.transactionResult = this.transactionResult.toBuilder()
                .setFee(this.transactionResult.getFee() + fee).build();
    }

    public void setErrorCode(Transaction.Result.code code) {
        this.transactionResult = this.transactionResult.toBuilder().setRet(code).build();
    }

    @Override
    public byte[] getData() {
        return this.transactionResult.toByteArray();
    }

    @Override
    public Result getInstance() {
        return this.transactionResult;
    }
}