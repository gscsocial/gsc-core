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

import java.io.Serializable;
import java.util.Arrays;

import org.spongycastle.util.encoders.Hex;
import org.gsc.utils.FastByteComparisons;


public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>, Serializable {

    private static final long serialVersionUID = -8645797230368480951L;

    private final byte[] data;
    private int hashCode = 0;

    /**
     * constructor.
     */
    public ByteArrayWrapper(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        this.data = data;
        this.hashCode = Arrays.hashCode(data);
    }


    /**
     * equals Objects.
     */
    public boolean equals(Object other) {
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        byte[] otherData = ((ByteArrayWrapper) other).getData();
        return FastByteComparisons.compareTo(
                data, 0, data.length,
                otherData, 0, otherData.length) == 0;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(ByteArrayWrapper o) {
        return FastByteComparisons.compareTo(
                data, 0, data.length,
                o.getData(), 0, o.getData().length);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return Hex.toHexString(data);
    }
}
