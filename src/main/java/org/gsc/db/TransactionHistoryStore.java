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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.gsc.core.wrapper.TransactionInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BadItemException;

@Component
public class TransactionHistoryStore extends GSCStoreWithRevoking<TransactionInfoWrapper> {

    @Autowired
    public TransactionHistoryStore(@Value("transaction_info_store") String dbName) {
        super(dbName);
    }

    @Override
    public TransactionInfoWrapper get(byte[] key) throws BadItemException {
        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new TransactionInfoWrapper(value);
    }

    @Override
    public void put(byte[] key, TransactionInfoWrapper item) {
        if (BooleanUtils.toBoolean(Args.getInstance().getStorage().getTransactionHistoreSwitch())) {
            super.put(key, item);
        }
    }
}