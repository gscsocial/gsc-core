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

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.wrapper.BytesWrapper;
import org.gsc.core.exception.ItemNotFoundException;

@Component
public class BlockIndexStore extends GSCStoreWithRevoking<BytesWrapper> {


    @Autowired
    public BlockIndexStore(@Value("block_index") String dbName) {
        super(dbName);

    }

    public void put(BlockId id) {
        put(ByteArray.fromLong(id.getNum()), new BytesWrapper(id.getBytes()));
    }

    public BlockId get(Long num)
            throws ItemNotFoundException {
        BytesWrapper value = getUnchecked(ByteArray.fromLong(num));
        if (value == null || value.getData() == null) {
            throw new ItemNotFoundException("number: " + num + " is not found!");
        }
        return new BlockId(Sha256Hash.wrap(value.getData()), num);
    }

    @Override
    public BytesWrapper get(byte[] key)
            throws ItemNotFoundException {
        byte[] value = revokingDB.getUnchecked(key);
        if (ArrayUtils.isEmpty(value)) {
            throw new ItemNotFoundException("number: " + Arrays.toString(key) + " is not found!");
        }
        return new BytesWrapper(value);
    }
}