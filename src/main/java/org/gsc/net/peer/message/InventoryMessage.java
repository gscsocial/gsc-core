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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.gsc.utils.Sha256Hash;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Inventory;
import org.gsc.protos.Protocol.Inventory.InventoryType;


public class InventoryMessage extends GSCMessage {

    protected Inventory inv;

    public InventoryMessage(byte[] data) throws Exception {
        super(data);
        this.type = MessageTypes.INVENTORY.asByte();
        this.inv = Protocol.Inventory.parseFrom(data);
    }

    public InventoryMessage(Inventory inv) {
        this.inv = inv;
        this.type = MessageTypes.INVENTORY.asByte();
        this.data = inv.toByteArray();
    }

    public InventoryMessage(List<Sha256Hash> hashList, InventoryType type) {
        Inventory.Builder invBuilder = Inventory.newBuilder();

        for (Sha256Hash hash :
                hashList) {
            invBuilder.addIds(hash.getByteString());
        }
        invBuilder.setType(type);
        inv = invBuilder.build();
        this.type = MessageTypes.INVENTORY.asByte();
        this.data = inv.toByteArray();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public Inventory getInventory() {
        return inv;
    }

    public MessageTypes getInvMessageType() {
        return getInventoryType().equals(InventoryType.BLOCK) ? MessageTypes.BLOCK : MessageTypes.TRX;

    }

    public InventoryType getInventoryType() {
        return inv.getType();
    }

    @Override
    public String toString() {
        Deque<Sha256Hash> hashes = new LinkedList<>(getHashList());
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString()).append("invType: ").append(getInvMessageType())
                .append(", size: ").append(hashes.size())
                .append(", First hash: ").append(hashes.peekFirst());
        if (hashes.size() > 1) {
            builder.append(", End hash: ").append(hashes.peekLast());
        }
        return builder.toString();
    }

    public List<Sha256Hash> getHashList() {
        return getInventory().getIdsList().stream()
                .map(hash -> Sha256Hash.wrap(hash.toByteArray()))
                .collect(Collectors.toList());
    }

}
