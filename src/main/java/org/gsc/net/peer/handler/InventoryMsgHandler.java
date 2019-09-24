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

package org.gsc.net.peer.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.utils.Sha256Hash;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.peer.message.InventoryMessage;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.Item;
import org.gsc.net.peer.PeerConnection;
import org.gsc.net.service.AdvService;
import org.gsc.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class InventoryMsgHandler implements GSCMsgHandler {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    @Autowired
    private AdvService advService;

    @Autowired
    private TransactionsMsgHandler transactionsMsgHandler;

    private int maxCountIn10s = 10_000;

    @Override
    public void processMessage(PeerConnection peer, GSCMessage msg) {
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        InventoryType type = inventoryMessage.getInventoryType();

        if (!check(peer, inventoryMessage)) {
            return;
        }

        for (Sha256Hash id : inventoryMessage.getHashList()) {
            Item item = new Item(id, type);
            peer.getAdvInvReceive().put(item, System.currentTimeMillis());
            advService.addInv(item);
        }
    }

    private boolean check(PeerConnection peer, InventoryMessage inventoryMessage) {
        InventoryType type = inventoryMessage.getInventoryType();
        int size = inventoryMessage.getHashList().size();

        if (peer.isNeedSyncFromPeer() || peer.isNeedSyncFromUs()) {
            logger.warn("Drop inv: {} size: {} from Peer {}, syncFromUs: {}, syncFromPeer: {}.",
                    type, size, peer.getInetAddress(), peer.isNeedSyncFromUs(), peer.isNeedSyncFromPeer());
            return false;
        }

        if (type.equals(InventoryType.TRX)) {
            int count = peer.getNodeStatistics().messageStatistics.gscInTrxInventoryElement.getCount(10);
            if (count > maxCountIn10s) {
                logger.warn("Drop inv: {} size: {} from Peer {}, Inv count: {} is overload.",
                        type, size, peer.getInetAddress(), count);
                return false;
            }

            if (transactionsMsgHandler.isBusy()) {
                logger.warn("Drop inv: {} size: {} from Peer {}, transactionsMsgHandler is busy.",
                        type, size, peer.getInetAddress());
                return false;
            }
        }

        return true;
    }
}
