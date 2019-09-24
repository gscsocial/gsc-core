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

package org.gsc.net.peer;

import lombok.Getter;
import org.gsc.utils.Sha256Hash;
import org.gsc.protos.Protocol.Inventory.InventoryType;

@Getter
public class Item {

    @Getter
    private Sha256Hash hash;
    @Getter
    private InventoryType type;
    @Getter
    private long time;


    public Item(Sha256Hash hash, InventoryType type) {
        this.hash = hash;
        this.type = type;
        this.time = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Item item = (Item) o;
        return hash.equals(item.getHash()) &&
                type.equals(item.getType());
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    @Override
    public String toString() {
        return type + ":" + hash;
    }
}
