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

package org.gsc.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.disk.DiskIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;

import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.db.common.WrappedByteArray;
import org.gsc.db.db2.core.IGSCChainBase;
import org.gsc.protos.Protocol.Transaction;

@Component
@Slf4j(topic = "DB")
public class TransactionIndex extends AbstractIndex<TransactionWrapper, Transaction> {

    public static SimpleAttribute<WrappedByteArray, String> Transaction_ID;
    public static Attribute<WrappedByteArray, String> OWNERS;
    public static Attribute<WrappedByteArray, String> TOS;
    public static Attribute<WrappedByteArray, Long> TIMESTAMP;

    @Autowired
    public TransactionIndex(
            @Qualifier("transactionStore") final IGSCChainBase<TransactionWrapper> database) {
        super(database);
    }

    @PostConstruct
    public void init() {
        initIndex(DiskPersistence.onPrimaryKeyInFile(Transaction_ID, indexPath));
//    index.addIndex(DiskIndex.onAttribute(Transaction_ID));
        index.addIndex(DiskIndex.onAttribute(OWNERS));
        index.addIndex(DiskIndex.onAttribute(TOS));
        index.addIndex(DiskIndex.onAttribute(TIMESTAMP));
    }

    @Override
    protected void setAttribute() {
        Transaction_ID =
                attribute("transaction id",
                        bytes -> new TransactionWrapper(getObject(bytes)).getTransactionId().toString());
        OWNERS =
                attribute(String.class, "owner address",
                        bytes -> getObject(bytes).getRawData().getContractList().stream()
                                .map(TransactionWrapper::getOwner)
                                .filter(Objects::nonNull)
                                .map(ByteArray::toHexString)
                                .collect(Collectors.toList()));
        TOS =
                attribute(String.class, "to address",
                        bytes -> getObject(bytes).getRawData().getContractList().stream()
                                .map(TransactionWrapper::getToAddress)
                                .filter(Objects::nonNull)
                                .map(ByteArray::toHexString)
                                .collect(Collectors.toList()));
        TIMESTAMP =
                attribute("timestamp", bytes -> getObject(bytes).getRawData().getTimestamp());
    }
}
