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


package org.gsc.core.wrapper.utils;

/**
 */
public class RLPItem implements RLPElement {

    private final byte[] rlpData;

    public RLPItem(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    public byte[] getRLPData() {
        if (rlpData.length == 0) {
            return null;
        }
        return rlpData;
    }
}
