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

import java.util.ArrayList;
import java.util.List;


public class CompositeProgramListener implements ProgramListener {

    private List<ProgramListener> listeners = new ArrayList<>();

    @Override
    public void onMemoryExtend(int delta) {
        for (ProgramListener listener : listeners) {
            listener.onMemoryExtend(delta);
        }
    }

    @Override
    public void onMemoryWrite(int address, byte[] data, int size) {
        for (ProgramListener listener : listeners) {
            listener.onMemoryWrite(address, data, size);
        }
    }

    @Override
    public void onStackPop() {
        for (ProgramListener listener : listeners) {
            listener.onStackPop();
        }
    }

    @Override
    public void onStackPush(DataWord value) {
        for (ProgramListener listener : listeners) {
            listener.onStackPush(value);
        }
    }

    @Override
    public void onStackSwap(int from, int to) {
        for (ProgramListener listener : listeners) {
            listener.onStackSwap(from, to);
        }
    }

    @Override
    public void onStoragePut(DataWord key, DataWord value) {
        for (ProgramListener listener : listeners) {
            listener.onStoragePut(key, value);
        }
    }

    @Override
    public void onStorageClear() {
        for (ProgramListener listener : listeners) {
            listener.onStorageClear();
        }
    }

    public void addListener(ProgramListener listener) {
        listeners.add(listener);
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }
}
