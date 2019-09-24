package org.gsc.db.db2.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import lombok.Getter;
import org.gsc.db.common.WrappedByteArray;
import org.gsc.db.db2.common.HashDB;
import org.gsc.db.db2.common.Key;
import org.gsc.db.db2.common.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SnapshotImpl extends AbstractSnapshot<Key, Value> {

    @Getter
    protected Snapshot root;

    SnapshotImpl(Snapshot snapshot) {
        root = snapshot.getRoot();
        previous = snapshot;
        snapshot.setNext(this);
        synchronized (this) {
            db = new HashDB();
        }

    }

    @Override
    public byte[] get(byte[] key) {
        return get(this, key);
    }

    @Override
    public void put(byte[] key, byte[] value) {
        Preconditions.checkNotNull(key, "key in db is not null.");
        Preconditions.checkNotNull(value, "value in db is not null.");

        db.put(Key.copyOf(key), Value.copyOf(Value.Operator.PUT, value));
    }

    @Override
    public void remove(byte[] key) {
        Preconditions.checkNotNull(key, "key in db is not null.");
        db.put(Key.of(key), Value.of(Value.Operator.DELETE, null));
    }

    private byte[] get(Snapshot head, byte[] key) {
        Snapshot snapshot = head;
        Value value;
        while (Snapshot.isImpl(snapshot)) {
            if ((value = ((SnapshotImpl) snapshot).db.get(Key.of(key))) != null) {
                return value.getBytes();
            }

            snapshot = snapshot.getPrevious();
        }

        return snapshot == null ? null : snapshot.get(key);
    }

    // we have a 3x3 matrix of all possibilities when merging previous snapshot and current snapshot :
    //                  -------------- snapshot -------------
    //                 /                                     \
    //                +------------+------------+------------+
    //                | put(Y)     | delete(Y)  | nop        |
    //   +------------+------------+------------+------------+
    // / | put(X)     | put(Y)     | del        | put(X)     |
    // p +------------+------------+------------+------------+
    // r | delete(X)  | put(Y)     | del        | del        |
    // e +------------+------------+------------+------------+
    // | | nop        | put(Y)     | del        | nop        |
    // \ +------------+------------+------------+------------+
    @Override
    public void merge(Snapshot from) {
        SnapshotImpl fromImpl = (SnapshotImpl) from;
        Streams.stream(fromImpl.db).forEach(e -> db.put(e.getKey(), e.getValue()));
    }

    // we have a 4x4 matrix of all possibilities when merging previous snapshot and current snapshot :
    //                  --------------------- snapshot ------------------
    //                 /                                                 \
    //                +------------+------------+------------+------------+
    //                | new(Y)     | upd(Y)     | del        | nop        |
    //   +------------+------------+------------+------------+------------+
    // / | new(X)     | N/A        | new(Y)     | nop        | new(X)     |
    // | +------------+------------+------------+------------+------------+
    // p | upd(X)     | N/A        | upd(Y)     | del        | upd(X)     |
    // r +------------+------------+------------+------------+------------+
    // e | del        | upd(Y)     | N/A        | N/A        | del        |
    // | +------------+------------+------------+------------+------------+
    // \ | nop        | new(Y)     | upd(Y)     | del        | nop        |
    //   +------------+------------+------------+------------+------------+
    public void merge2(Snapshot from) {
        SnapshotImpl fromImpl = (SnapshotImpl) from;

        Streams.stream(fromImpl.db)
                .filter(e -> e.getValue().getOperator() == Value.Operator.CREATE)
                .forEach(e -> {
                    Key k = e.getKey();
                    Value v = e.getValue();
                    Value value = db.get(k);
                    if (value == null) {
                        db.put(k, v);
                    } else if (value.getOperator() == Value.Operator.DELETE) {
                        db.put(k, Value.copyOf(Value.Operator.MODIFY, v.getBytes()));
                    } else {
                        throw new IllegalStateException();
                    }
                });

        Streams.stream(fromImpl.db)
                .filter(e -> e.getValue().getOperator() == Value.Operator.MODIFY)
                .forEach(e -> {
                    Key k = e.getKey();
                    Value v = e.getValue();
                    Value value = db.get(k);
                    if (value == null || value.getOperator() == Value.Operator.MODIFY) {
                        db.put(k, v);
                    } else if (value.getOperator() == Value.Operator.CREATE) {
                        db.put(k, Value.copyOf(Value.Operator.CREATE, v.getBytes()));
                    } else {
                        throw new IllegalStateException();
                    }
                });

        Streams.stream(fromImpl.db)
                .filter(e -> e.getValue().getOperator() == Value.Operator.DELETE)
                .map(Map.Entry::getKey)
                .forEach(k -> {
                    Value value = db.get(k);
                    if (value == null || value.getOperator() == Value.Operator.MODIFY) {
                        db.put(k, Value.of(Value.Operator.DELETE, null));
                    } else if (value.getOperator() == Value.Operator.CREATE) {
                        db.remove(k);
                    } else {
                        throw new IllegalStateException();
                    }
                });
    }

    @Override
    public Snapshot retreat() {
        return previous;
    }

    @Override
    public Snapshot getConfirmed() {
        return root.getConfirmed();
    }

    @Override
    public Iterator<Map.Entry<byte[], byte[]>> iterator() {
        Map<WrappedByteArray, WrappedByteArray> all = new HashMap<>();
        collect(all);
        Set<WrappedByteArray> keys = new HashSet<>(all.keySet());
        all.entrySet()
                .removeIf(entry -> entry.getValue() == null || entry.getValue().getBytes() == null);
        return Iterators.concat(
                Iterators.transform(all.entrySet().iterator(),
                        e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes())),
                Iterators.filter(getRoot().iterator(),
                        e -> !keys.contains(WrappedByteArray.of(e.getKey()))));
    }

    synchronized void collect(Map<WrappedByteArray, WrappedByteArray> all) {
        Snapshot next = getRoot().getNext();
        while (next != null) {
            Streams.stream(((SnapshotImpl) next).db)
                    .forEach(e -> all.put(WrappedByteArray.of(e.getKey().getBytes()),
                            WrappedByteArray.of(e.getValue().getBytes())));
            next = next.getNext();
        }
    }

    @Override
    public void close() {
        getRoot().close();
    }

    @Override
    public void reset() {
        getRoot().reset();
    }

    @Override
    public void resetConfirmed() {
        root.resetConfirmed();
    }

    @Override
    public void updateConfirmed() {
        root.updateConfirmed();
    }
}
