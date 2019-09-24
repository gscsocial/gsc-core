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

package org.gsc.db.dbsource;

import lombok.Getter;

public class WriteOptionsWrapper {

    @Getter
    private org.rocksdb.WriteOptions rocks = null;
    @Getter
    private org.iq80.leveldb.WriteOptions level = null;

    public static WriteOptionsWrapper getInstance() {
        WriteOptionsWrapper wapper = new WriteOptionsWrapper();
        wapper.level = new org.iq80.leveldb.WriteOptions();
        wapper.rocks = new org.rocksdb.WriteOptions();
        return wapper;
    }

    public WriteOptionsWrapper sync(boolean bool) {
        this.level.sync(bool);
        this.rocks.setSync(bool);
        return this;
    }
}