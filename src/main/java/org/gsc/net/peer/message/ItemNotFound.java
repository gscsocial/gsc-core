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

import org.gsc.protos.Protocol;

public class ItemNotFound extends GSCMessage {

    private org.gsc.protos.Protocol.Items notFound;

    /**
     * means can not find this block or trx.
     */
    public ItemNotFound() {
        Protocol.Items.Builder itemsBuilder = Protocol.Items.newBuilder();
        itemsBuilder.setType(Protocol.Items.ItemType.ERR);
        notFound = itemsBuilder.build();
        this.type = MessageTypes.ITEM_NOT_FOUND.asByte();
        this.data = notFound.toByteArray();
    }

    @Override
    public String toString() {
        return "item not found";
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

}
