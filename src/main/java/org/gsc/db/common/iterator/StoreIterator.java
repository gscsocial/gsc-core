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

import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DBIterator;

import java.io.IOException;
import java.util.Map.Entry;

@Slf4j(topic = "DB")
public final class StoreIterator implements org.gsc.db.common.iterator.DBIterator {

    private DBIterator dbIterator;
    private boolean first = true;

    public StoreIterator(DBIterator dbIterator) {
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

            if (!(hasNext = dbIterator.hasNext())) { // false is last item
                dbIterator.close();
            }
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }

        return hasNext;
    }

    @Override
    public Entry<byte[], byte[]> next() {
        return dbIterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
