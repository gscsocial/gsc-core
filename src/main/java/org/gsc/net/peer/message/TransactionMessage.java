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

import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.net.peer.p2p.Message;
import org.gsc.utils.Sha256Hash;
import org.gsc.protos.Protocol.Transaction;

public class TransactionMessage extends GSCMessage {

    private TransactionWrapper transactionWrapper;

    public TransactionMessage(byte[] data) throws Exception {
        super(data);
        this.transactionWrapper = new TransactionWrapper(getCodedInputStream(data));
        this.type = MessageTypes.TRX.asByte();
        if (Message.isFilter()) {
            compareBytes(data, transactionWrapper.getInstance().toByteArray());
            transactionWrapper
                    .validContractProto(transactionWrapper.getInstance().getRawData().getContract(0));
        }
    }

    public TransactionMessage(Transaction trx) {
        this.transactionWrapper = new TransactionWrapper(trx);
        this.type = MessageTypes.TRX.asByte();
        this.data = trx.toByteArray();
    }

    @Override
    public String toString() {
        return new StringBuilder().append(super.toString())
                .append("messageId: ").append(super.getMessageId()).toString();
    }

    @Override
    public Sha256Hash getMessageId() {
        return this.transactionWrapper.getTransactionId();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public TransactionWrapper getTransactionWrapper() {
        return this.transactionWrapper;
    }
}
