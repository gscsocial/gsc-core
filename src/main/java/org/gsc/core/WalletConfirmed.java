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

package org.gsc.core;

import com.google.protobuf.ByteString;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI.TransactionList;
import org.gsc.utils.ByteArray;
import org.gsc.db.api.StoreAPI;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
@Component
public class WalletConfirmed {

    @Autowired
    private StoreAPI storeAPI;

    public TransactionList getTransactionsFromThis(ByteString thisAddress, long offset, long limit) {
        List<Transaction> transactionsFromThis = storeAPI
                .getTransactionsFromThis(ByteArray.toHexString(thisAddress.toByteArray()), offset, limit);
        TransactionList transactionList = TransactionList.newBuilder()
                .addAllTransaction(transactionsFromThis).build();
        return transactionList;
    }

    public TransactionList getTransactionsToThis(ByteString toAddress, long offset, long limit) {
        List<Transaction> transactionsToThis = storeAPI
                .getTransactionsToThis(ByteArray.toHexString(toAddress.toByteArray()), offset, limit);
        TransactionList transactionList = TransactionList.newBuilder()
                .addAllTransaction(transactionsToThis).build();
        return transactionList;
    }
}
