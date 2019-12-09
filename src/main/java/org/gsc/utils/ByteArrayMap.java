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


package org.gsc.utils;


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.gsc.db.ByteArrayWrapper;


public class ByteArrayMap<V> implements Map<byte[], V> {
    private static final String RUNTIME_EXCEPTION_MSG = "Not implemented";

    private final Map<ByteArrayWrapper, V> delegate;

    public ByteArrayMap() {
        this(new HashMap<ByteArrayWrapper, V>());
    }

    public ByteArrayMap(Map<ByteArrayWrapper, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(new ByteArrayWrapper((byte[]) key));
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return delegate.get(new ByteArrayWrapper((byte[]) key));
    }

    @Override
    public V put(byte[] key, V value) {
        return delegate.put(new ByteArrayWrapper(key), value);
    }

    @Override
    public V remove(Object key) {
        return delegate.remove(new ByteArrayWrapper((byte[]) key));
    }

    @Override
    public void putAll(Map<? extends byte[], ? extends V> m) {
        for (Entry<? extends byte[], ? extends V> entry : m.entrySet()) {
            delegate.put(new ByteArrayWrapper(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<byte[]> keySet() {
        return new ByteArraySet(new SetAdapter<>(delegate));
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<byte[], V>> entrySet() {
        return new MapEntrySet(delegate.entrySet());
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    private class MapEntrySet implements Set<Entry<byte[], V>> {

        private final Set<Entry<ByteArrayWrapper, V>> delegate;

        private MapEntrySet(Set<Entry<ByteArrayWrapper, V>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public Iterator<Entry<byte[], V>> iterator() {
            final Iterator<Entry<ByteArrayWrapper, V>> it = delegate.iterator();
            return new Iterator<Entry<byte[], V>>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Entry<byte[], V> next() {
                    Entry<ByteArrayWrapper, V> next = it.next();
                    return Maps.immutableEntry(next.getKey().getData(), next.getValue());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public Object[] toArray() {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public boolean add(Entry<byte[], V> vEntry) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public boolean addAll(Collection<? extends Entry<byte[], V>> c) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public boolean remove(Object o) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
        }

        @Override
        public void clear() {
            throw new RuntimeException(RUNTIME_EXCEPTION_MSG);

        }
    }
}
