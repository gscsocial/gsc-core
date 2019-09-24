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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.Parameter.NodeConstant;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.peer.message.ChainInventoryMessage;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.PeerConnection;
import org.gsc.net.service.SyncService;

@Slf4j(topic = "net")
@Component
public class ChainInventoryMsgHandler implements GSCMsgHandler {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    @Autowired
    private SyncService syncService;

    @Override
    public void processMessage(PeerConnection peer, GSCMessage msg) throws P2pException {

        ChainInventoryMessage chainInventoryMessage = (ChainInventoryMessage) msg;

        check(peer, chainInventoryMessage);

        peer.setNeedSyncFromPeer(true);

        peer.setSyncChainRequested(null); //todo thread sec

        Deque<BlockId> blockIdWeGet = new LinkedList<>(chainInventoryMessage.getBlockIds());

        if (blockIdWeGet.size() == 1 && gscNetDelegate.containBlock(blockIdWeGet.peek())) {
            peer.setNeedSyncFromPeer(false);
            return;
        }

        while (!peer.getSyncBlockToFetch().isEmpty()) {
            if (peer.getSyncBlockToFetch().peekLast().equals(blockIdWeGet.peekFirst())) {
                break;
            }
            peer.getSyncBlockToFetch().pollLast();
        }

        blockIdWeGet.poll();

        peer.setRemainNum(chainInventoryMessage.getRemainNum());
        peer.getSyncBlockToFetch().addAll(blockIdWeGet);

        synchronized (gscNetDelegate.getBlockLock()) {
            while (!peer.getSyncBlockToFetch().isEmpty() && gscNetDelegate
                    .containBlock(peer.getSyncBlockToFetch().peek())) {
                BlockId blockId = peer.getSyncBlockToFetch().pop();
                peer.setBlockBothHave(blockId);
                logger.info("Block {} from {} is processed", blockId.getString(), peer.getNode().getHost());
            }
        }

        //if (chainInventoryMessage.getRemainNum() == 0 && peer.getSyncBlockToFetch().isEmpty()) {
        //  peer.setNeedSyncFromPeer(false);
        //}

        if ((chainInventoryMessage.getRemainNum() == 0 && !peer.getSyncBlockToFetch().isEmpty()) ||
                (chainInventoryMessage.getRemainNum() != 0
                        && peer.getSyncBlockToFetch().size() > NodeConstant.SYNC_FETCH_BATCH_NUM)) {
            syncService.setFetchFlag(true);
        } else {
            syncService.syncNext(peer);
        }
    }

    private void check(PeerConnection peer, ChainInventoryMessage msg) throws P2pException {
        if (peer.getSyncChainRequested() == null) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "not send syncBlockChainMsg");
        }

        List<BlockId> blockIds = msg.getBlockIds();
        if (CollectionUtils.isEmpty(blockIds)) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "blockIds is empty");
        }

        if (blockIds.size() > NodeConstant.SYNC_FETCH_BATCH_NUM + 1) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "big blockIds size: " + blockIds.size());
        }

        if (msg.getRemainNum() != 0 && blockIds.size() < NodeConstant.SYNC_FETCH_BATCH_NUM) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                    "remain: " + msg.getRemainNum() + ", blockIds size: " + blockIds.size());
        }

        long num = blockIds.get(0).getNum();
        for (BlockId id : msg.getBlockIds()) {
            if (id.getNum() != num++) {
                throw new P2pException(TypeEnum.BAD_MESSAGE, "not continuous block");
            }
        }

        if (!peer.getSyncChainRequested().getKey().contains(blockIds.get(0))) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "unlinked block, my head: "
                    + peer.getSyncChainRequested().getKey().getLast().getString()
                    + ", peer: " + blockIds.get(0).getString());
        }

        if (gscNetDelegate.getHeadBlockId().getNum() > 0) {
            long maxRemainTime =
                    ChainConstant.CLOCK_MAX_DELAY + System.currentTimeMillis() - gscNetDelegate
                            .getBlockTime(gscNetDelegate.getSolidBlockId());
            long maxFutureNum =
                    maxRemainTime / BLOCK_PRODUCED_INTERVAL + gscNetDelegate.getSolidBlockId().getNum();
            long lastNum = blockIds.get(blockIds.size() - 1).getNum();
            if (lastNum + msg.getRemainNum() > maxFutureNum) {
                throw new P2pException(TypeEnum.BAD_MESSAGE, "lastNum: " + lastNum + " + remainNum: "
                        + msg.getRemainNum() + " > futureMaxNum: " + maxFutureNum);
            }
        }
    }

}
