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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.gsc.core.wrapper.BytesWrapper;
import org.gsc.db.db2.common.TxCacheDB;

@Slf4j
public class TransactionCache extends GSCStoreWithRevoking<BytesWrapper> {

    @Autowired
    public TransactionCache(@Value("trans-cache") String dbName) {
        super(dbName, TxCacheDB.class);
    }
}
