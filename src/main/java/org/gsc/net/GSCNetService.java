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

package org.gsc.net;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.peer.p2p.Message;
import org.gsc.net.server.ChannelManager;
import org.gsc.db.Manager;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.handler.BlockMsgHandler;
import org.gsc.net.peer.handler.ChainInventoryMsgHandler;
import org.gsc.net.peer.handler.FetchInvDataMsgHandler;
import org.gsc.net.peer.handler.InventoryMsgHandler;
import org.gsc.net.peer.handler.SyncBlockChainMsgHandler;
import org.gsc.net.peer.handler.TransactionsMsgHandler;
import org.gsc.net.peer.PeerConnection;
import org.gsc.net.peer.PeerStatusCheck;
import org.gsc.net.service.AdvService;
import org.gsc.net.service.SyncService;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class GSCNetService {

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private AdvService advService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private PeerStatusCheck peerStatusCheck;

    @Autowired
    private SyncBlockChainMsgHandler syncBlockChainMsgHandler;

    @Autowired
    private ChainInventoryMsgHandler chainInventoryMsgHandler;

    @Autowired
    private InventoryMsgHandler inventoryMsgHandler;

    @Autowired
    private FetchInvDataMsgHandler fetchInvDataMsgHandler;

    @Autowired
    private BlockMsgHandler blockMsgHandler;

    @Autowired
    private TransactionsMsgHandler transactionsMsgHandler;

    @Autowired
    private Manager manager;

    public void start() {
        manager.setGscNetService(this);
        channelManager.init();
        advService.init();
        syncService.init();
        peerStatusCheck.init();
        transactionsMsgHandler.init();
        logger.info("gscNetService start successfully.");
    }

    public void close() {
        channelManager.close();
        advService.close();
        syncService.close();
        peerStatusCheck.close();
        transactionsMsgHandler.close();
        logger.info("gscNetService closed successfully.");
    }

    public void fastForward(BlockMessage msg) {
        advService.fastForward(msg);
    }

    public void broadcast(Message msg) {
        advService.broadcast(msg);
    }

    protected void onMessage(PeerConnection peer, GSCMessage msg) {
        try {
            switch (msg.getType()) {
                case SYNC_BLOCK_CHAIN:
                    syncBlockChainMsgHandler.processMessage(peer, msg);
                    break;
                case BLOCK_CHAIN_INVENTORY:
                    chainInventoryMsgHandler.processMessage(peer, msg);
                    break;
                case INVENTORY:
                    inventoryMsgHandler.processMessage(peer, msg);
                    break;
                case FETCH_INV_DATA:
                    fetchInvDataMsgHandler.processMessage(peer, msg);
                    break;
                case BLOCK:
                    blockMsgHandler.processMessage(peer, msg);
                    break;
                case TRXS:
                    transactionsMsgHandler.processMessage(peer, msg);
                    break;
                default:
                    throw new P2pException(TypeEnum.NO_SUCH_MESSAGE, msg.getType().toString());
            }
        } catch (Exception e) {
            processException(peer, msg, e);
        }
    }

    private void processException(PeerConnection peer, GSCMessage msg, Exception ex) {
        ReasonCode code;

        if (ex instanceof P2pException) {
            TypeEnum type = ((P2pException) ex).getType();
            switch (type) {
                case BAD_TRX:
                    code = ReasonCode.BAD_TX;
                    break;
                case BAD_BLOCK:
                    code = ReasonCode.BAD_BLOCK;
                    break;
                case NO_SUCH_MESSAGE:
                case MESSAGE_WITH_WRONG_LENGTH:
                case BAD_MESSAGE:
                    code = ReasonCode.BAD_PROTOCOL;
                    break;
                case SYNC_FAILED:
                    code = ReasonCode.SYNC_FAIL;
                    break;
                case UNLINK_BLOCK:
                    code = ReasonCode.UNLINKABLE;
                    break;
                default:
                    code = ReasonCode.UNKNOWN;
                    break;
            }
            logger.error("Message from {} process failed, {} \n type: {}, detail: {}.",
                    peer.getInetAddress(), msg, type, ex.getMessage());
        } else {
            code = ReasonCode.UNKNOWN;
            logger.error("Message from {} process failed, {}",
                    peer.getInetAddress(), msg, ex);
        }

        peer.disconnect(code);
    }
}
