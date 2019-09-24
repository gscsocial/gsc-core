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

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.protos.Protocol.TransactionRet;

@Slf4j(topic = "wrapper")
public class TransactionRetWrapper implements ProtoWrapper<TransactionRet> {
    private TransactionRet transactionRet;

    public TransactionRetWrapper(BlockWrapper blockWrapper) {
        transactionRet = TransactionRet.newBuilder().build();
        if (Objects.isNull(blockWrapper)) {
            return;
        }
        TransactionRet.Builder build = transactionRet.toBuilder()
                .setBlockNumber(blockWrapper.getNum()).setBlockTimeStamp(blockWrapper.getTimeStamp());
        transactionRet = build.build();
    }

    // only for test
    public TransactionRetWrapper() {
        transactionRet = TransactionRet.newBuilder().build();
    }

    public TransactionRetWrapper(byte[] data) throws BadItemException {
        try {
            this.transactionRet = transactionRet.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new BadItemException("TransactionInfoWrapper proto data parse exception");
        }
    }

    public void addTransactionInfo(TransactionInfo result) {
        this.transactionRet = this.transactionRet.toBuilder().addTransactioninfo(result).build();
    }

    @Override
    public byte[] getData() {
        if (Objects.isNull(transactionRet)) {
            return null;
        }
        return transactionRet.toByteArray();
    }

    @Override
    public TransactionRet getInstance() {
        return transactionRet;
    }
}