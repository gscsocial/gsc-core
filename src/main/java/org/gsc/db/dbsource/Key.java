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

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class Key {
    /**
     * data could not be null
     */
    private byte[] data = new byte[0];

    /**
     * @param data
     */
    public Key(byte[] data) {
        if (data != null && data.length != 0) {
            this.data = new byte[data.length];
            System.arraycopy(data, 0, this.data, 0, data.length);
        }
    }

    /**
     * @param key
     */
    private Key(Key key) {
        this.data = new byte[key.getData().length];
        System.arraycopy(key.getData(), 0, this.data, 0, this.data.length);
    }

    /**
     * @return
     */
    public Key clone() {
        return new Key(this);
    }

    /**
     * @return
     */
    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Key key = (Key) o;
        return Arrays.equals(key.getData(), this.data);
    }

    @Override
    public int hashCode() {
        return data != null ? ArrayUtils.hashCode(data) : 0;
    }

    /**
     * @param data
     * @return
     */
    public static Key create(byte[] data) {
        return new Key(data);
    }
}
