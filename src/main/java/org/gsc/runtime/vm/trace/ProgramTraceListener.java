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

package org.gsc.runtime.vm.trace;

import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.listener.ProgramListenerAdaptor;

public class ProgramTraceListener extends ProgramListenerAdaptor {

    private final boolean enabled;
    private OpActions actions = new OpActions();

    public ProgramTraceListener(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onMemoryExtend(int delta) {
        if (enabled) {
            actions.addMemoryExtend(delta);
        }
    }

    @Override
    public void onMemoryWrite(int address, byte[] data, int size) {
        if (enabled) {
            actions.addMemoryWrite(address, data, size);
        }
    }

    @Override
    public void onStackPop() {
        if (enabled) {
            actions.addStackPop();
        }
    }

    @Override
    public void onStackPush(DataWord value) {
        if (enabled) {
            actions.addStackPush(value);
        }
    }

    @Override
    public void onStackSwap(int from, int to) {
        if (enabled) {
            actions.addStackSwap(from, to);
        }
    }

    @Override
    public void onStoragePut(DataWord key, DataWord value) {
        if (enabled) {
            if (value.equals(DataWord.ZERO)) {
                actions.addStorageRemove(key);
            } else {
                actions.addStoragePut(key, value);
            }
        }
    }

    @Override
    public void onStorageClear() {
        if (enabled) {
            actions.addStorageClear();
        }
    }

    public OpActions resetActions() {
        OpActions actions = this.actions;
        this.actions = new OpActions();
        return actions;
    }
}
