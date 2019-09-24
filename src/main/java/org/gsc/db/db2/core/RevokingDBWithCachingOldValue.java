package org.gsc.db.db2.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.db.dbsource.leveldb.LevelDbDataSourceImpl;
import org.gsc.config.args.Args;
import org.gsc.db.AbstractRevokingStore;
import org.gsc.db.RevokingStore;
import org.gsc.db.common.WrappedByteArray;
import org.gsc.db.db2.common.IRevokingDB;
import org.gsc.core.exception.ItemNotFoundException;

public class RevokingDBWithCachingOldValue implements IRevokingDB {

    private AbstractRevokingStore revokingDatabase;
    @Getter
    private LevelDbDataSourceImpl dbSource;

    public RevokingDBWithCachingOldValue(String dbName) {
        this(dbName, RevokingStore.getInstance());
    }

    // only for unit test
    public RevokingDBWithCachingOldValue(String dbName, AbstractRevokingStore revokingDatabase) {
        dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName(dbName),
                dbName);
        dbSource.initDB();
        this.revokingDatabase = revokingDatabase;
    }

    @Override
    public void put(byte[] key, byte[] newValue) {
        if (Objects.isNull(key) || Objects.isNull(newValue)) {
            return;
        }
        //logger.info("Address is {}, " + item.getClass().getSimpleName() + " is {}", key, item);
        byte[] value = dbSource.getData(key);
        if (ArrayUtils.isNotEmpty(value)) {
            onModify(key, value);
        }

        dbSource.putData(key, newValue);

        if (ArrayUtils.isEmpty(value)) {
            onCreate(key);
        }
    }

    @Override
    public void delete(byte[] key) {
        onDelete(key);
        dbSource.deleteData(key);
    }

    @Override
    public boolean has(byte[] key) {
        return dbSource.getData(key) != null;
    }

    @Override
    public byte[] get(byte[] key) throws ItemNotFoundException {
        byte[] value = dbSource.getData(key);
        if (ArrayUtils.isEmpty(value)) {
            throw new ItemNotFoundException();
        }
        return value;
    }

    @Override
    public byte[] getUnchecked(byte[] key) {
        try {
            return get(key);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    @Override
    public void close() {
        dbSource.closeDB();
    }

    @Override
    public void reset() {
        dbSource.resetDb();
    }

    @Override
    public void setMode(boolean mode) {
    }

    /**
     * This should be called just after an object is created
     */
    private void onCreate(byte[] key) {
        revokingDatabase.onCreate(new AbstractRevokingStore.RevokingTuple(dbSource, key), null);
    }

    /**
     * This should be called just before an object is modified
     */
    private void onModify(byte[] key, byte[] value) {
        revokingDatabase.onModify(new AbstractRevokingStore.RevokingTuple(dbSource, key), value);
    }

    /**
     * This should be called just before an object is removed.
     */
    private void onDelete(byte[] key) {
        byte[] value;
        if (Objects.nonNull(value = dbSource.getData(key))) {
            revokingDatabase.onRemove(new AbstractRevokingStore.RevokingTuple(dbSource, key), value);
        }
    }

    @Override
    public Iterator<Map.Entry<byte[], byte[]>> iterator() {
        return dbSource.iterator();
    }

    @Override
    public Set<byte[]> getlatestValues(long limit) {
        return dbSource.getlatestValues(limit);
    }

    @Override
    public Set<byte[]> getValuesNext(byte[] key, long limit) {
        return dbSource.getValuesNext(key, limit);
    }

    @Override
    public Set<byte[]> getValuesPrevious(byte[] key, long limit) {
        return dbSource.getPrevious(key, limit, Long.SIZE / Byte.SIZE).values().stream()
                .collect(Collectors.toSet());
    }

    public Map<WrappedByteArray, WrappedByteArray> getAllValues() {
        Map<WrappedByteArray, WrappedByteArray> result = new HashMap<>();
        dbSource.getAll().forEach((key, value) -> {
            result.put(WrappedByteArray.of(key), WrappedByteArray.of(value));
        });
        return result;
    }
}
