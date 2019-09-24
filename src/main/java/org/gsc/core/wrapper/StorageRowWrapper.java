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

import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.vm.DataWord;
import org.gsc.utils.Sha256Hash;


@Slf4j(topic = "wrapper")
public class StorageRowWrapper implements ProtoWrapper<byte[]> {

    @Getter
    private byte[] rowValue;
    @Setter
    @Getter
    private byte[] rowKey;

    @Getter
    private boolean dirty = false;

    public StorageRowWrapper(StorageRowWrapper rowWrapper) {
        this.rowKey = rowWrapper.getRowKey().clone();
        this.rowValue = rowWrapper.getRowValue().clone();
        this.dirty = rowWrapper.isDirty();
    }

    public StorageRowWrapper(byte[] rowKey, byte[] rowValue) {
        this.rowKey = rowKey;
        this.rowValue = rowValue;
        markDirty();
    }

    public StorageRowWrapper(byte[] rowValue) {
        this.rowValue = rowValue;
    }

    private void markDirty() {
        dirty = true;
    }

    public Sha256Hash getHash() {
        return Sha256Hash.of(this.rowValue);
    }

    public DataWord getValue() {
        return new DataWord(this.rowValue);
    }

    public void setValue(DataWord value) {
        this.rowValue = value.getData();
        markDirty();
    }

    @Override
    public byte[] getData() {
        return this.rowValue;
    }

    @Override
    public byte[] getInstance() {
        return this.rowValue;
    }

    @Override
    public String toString() {
        return Arrays.toString(rowValue);
    }
}
