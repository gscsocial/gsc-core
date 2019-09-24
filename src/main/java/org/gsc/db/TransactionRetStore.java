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

package org.gsc.db;

import com.google.protobuf.ByteString;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.gsc.core.wrapper.TransactionInfoWrapper;
import org.gsc.core.wrapper.TransactionRetWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j(topic = "DB")
@Component
public class TransactionRetStore extends GSCStoreWithRevoking<TransactionRetWrapper> {

    @Autowired
    private TransactionStore transactionStore;

    @Autowired
    public TransactionRetStore(@Value("transaction_result_store") String dbName) {
        super(dbName);
    }

    @Override
    public void put(byte[] key, TransactionRetWrapper item) {
        if (BooleanUtils.toBoolean(Args.getInstance().getStorage().getTransactionHistoreSwitch())) {
            super.put(key, item);
        }
    }

    public TransactionInfoWrapper getTransactionInfo(byte[] key) throws BadItemException {
        long blockNumber = transactionStore.getBlockNumber(key);
        if (blockNumber == -1) {
            return null;
        }
        byte[] value = revokingDB.getUnchecked(ByteArray.fromLong(blockNumber));
        if (Objects.isNull(value)) {
            return null;
        }

        TransactionRetWrapper result = new TransactionRetWrapper(value);
        if (Objects.isNull(result) || Objects.isNull(result.getInstance())) {
            return null;
        }

        for (TransactionInfo transactionResultInfo : result.getInstance().getTransactioninfoList()) {
            if (transactionResultInfo.getId().equals(ByteString.copyFrom(key))) {
                return new TransactionInfoWrapper(transactionResultInfo);
            }
        }
        return null;
    }

}
