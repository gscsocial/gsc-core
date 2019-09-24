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

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountIndexStore extends GSCStoreWithRevoking<BytesWrapper> {

    @Autowired
    public AccountIndexStore(@Value("account_index") String dbName) {
        super(dbName);
    }

    public void put(AccountWrapper accountWrapper) {
        put(accountWrapper.getAccountName().toByteArray(),
                new BytesWrapper(accountWrapper.getAddress().toByteArray()));
    }

    public byte[] get(ByteString name) {
        BytesWrapper bytesWrapper = get(name.toByteArray());
        if (Objects.nonNull(bytesWrapper)) {
            return bytesWrapper.getData();
        }
        return null;
    }

    @Override
    public BytesWrapper get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        if (ArrayUtils.isEmpty(value)) {
            return null;
        }
        return new BytesWrapper(value);
    }

    @Override
    public boolean has(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return !ArrayUtils.isEmpty(value);
    }
}