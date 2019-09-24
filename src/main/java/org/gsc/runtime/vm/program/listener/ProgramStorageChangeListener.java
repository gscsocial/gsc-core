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

package org.gsc.runtime.vm.program.listener;

import org.gsc.runtime.vm.DataWord;

import java.util.HashMap;
import java.util.Map;

public class ProgramStorageChangeListener extends ProgramListenerAdaptor {

    private Map<DataWord, DataWord> diff = new HashMap<>();

    @Override
    public void onStoragePut(DataWord key, DataWord value) {
        diff.put(key, value);
    }

    @Override
    public void onStorageClear() {
        // do nothing
    }

    public Map<DataWord, DataWord> getDiff() {
        return new HashMap<>(diff);
    }

    public void merge(Map<DataWord, DataWord> diff) {
        this.diff.putAll(diff);
    }
}
