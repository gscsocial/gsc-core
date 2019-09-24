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

package org.gsc.db.common.iterator;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksIterator;

@Slf4j
public final class RockStoreIterator implements DBIterator {

    private RocksIterator dbIterator;
    private boolean first = true;

    public RockStoreIterator(RocksIterator dbIterator) {
        this.dbIterator = dbIterator;
    }

    @Override
    public void close() throws IOException {
        dbIterator.close();
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = false;
        // true is first item
        try {
            if (first) {
                dbIterator.seekToFirst();
                first = false;
            }
            if (!(hasNext = dbIterator.isValid())) { // false is last item
                dbIterator.close();
            }
        } catch (Exception e) {
            System.out.println("e:" + e);
            try {
                dbIterator.close();
            } catch (Exception e1) {
                System.out.println("e1:" + e1);
            }
        }
        return hasNext;
    }

    @Override
    public Entry<byte[], byte[]> next() {
        if (!dbIterator.isValid()) {
            throw new NoSuchElementException();
        }
        byte[] key = dbIterator.key();
        byte[] value = dbIterator.value();
        dbIterator.next();
        return new Entry<byte[], byte[]>() {
            @Override
            public byte[] getKey() {
                return key;
            }

            @Override
            public byte[] getValue() {
                return value;
            }

            @Override
            public byte[] setValue(byte[] value) {
                throw new UnsupportedOperationException();
            }
        };
    }
}