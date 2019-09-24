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

package org.gsc.db.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WrappedByteArray {

    @Getter
    private byte[] bytes;

    public static WrappedByteArray of(byte[] bytes) {
        return new WrappedByteArray(bytes);
    }

    public static WrappedByteArray copyOf(byte[] bytes) {
        byte[] value = null;
        if (bytes != null) {
            value = Arrays.copyOf(bytes, bytes.length);
        }

        return new WrappedByteArray(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WrappedByteArray byteArray = (WrappedByteArray) o;
        return Arrays.equals(bytes, byteArray.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
