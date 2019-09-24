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

import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.config.Parameter.NodeConstant;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.peer.message.ChainInventoryMessage;
import org.gsc.net.peer.message.SyncBlockChainMessage;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
public class SyncBlockChainMsgHandler implements GSCMsgHandler {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    @Override
    public void processMessage(PeerConnection peer, GSCMessage msg) throws P2pException {

        SyncBlockChainMessage syncBlockChainMessage = (SyncBlockChainMessage) msg;

        check(peer, syncBlockChainMessage);

        long remainNum = 0;

        List<BlockId> summaryChainIds = syncBlockChainMessage.getBlockIds();

        LinkedList<BlockId> blockIds = getLostBlockIds(summaryChainIds);

        if (blockIds.size() == 1) {
            peer.setNeedSyncFromUs(false);
        } else {
            peer.setNeedSyncFromUs(true);
            remainNum = gscNetDelegate.getHeadBlockId().getNum() - blockIds.peekLast().getNum();
        }

        peer.setLastSyncBlockId(blockIds.peekLast());
        peer.setRemainNum(remainNum);
        peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));
    }

    private void check(PeerConnection peer, SyncBlockChainMessage msg) throws P2pException {
        List<BlockId> blockIds = msg.getBlockIds();
        if (CollectionUtils.isEmpty(blockIds)) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "SyncBlockChain blockIds is empty");
        }

        BlockId firstId = blockIds.get(0);
        if (!gscNetDelegate.containBlockInMainChain(firstId)) {
            throw new P2pException(TypeEnum.BAD_MESSAGE, "No first block:" + firstId.getString());
        }

        long headNum = gscNetDelegate.getHeadBlockId().getNum();
        if (firstId.getNum() > headNum) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                    "First blockNum:" + firstId.getNum() + " gt my head BlockNum:" + headNum);
        }

        BlockId lastSyncBlockId = peer.getLastSyncBlockId();
        long lastNum = blockIds.get(blockIds.size() - 1).getNum();
        if (lastSyncBlockId != null && lastSyncBlockId.getNum() > lastNum) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                    "lastSyncNum:" + lastSyncBlockId.getNum() + " gt lastNum:" + lastNum);
        }
    }

    private LinkedList<BlockId> getLostBlockIds(List<BlockId> blockIds) throws P2pException {

        BlockId unForkId = null;
        for (int i = blockIds.size() - 1; i >= 0; i--) {
            if (gscNetDelegate.containBlockInMainChain(blockIds.get(i))) {
                unForkId = blockIds.get(i);
                break;
            }
        }

        if (unForkId == null) {
            throw new P2pException(TypeEnum.SYNC_FAILED, "unForkId is null");
        }

        long len = Math.min(gscNetDelegate.getHeadBlockId().getNum(),
                unForkId.getNum() + NodeConstant.SYNC_FETCH_BATCH_NUM);

        LinkedList<BlockId> ids = new LinkedList<>();
        for (long i = unForkId.getNum(); i <= len; i++) {
            BlockId id = gscNetDelegate.getBlockIdByNum(i);
            ids.add(id);
        }
        return ids;
    }

}
