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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gsc.db.ByteArrayWrapper;


public class ByteArraySet implements Set<byte[]> {
    private static final String RUNTIME_EXCEPTION_MSG = "Not implemented";

    private Set<ByteArrayWrapper> delegate;

    public ByteArraySet() {
        this(new HashSet<>());
    }

    ByteArraySet(Set<ByteArrayWrapper> delegate) {
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
        return delegate.contains(new ByteArrayWrapper((byte[]) o));
    }

    @Override
    public Iterator<byte[]> iterator() {
        return new Iterator<byte[]>() {

            Iterator<ByteArrayWrapper> it = delegate.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public void remove() {
                it.remove();
            }

            @Override
            public byte[] next() {
                return it.next().getData();
            }

        };
    }

    @Override
    public Object[] toArray() {
        byte[][] ret = new byte[size()][];

        ByteArrayWrapper[] arr = delegate.toArray(new ByteArrayWrapper[size()]);
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i].getData();
        }
        return ret;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    @Override
    public boolean add(byte[] bytes) {
        return delegate.add(new ByteArrayWrapper(bytes));
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(new ByteArrayWrapper((byte[]) o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
    }

    @Override
    public boolean addAll(Collection<? extends byte[]> c) {
        boolean ret = false;
        for (byte[] bytes : c) {
            ret |= add(bytes);
        }
        return ret;
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
        delegate.clear();
    }

    @Override
    public boolean equals(Object o) {
        throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
    }

    @Override
    public int hashCode() {
        throw new RuntimeException(RUNTIME_EXCEPTION_MSG);
    }
}
