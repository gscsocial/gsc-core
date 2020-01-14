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

package org.gsc.net.service;

import static org.gsc.config.Parameter.NetConstants.MAX_BLOCK_FETCH_PER_PEER;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.util.Pair;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.server.Channel.GSCState;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.config.Parameter.NodeConstant;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.net.peer.message.FetchInvDataMessage;
import org.gsc.net.peer.message.SyncBlockChainMessage;
import org.gsc.net.peer.PeerConnection;
import org.gsc.protos.Protocol.Inventory.InventoryType;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class SyncService {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    private Map<BlockMessage, PeerConnection> blockWaitToProcess = new ConcurrentHashMap<>();

    private Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();

    private Cache<BlockId, Long> requestBlockIds = CacheBuilder.newBuilder().maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(10_000)
            .recordStats().build();

    private ScheduledExecutorService fetchExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledExecutorService blockHandleExecutor = Executors
            .newSingleThreadScheduledExecutor();

    private volatile boolean handleFlag = false;

    @Setter
    private volatile boolean fetchFlag = false;

    public void init() {
        fetchExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (fetchFlag) {
                    fetchFlag = false;
                    startFetchSyncBlock();
                }
            } catch (Throwable t) {
                logger.error("Fetch sync block error.", t);
            }
        }, 10, 1, TimeUnit.SECONDS);

        blockHandleExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (handleFlag) {
                    handleFlag = false;
                    handleSyncBlock();
                }
            } catch (Throwable t) {
                logger.error("Handle sync block error.", t);
            }
        }, 10, 1, TimeUnit.SECONDS);
    }

    public void close() {
        fetchExecutor.shutdown();
        blockHandleExecutor.shutdown();
    }

    public void startSync(PeerConnection peer) {
        peer.setGSCState(GSCState.SYNCING);
        peer.setNeedSyncFromPeer(true);
        peer.getSyncBlockToFetch().clear();
        peer.setRemainNum(0);
        peer.setBlockBothHave(gscNetDelegate.getGenesisBlockId());
        syncNext(peer);
    }

    public void syncNext(PeerConnection peer) {
        try {
            if (peer.getSyncChainRequested() != null) {
                logger.warn("Peer {} is in sync.", peer.getNode().getHost());
                return;
            }
            LinkedList<BlockId> chainSummary = getBlockChainSummary(peer);
            peer.setSyncChainRequested(new Pair<>(chainSummary, System.currentTimeMillis()));
            peer.sendMessage(new SyncBlockChainMessage(chainSummary));
        } catch (Exception e) {
            logger.error("Peer {} sync failed, reason: {}", peer.getInetAddress(), e.getMessage());
            peer.disconnect(ReasonCode.SYNC_FAIL);
        }
    }

    public void processBlock(PeerConnection peer, BlockMessage blockMessage) {
        synchronized (blockJustReceived) {
            blockJustReceived.put(blockMessage, peer);
        }
        handleFlag = true;
        if (peer.isIdle()) {
            if (peer.getRemainNum() > 0
                    && peer.getSyncBlockToFetch().size() <= NodeConstant.SYNC_FETCH_BATCH_NUM) {
                syncNext(peer);
            } else {
                fetchFlag = true;
            }
        }
    }

    public void onDisconnect(PeerConnection peer) {
        if (!peer.getSyncBlockRequested().isEmpty()) {
            peer.getSyncBlockRequested().keySet().forEach(blockId -> invalid(blockId));
        }
    }

    private void invalid(BlockId blockId) {
        requestBlockIds.invalidate(blockId);
        fetchFlag = true;
    }

    private LinkedList<BlockId> getBlockChainSummary(PeerConnection peer) throws Exception {

        BlockId beginBlockId = peer.getBlockBothHave();
        List<BlockId> blockIds = new ArrayList<>(peer.getSyncBlockToFetch());
        LinkedList<BlockId> forkList = new LinkedList<>();
        LinkedList<BlockId> summary = new LinkedList<>();
        long syncBeginNumber = gscNetDelegate.getSyncBeginNumber();
        long low = syncBeginNumber < 0 ? 0 : syncBeginNumber;
        long high;
        long highNoFork;

        if (beginBlockId.getNum() == 0) {
            highNoFork = high = gscNetDelegate.getHeadBlockId().getNum();
        } else {
            if (gscNetDelegate.containBlockInMainChain(beginBlockId)) {
                highNoFork = high = beginBlockId.getNum();
            } else {
                forkList = gscNetDelegate.getBlockChainHashesOnFork(beginBlockId);
                if (forkList.isEmpty()) {
                    throw new P2pException(TypeEnum.SYNC_FAILED,
                            "can't find blockId: " + beginBlockId.getString());
                }
                highNoFork = forkList.peekLast().getNum();
                forkList.pollLast();
                Collections.reverse(forkList);
                high = highNoFork + forkList.size();
            }
        }

        if (low > highNoFork) {
            throw new P2pException(TypeEnum.SYNC_FAILED, "low: " + low + " gt highNoFork: " + highNoFork);
        }

        long realHigh = high + blockIds.size();
        logger.info("Get block chain summary, low: {}, highNoFork: {}, high: {}, realHigh: {}",
                low, highNoFork, high, realHigh);

        while (low <= realHigh) {
            if (low <= highNoFork) {
                summary.offer(gscNetDelegate.getBlockIdByNum(low));
            } else if (low <= high) {
                summary.offer(forkList.get((int) (low - highNoFork - 1)));
            } else {
                summary.offer(blockIds.get((int) (low - high - 1)));
            }
            low += (realHigh - low + 2) / 2;
        }

        return summary;
    }

    private void startFetchSyncBlock() {
        HashMap<PeerConnection, List<BlockId>> send = new HashMap<>();

        gscNetDelegate.getActivePeer().stream()
                .filter(peer -> peer.isNeedSyncFromPeer() && peer.isIdle())
                .forEach(peer -> {
                    if (!send.containsKey(peer)) {
                        send.put(peer, new LinkedList<>());
                    }
                    for (BlockId blockId : peer.getSyncBlockToFetch()) {
                        if (requestBlockIds.getIfPresent(blockId) == null) {
                            requestBlockIds.put(blockId, System.currentTimeMillis());
                            peer.getSyncBlockRequested().put(blockId, System.currentTimeMillis());
                            send.get(peer).add(blockId);
                            if (send.get(peer).size() >= MAX_BLOCK_FETCH_PER_PEER) {
                                break;
                            }
                        }
                    }
                });

        send.forEach((peer, blockIds) -> {
            if (!blockIds.isEmpty()) {
                peer.sendMessage(new FetchInvDataMessage(new LinkedList<>(blockIds), InventoryType.BLOCK));
            }
        });
    }

    private synchronized void handleSyncBlock() {

        synchronized (blockJustReceived) {
            blockWaitToProcess.putAll(blockJustReceived);
            blockJustReceived.clear();
        }

        final boolean[] isProcessed = {true};

        while (isProcessed[0]) {

            isProcessed[0] = false;
            synchronized (gscNetDelegate.getBlockLock()) {
                blockWaitToProcess.forEach((msg, peerConnection) -> {
                    if (peerConnection.isDisconnect()) {
                        blockWaitToProcess.remove(msg);
                        invalid(msg.getBlockId());
                        return;
                    }
                    final boolean[] isFound = {false};
                    gscNetDelegate.getActivePeer().stream()
                            .filter(peer -> msg.getBlockId().equals(peer.getSyncBlockToFetch().peek()))
                            .forEach(peer -> {
                                peer.getSyncBlockToFetch().pop();
                                peer.getSyncBlockInProcess().add(msg.getBlockId());
                                isFound[0] = true;
                            });
                    if (isFound[0]) {
                        blockWaitToProcess.remove(msg);
                        isProcessed[0] = true;
                        processSyncBlock(msg.getBlockWrapper());
                    }
                });
            }
        }
    }

    private void processSyncBlock(BlockWrapper block) {
        boolean flag = true;
        BlockId blockId = block.getBlockId();
        try {
            gscNetDelegate.processBlock(block);
        } catch (Exception e) {
            logger.error("Process sync block {} failed.", blockId.getString(), e);
            flag = false;
        }
        for (PeerConnection peer : gscNetDelegate.getActivePeer()) {
            if (peer.getSyncBlockInProcess().remove(blockId)) {
                if (flag) {
                    peer.setBlockBothHave(blockId);
                    if (peer.getSyncBlockToFetch().isEmpty()) {
                        syncNext(peer);
                    }
                } else {
                    peer.disconnect(ReasonCode.BAD_BLOCK);
                }
            }
        }
    }

}
