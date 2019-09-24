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

package org.gsc.runtime.vm.program;

import static java.lang.System.arraycopy;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import org.gsc.core.wrapper.StorageRowWrapper;
import org.gsc.crypto.Hash;
import org.gsc.runtime.vm.DataWord;
import org.gsc.utils.ByteUtil;
import org.gsc.db.StorageRowStore;

public class Storage {

    @Getter
    private byte[] addrHash;
    @Getter
    private StorageRowStore store;
    @Getter
    private final Map<DataWord, StorageRowWrapper> rowCache = new HashMap<>();

    @Getter
    private byte[] address;

    private static final int PREFIX_BYTES = 16;

    public Storage(byte[] address, StorageRowStore store) {
        addrHash = addrHash(address);
        this.address = address;
        this.store = store;
    }

    public void generateAddrHash(byte[] trxId) {
        // update addreHash for create2
        addrHash = addrHash(address, trxId);
    }

    public Storage(Storage storage) {
        this.addrHash = storage.addrHash.clone();
        this.address = storage.getAddress().clone();
        this.store = storage.store;
        storage.getRowCache().forEach((DataWord rowKey, StorageRowWrapper row) -> {
            StorageRowWrapper newRow = new StorageRowWrapper(row);
            this.rowCache.put(rowKey.clone(), newRow);
        });
    }

    public DataWord getValue(DataWord key) {
        if (rowCache.containsKey(key)) {
            return rowCache.get(key).getValue();
        } else {
            StorageRowWrapper row = store.get(compose(key.getData(), addrHash));
            if (row == null || row.getInstance() == null) {
                return null;
            }
            rowCache.put(key, row);
            return row.getValue();
        }
    }

    public void put(DataWord key, DataWord value) {
        if (rowCache.containsKey(key)) {
            rowCache.get(key).setValue(value);
        } else {
            byte[] rowKey = compose(key.getData(), addrHash);
            StorageRowWrapper row = new StorageRowWrapper(rowKey, value.getData());
            rowCache.put(key, row);
        }
    }

    private static byte[] compose(byte[] key, byte[] addrHash) {
        byte[] result = new byte[key.length];
        arraycopy(addrHash, 0, result, 0, PREFIX_BYTES);
        arraycopy(key, PREFIX_BYTES, result, PREFIX_BYTES, PREFIX_BYTES);
        return result;
    }

    // 32 bytes
    private static byte[] addrHash(byte[] address) {
        return Hash.sha3(address);
    }

    private static byte[] addrHash(byte[] address, byte[] trxHash) {
        if (ByteUtil.isNullOrZeroArray(trxHash)) {
            return Hash.sha3(address);
        }
        return Hash.sha3(ByteUtil.merge(address, trxHash));
    }

    public void commit() {
        rowCache.forEach((DataWord rowKey, StorageRowWrapper row) -> {
            if (row.isDirty()) {
                if (row.getValue().isZero()) {
                    this.store.delete(row.getRowKey());
                } else {
                    this.store.put(row.getRowKey(), row);
                }
            }
        });
    }
}
