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

import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.gsc.db.dbsource.DbSourceInter;
import org.gsc.db.dbsource.leveldb.LevelDbDataSourceImpl;
import org.gsc.db.dbsource.leveldb.RocksDbDataSourceImpl;
import org.gsc.config.args.Args;
import org.gsc.db.api.IndexHelper;
import org.gsc.db.db2.core.IGSCChainBase;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;

@Slf4j(topic = "DB")
public abstract class GSCDatabase<T> implements IGSCChainBase<T> {

    protected DbSourceInter<byte[]> dbSource;
    @Getter
    private String dbName;

    @Autowired(required = false)
    protected IndexHelper indexHelper;

    protected GSCDatabase(String dbName) {
        this.dbName = dbName;

        if ("LEVELDB".equals(Args.getInstance().getStorage().getDbEngine().toUpperCase())) {
            dbSource =
                    new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
        } else if ("ROCKSDB".equals(Args.getInstance().getStorage().getDbEngine().toUpperCase())) {
            String parentName = Paths.get(Args.getInstance().getOutputDirectoryByDbName(dbName),
                    Args.getInstance().getStorage().getDbDirectory()).toString();
            dbSource =
                    new RocksDbDataSourceImpl(parentName, dbName);
        }

        dbSource.initDB();
    }

    protected GSCDatabase() {
    }

    public DbSourceInter<byte[]> getDbSource() {
        return dbSource;
    }

    /**
     * reset the database.
     */
    public void reset() {
        dbSource.resetDb();
    }

    /**
     * close the database.
     */
    @Override
    public void close() {
        dbSource.closeDB();
    }

    public abstract void put(byte[] key, T item);

    public abstract void delete(byte[] key);

    public abstract T get(byte[] key)
            throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

    public T getUnchecked(byte[] key) {
        return null;
    }

    public abstract boolean has(byte[] key);

    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Iterator<Entry<byte[], T>> iterator() {
        throw new UnsupportedOperationException();
    }
}
