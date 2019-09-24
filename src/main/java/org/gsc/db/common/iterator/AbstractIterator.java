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

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public abstract class AbstractIterator<T> implements Iterator<Map.Entry<byte[], T>> {

    protected Iterator<Map.Entry<byte[], byte[]>> iterator;
    private TypeToken<T> typeToken = new TypeToken<T>(getClass()) {
    };

    public AbstractIterator(Iterator<Map.Entry<byte[], byte[]>> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    protected T of(byte[] value) {
        try {
            @SuppressWarnings("unchecked")
            T t = (T) typeToken.getRawType().getConstructor(byte[].class).newInstance(value);
            return t;
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map.Entry<byte[], T> next() {
        Entry<byte[], byte[]> entry = iterator.next();
        return Maps.immutableEntry(entry.getKey(), of(entry.getValue()));
    }
}
