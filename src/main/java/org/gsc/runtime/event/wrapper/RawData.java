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


package org.gsc.runtime.event.wrapper;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.vm.DataWord;

@Data
public class RawData {

    // for mongodb
    @Getter
    private String address;
    @Getter
    private List<DataWord> topics;
    @Getter
    private String data;

    public RawData(byte[] address, List<DataWord> topics, byte[] data) {
        this.address = (address != null) ? Hex.toHexString(address) : "";
        this.topics = (address != null) ? topics : new ArrayList<>();
        this.data = (data != null) ? Hex.toHexString(data) : "";
    }
}
