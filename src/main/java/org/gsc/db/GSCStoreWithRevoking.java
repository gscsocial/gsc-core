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

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.gsc.core.wrapper.ProtoWrapper;
import org.gsc.config.args.Args;
import org.gsc.db.api.IndexHelper;
import org.gsc.db.db2.common.DB;
import org.gsc.db.db2.common.IRevokingDB;
import org.gsc.db.db2.common.LevelDB;
import org.gsc.db.db2.common.RocksDB;
import org.gsc.db.db2.core.IGSCChainBase;
import org.gsc.db.db2.core.RevokingDBWithCachingNewValue;
import org.gsc.db.db2.core.RevokingDBWithCachingOldValue;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;

@Slf4j(topic = "DB")
public abstract class GSCStoreWithRevoking<T extends ProtoWrapper> implements IGSCChainBase<T> {

    @Getter // only for unit test
    protected IRevokingDB revokingDB;
    private TypeToken<T> token = new TypeToken<T>(getClass()) {
    };

    @Autowired
    private RevokingDatabase revokingDatabase;
    @Autowired(required = false)
    protected IndexHelper indexHelper;
    @Getter
    private String dbName;

    protected GSCStoreWithRevoking(String dbName) {
        this.dbName = dbName;
        int dbVersion = Args.getInstance().getStorage().getDbVersion();
        String dbEngine = Args.getInstance().getStorage().getDbEngine();
        if (dbVersion == 1) {
            this.revokingDB = new RevokingDBWithCachingOldValue(dbName);
        } else if (dbVersion == 2) {
            if ("LEVELDB".equals(dbEngine.toUpperCase())) {
                this.revokingDB = new RevokingDBWithCachingNewValue(dbName, LevelDB.class);
            } else if ("ROCKSDB".equals(dbEngine.toUpperCase())) {
                this.revokingDB = new RevokingDBWithCachingNewValue(dbName, RocksDB.class);
            }
        } else {
            throw new RuntimeException("db version is error.");
        }
    }

    protected GSCStoreWithRevoking(String dbName, Class<? extends DB> clz) {
        this.dbName = dbName;
        int dbVersion = Args.getInstance().getStorage().getDbVersion();
        if (dbVersion == 2) {
            this.revokingDB = new RevokingDBWithCachingNewValue(dbName, clz);
        } else {
            throw new RuntimeException("db version is only 2.(" + dbVersion + ")");
        }
    }

    @PostConstruct
    private void init() {
        revokingDatabase.add(revokingDB);
    }

    // only for test
    protected GSCStoreWithRevoking(String dbName, RevokingDatabase revokingDatabase) {
        this.revokingDB = new RevokingDBWithCachingOldValue(dbName,
                (AbstractRevokingStore) revokingDatabase);
    }

    @Override
    public void put(byte[] key, T item) {
        if (Objects.isNull(key) || Objects.isNull(item)) {
            return;
        }

        revokingDB.put(key, item.getData());
    }

    @Override
    public void delete(byte[] key) {
        revokingDB.delete(key);
    }

    @Override
    public T get(byte[] key) throws ItemNotFoundException, BadItemException {
        return of(revokingDB.get(key));
    }

    @Override
    public T getUnchecked(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);

        try {
            return of(value);
        } catch (BadItemException e) {
            return null;
        }
    }

    public T of(byte[] value) throws BadItemException {
        try {
            Constructor constructor = token.getRawType().getConstructor(byte[].class);
            @SuppressWarnings("unchecked")
            T t = (T) constructor.newInstance((Object) value);
            return t;
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new BadItemException(e.getMessage());
        }
    }

    @Override
    public boolean has(byte[] key) {
        return revokingDB.has(key);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void close() {
        revokingDB.close();
    }

    @Override
    public void reset() {
        revokingDB.reset();
    }

    @Override
    public Iterator<Map.Entry<byte[], T>> iterator() {
        return Iterators.transform(revokingDB.iterator(), e -> {
            try {
                return Maps.immutableEntry(e.getKey(), of(e.getValue()));
            } catch (BadItemException e1) {
                throw new RuntimeException(e1);
            }
        });
    }

    public long size() {
        return Streams.stream(revokingDB.iterator()).count();
    }

    public void setMode(boolean mode) {
        revokingDB.setMode(mode);
    }
}
