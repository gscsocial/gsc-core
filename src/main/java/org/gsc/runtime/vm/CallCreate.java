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

package org.gsc.runtime.vm;

/**
 * @author Roman Mandeleil
 * @since 03.07.2014
 */
public class CallCreate {

    private final byte[] data;
    private final byte[] destination;
    private final byte[] cpuLimit;
    private final byte[] value;


    public CallCreate(byte[] data, byte[] destination, byte[] cpuLimit, byte[] value) {
        this.data = data;
        this.destination = destination;
        this.cpuLimit = cpuLimit;
        this.value = value;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getDestination() {
        return destination;
    }

    public byte[] getCpuLimit() {
        return cpuLimit;
    }

    public byte[] getValue() {
        return value;
    }
}
