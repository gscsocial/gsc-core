package org.gsc.db.db2.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.gsc.core.wrapper.BytesWrapper;

public class ConcurrentHashDB implements DB<byte[], BytesWrapper> {

    private Map<Key, BytesWrapper> db = new ConcurrentHashMap<>();


    @Override
    public BytesWrapper get(byte[] bytes) {
        return db.get(Key.of(bytes));
    }

    @Override
    public void put(byte[] bytes, BytesWrapper bytes2) {
        db.put(Key.of(bytes), bytes2);
    }

    @Override
    public long size() {
        return db.size();
    }

    @Override
    public boolean isEmpty() {
        return db.isEmpty();
    }

    @Override
    public void remove(byte[] bytes) {
        db.remove(Key.of(bytes));
    }

    @Override
    public Iterator<Entry<byte[], BytesWrapper>> iterator() {
        return null;
    }
}
