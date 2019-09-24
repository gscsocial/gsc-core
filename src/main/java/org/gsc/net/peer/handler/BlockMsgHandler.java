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

import static org.gsc.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.gsc.config.Parameter.ChainConstant.BLOCK_SIZE;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.config.args.Args;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.Item;
import org.gsc.net.peer.PeerConnection;
import org.gsc.net.service.AdvService;
import org.gsc.net.service.SyncService;
import org.gsc.services.WitnessProductBlockService;
import org.gsc.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class BlockMsgHandler implements GSCMsgHandler {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    @Autowired
    private AdvService advService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private WitnessProductBlockService witnessProductBlockService;

    private int maxBlockSize = BLOCK_SIZE + 1000;

    private boolean fastForward = Args.getInstance().isFastForward();

    @Override
    public void processMessage(PeerConnection peer, GSCMessage msg) throws P2pException {

        BlockMessage blockMessage = (BlockMessage) msg;
        BlockId blockId = blockMessage.getBlockId();

        if (!fastForward && !peer.isFastForwardPeer()) {
            check(peer, blockMessage);
        }

        if (peer.getSyncBlockRequested().containsKey(blockId)) {
            peer.getSyncBlockRequested().remove(blockId);
            syncService.processBlock(peer, blockMessage);
        } else {
            Long time = peer.getAdvInvRequest().remove(new Item(blockId, InventoryType.BLOCK));
            long now = System.currentTimeMillis();
            long interval = blockId.getNum() - gscNetDelegate.getHeadBlockId().getNum();
            processBlock(peer, blockMessage.getBlockWrapper());
            logger.info(
                    "Receive block/interval {}/{} from {} fetch/delay {}/{}ms, txs/process {}/{}ms, witness: {}",
                    blockId.getNum(),
                    interval,
                    peer.getInetAddress(),
                    time == null ? 0 : now - time,
                    now - blockMessage.getBlockWrapper().getTimeStamp(),
                    ((BlockMessage) msg).getBlockWrapper().getTransactions().size(),
                    System.currentTimeMillis() - now,
                    Hex.toHexString(blockMessage.getBlockWrapper().getWitnessAddress().toByteArray()));
        }
    }

    private void check(PeerConnection peer, BlockMessage msg) throws P2pException {
        Item item = new Item(msg.getBlockId(), InventoryType.BLOCK);
        if (!peer.getSyncBlockRequested().containsKey(msg.getBlockId()) && !peer.getAdvInvRequest()
                .containsKey(item)) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "no request");
        }
        BlockWrapper blockWrapper = msg.getBlockWrapper();
        if (blockWrapper.getInstance().getSerializedSize() > maxBlockSize) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "block size over limit");
        }
        long gap = blockWrapper.getTimeStamp() - System.currentTimeMillis();
        if (gap >= BLOCK_PRODUCED_INTERVAL) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "block time error");
        }
    }

    private void processBlock(PeerConnection peer, BlockWrapper block) throws P2pException {
        BlockId blockId = block.getBlockId();
        if (!gscNetDelegate.containBlock(block.getParentBlockId())) {
            logger.warn("Get unlink block {} from {}, head is {}.", blockId.getString(),
                    peer.getInetAddress(), gscNetDelegate.getHeadBlockId().getString());
            syncService.startSync(peer);
            return;
        }

        Item item = new Item(blockId, InventoryType.BLOCK);
        if (fastForward || peer.isFastForwardPeer()) {
            peer.getAdvInvReceive().put(item, System.currentTimeMillis());
            advService.addInvToCache(item);
        }

        if (fastForward) {
            if (block.getNum() < gscNetDelegate.getHeadBlockId().getNum()) {
                logger.warn("Receive a low block {}, head {}",
                        blockId.getString(), gscNetDelegate.getHeadBlockId().getString());
                return;
            }
            if (gscNetDelegate.validBlock(block)) {
                advService.fastForward(new BlockMessage(block));
                gscNetDelegate.trustNode(peer);
            }
        }

        gscNetDelegate.processBlock(block);
        witnessProductBlockService.validWitnessProductTwoBlock(block);
        gscNetDelegate.getActivePeer().forEach(p -> {
            if (p.getAdvInvReceive().getIfPresent(blockId) != null) {
                p.setBlockBothHave(blockId);
            }
        });

        if (!fastForward) {
            advService.broadcast(new BlockMessage(block));
        }
    }

}
