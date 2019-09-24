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

// Empty ListenerAdapter
public class ProgramListenerAdaptor implements ProgramListener {

    @Override
    public void onMemoryExtend(int delta) {
        // do nothing
    }

    @Override
    public void onMemoryWrite(int address, byte[] data, int size) {
        // do nothing
    }

    @Override
    public void onStackPop() {
        // do nothing
    }

    @Override
    public void onStackPush(DataWord value) {
        // do nothing
    }

    @Override
    public void onStackSwap(int from, int to) {
        // do nothing
    }

    @Override
    public void onStoragePut(DataWord key, DataWord value) {
        // do nothing
    }

    @Override
    public void onStorageClear() {
        // do nothing
    }
}
