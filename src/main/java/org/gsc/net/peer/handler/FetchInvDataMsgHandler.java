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

import com.google.common.collect.Lists;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.node.statistics.MessageCount;
import org.gsc.net.peer.p2p.Message;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.Parameter.NodeConstant;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.net.peer.message.FetchInvDataMessage;
import org.gsc.net.peer.message.MessageTypes;
import org.gsc.net.peer.message.TransactionMessage;
import org.gsc.net.peer.message.TransactionsMessage;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.Item;
import org.gsc.net.peer.PeerConnection;
import org.gsc.net.service.AdvService;
import org.gsc.net.service.SyncService;
import org.gsc.protos.Protocol.Inventory.InventoryType;
import org.gsc.protos.Protocol.ReasonCode;
import org.gsc.protos.Protocol.Transaction;

@Slf4j(topic = "net")
@Component
public class FetchInvDataMsgHandler implements GSCMsgHandler {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    @Autowired
    private SyncService syncService;

    @Autowired
    private AdvService advService;

    private int MAX_SIZE = 1_000_000;

    @Override
    public void processMessage(PeerConnection peer, GSCMessage msg) throws P2pException {

        FetchInvDataMessage fetchInvDataMsg = (FetchInvDataMessage) msg;

        check(peer, fetchInvDataMsg);

        InventoryType type = fetchInvDataMsg.getInventoryType();
        List<Transaction> transactions = Lists.newArrayList();

        int size = 0;

        for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
            Item item = new Item(hash, type);
            Message message = advService.getMessage(item);
            if (message == null) {
                try {
                    message = gscNetDelegate.getData(hash, type);
                } catch (Exception e) {
                    logger.error("Fetch item {} failed. reason: {}", item, hash, e.getMessage());
                    peer.disconnect(ReasonCode.FETCH_FAIL);
                    return;
                }
            }

            if (type.equals(InventoryType.BLOCK)) {
                BlockId blockId = ((BlockMessage) message).getBlockWrapper().getBlockId();
                if (peer.getBlockBothHave().getNum() < blockId.getNum()) {
                    peer.setBlockBothHave(blockId);
                }
                peer.sendMessage(message);
            } else {
                transactions.add(((TransactionMessage) message).getTransactionWrapper().getInstance());
                size += ((TransactionMessage) message).getTransactionWrapper().getInstance()
                        .getSerializedSize();
                if (size > MAX_SIZE) {
                    peer.sendMessage(new TransactionsMessage(transactions));
                    transactions = Lists.newArrayList();
                    size = 0;
                }
            }
        }
        if (transactions.size() > 0) {
            peer.sendMessage(new TransactionsMessage(transactions));
        }
    }

    private void check(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) throws P2pException {
        MessageTypes type = fetchInvDataMsg.getInvMessageType();

        if (type == MessageTypes.TRX) {
            for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
                if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.TRX)) == null) {
                    throw new P2pException(TypeEnum.BAD_MESSAGE, "not spread inv: {}" + hash);
                }
            }
            int fetchCount = peer.getNodeStatistics().messageStatistics.gscInTrxFetchInvDataElement
                    .getCount(10);
            int maxCount = advService.getTrxCount().getCount(60);
            if (fetchCount > maxCount) {
                throw new P2pException(TypeEnum.BAD_MESSAGE,
                        "maxCount: " + maxCount + ", fetchCount: " + fetchCount);
            }
        } else {
            boolean isAdv = true;
            for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
                if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BLOCK)) == null) {
                    isAdv = false;
                    break;
                }
            }
            if (isAdv) {
                MessageCount gscOutAdvBlock = peer.getNodeStatistics().messageStatistics.gscOutAdvBlock;
                gscOutAdvBlock.add(fetchInvDataMsg.getHashList().size());
                int outBlockCountIn1min = gscOutAdvBlock.getCount(60);
                int producedBlockIn2min = 120_000 / ChainConstant.BLOCK_PRODUCED_INTERVAL;
                if (outBlockCountIn1min > producedBlockIn2min) {
                    throw new P2pException(TypeEnum.BAD_MESSAGE, "producedBlockIn2min: " + producedBlockIn2min
                            + ", outBlockCountIn1min: " + outBlockCountIn1min);
                }
            } else {
                if (!peer.isNeedSyncFromUs()) {
                    throw new P2pException(TypeEnum.BAD_MESSAGE, "no need sync");
                }
                for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
                    long blockNum = new BlockId(hash).getNum();
                    long minBlockNum =
                            peer.getLastSyncBlockId().getNum() - 2 * NodeConstant.SYNC_FETCH_BATCH_NUM;
                    if (blockNum < minBlockNum) {
                        throw new P2pException(TypeEnum.BAD_MESSAGE,
                                "minBlockNum: " + minBlockNum + ", blockNum: " + blockNum);
                    }
                    if (peer.getSyncBlockIdCache().getIfPresent(hash) != null) {
                        throw new P2pException(TypeEnum.BAD_MESSAGE,
                                new BlockId(hash).getString() + " is exist");
                    }
                    peer.getSyncBlockIdCache().put(hash, System.currentTimeMillis());
                }
            }
        }
    }

}
