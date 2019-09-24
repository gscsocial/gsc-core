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

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.CodeWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j(topic = "DB")
@Component
public class CodeStore extends GSCStoreWithRevoking<CodeWrapper> {

    @Autowired
    private CodeStore(@Value("contract_code") String dbName) {
        super(dbName);
    }

    @Override
    public CodeWrapper get(byte[] key) {
        return getUnchecked(key);
    }

    public long getTotalCodes() {
        return Streams.stream(revokingDB.iterator()).count();
    }

    public byte[] findCodeByHash(byte[] hash) {
        return revokingDB.getUnchecked(hash);
    }
}
