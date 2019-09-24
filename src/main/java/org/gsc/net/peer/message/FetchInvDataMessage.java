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

package org.gsc.net.peer.message;

import java.util.List;

import org.gsc.utils.Sha256Hash;
import org.gsc.protos.Protocol.Inventory;
import org.gsc.protos.Protocol.Inventory.InventoryType;

public class FetchInvDataMessage extends InventoryMessage {


    public FetchInvDataMessage(byte[] packed) throws Exception {
        super(packed);
        this.type = MessageTypes.FETCH_INV_DATA.asByte();
    }

    public FetchInvDataMessage(Inventory inv) {
        super(inv);
        this.type = MessageTypes.FETCH_INV_DATA.asByte();
    }

    public FetchInvDataMessage(List<Sha256Hash> hashList, InventoryType type) {
        super(hashList, type);
        this.type = MessageTypes.FETCH_INV_DATA.asByte();
    }

}
