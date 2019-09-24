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

package org.gsc.net.peer.message;

import java.util.List;

import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Transaction;

public class TransactionsMessage extends GSCMessage {

    private Protocol.Transactions transactions;

    public TransactionsMessage(List<Transaction> trxs) {
        Protocol.Transactions.Builder builder = Protocol.Transactions.newBuilder();
        trxs.forEach(trx -> builder.addTransactions(trx));
        this.transactions = builder.build();
        this.type = MessageTypes.TRXS.asByte();
        this.data = this.transactions.toByteArray();
    }

    public TransactionsMessage(byte[] data) throws Exception {
        super(data);
        this.type = MessageTypes.TRXS.asByte();
        this.transactions = Protocol.Transactions.parseFrom(getCodedInputStream(data));
        if (isFilter()) {
            compareBytes(data, transactions.toByteArray());
            TransactionWrapper.validContractProto(transactions.getTransactionsList());
        }
    }

    public Protocol.Transactions getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(super.toString()).append("trx size: ")
                .append(this.transactions.getTransactionsList().size()).toString();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

}
