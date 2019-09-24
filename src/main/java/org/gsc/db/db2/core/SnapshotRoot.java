package org.gsc.db.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import org.gsc.db.common.WrappedByteArray;
import org.gsc.db.db2.common.DB;
import org.gsc.db.db2.common.Flusher;
import org.gsc.db.db2.common.LevelDB;
import org.gsc.db.db2.common.RocksDB;
import org.gsc.db.db2.common.TxCacheDB;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

    @Getter
    private Snapshot confirmed;

    public SnapshotRoot(String parentName, String name, Class<? extends DB> clz) {
        try {
            if (clz == LevelDB.class || clz == RocksDB.class) {
                Constructor constructor = clz.getConstructor(String.class, String.class);
                @SuppressWarnings("unchecked")
                DB<byte[], byte[]> db = (DB<byte[], byte[]>) constructor
                        .newInstance((Object) parentName, (Object) name);
                this.db = db;
            } else if (clz == TxCacheDB.class) {
                @SuppressWarnings("unchecked")
                DB<byte[], byte[]> db = (DB<byte[], byte[]>) clz.newInstance();
                this.db = db;
            } else {
                throw new IllegalArgumentException();
            }
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException();
        }

        confirmed = this;
    }

    @Override
    public byte[] get(byte[] key) {
        return db.get(key);
    }

    @Override
    public void put(byte[] key, byte[] value) {
        db.put(key, value);
    }

    @Override
    public void remove(byte[] key) {
        db.remove(key);
    }

    @Override
    public void merge(Snapshot from) {
        SnapshotImpl snapshot = (SnapshotImpl) from;
        Map<WrappedByteArray, WrappedByteArray> batch = Streams.stream(snapshot.db)
                .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
                        WrappedByteArray.of(e.getValue().getBytes())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ((Flusher) db).flush(batch);
    }

    public void merge(List<Snapshot> snapshots) {
        Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
        for (Snapshot snapshot : snapshots) {
            SnapshotImpl from = (SnapshotImpl) snapshot;
            Streams.stream(from.db)
                    .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
                            WrappedByteArray.of(e.getValue().getBytes())))
                    .forEach(e -> batch.put(e.getKey(), e.getValue()));
        }

        ((Flusher) db).flush(batch);
    }

    @Override
    public Snapshot retreat() {
        return this;
    }

    @Override
    public Snapshot getRoot() {
        return this;
    }

    @Override
    public Iterator<Map.Entry<byte[], byte[]>> iterator() {
        return db.iterator();
    }

    @Override
    public void close() {
        ((Flusher) db).close();
    }

    @Override
    public void reset() {
        ((Flusher) db).reset();
    }

    @Override
    public void resetConfirmed() {
        confirmed = this;
    }

    @Override
    public void updateConfirmed() {
        confirmed = confirmed.getNext();
    }
}
