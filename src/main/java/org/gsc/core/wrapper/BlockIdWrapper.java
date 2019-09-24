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

package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;

import java.util.Arrays;

import org.gsc.utils.Sha256Hash;

public class BlockIdWrapper extends Sha256Hash implements ProtoWrapper {

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public Object getInstance() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || (getClass() != o.getClass() && !(o instanceof Sha256Hash))) {
            return false;
        }
        return Arrays.equals(getBytes(), ((Sha256Hash) o).getBytes());
    }

    public String getString() {
        return "Num: " + num + ",ID:" + super.toString();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int compareTo(Sha256Hash other) {
        if (other.getClass().equals(BlockIdWrapper.class)) {
            long otherNum = ((BlockIdWrapper) other).getNum();
            return Long.compare(num, otherNum);
        }
        return super.compareTo(other);
    }

    private long num;

    public BlockIdWrapper() {
        super(Sha256Hash.ZERO_HASH.getBytes());
        num = 0;
    }

    /**
     * Use {@link #wrap(byte[])} instead.
     */
    public BlockIdWrapper(Sha256Hash hash, long num) {
        super(hash.getBytes());
        this.num = num;
    }

    public BlockIdWrapper(byte[] hash, long num) {
        super(hash);
        this.num = num;
    }

    public BlockIdWrapper(ByteString hash, long num) {
        super(hash.toByteArray());
        this.num = num;
    }

    public long getNum() {
        return num;
    }
}
