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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.peer.p2p.HelloMessage;
import org.gsc.net.peer.p2p.Message;
import org.gsc.net.server.Channel;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.config.Parameter.NodeConstant;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.service.AdvService;
import org.gsc.net.service.SyncService;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class PeerConnection extends Channel {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    @Autowired
    private AdvService advService;

    @Autowired
    private SyncService syncService;

    @Setter
    @Getter
    private HelloMessage helloMessage;

    private int invCacheSize = 100_000;

    @Setter
    @Getter
    private Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder().maximumSize(invCacheSize)
            .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

    @Setter
    @Getter
    private Cache<Item, Long> advInvReceive = CacheBuilder.newBuilder().maximumSize(invCacheSize)
            .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

    @Setter
    @Getter
    private Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();

    @Setter
    private BlockId fastForwardBlock;

    @Getter
    private BlockId blockBothHave = new BlockId();

    public void setBlockBothHave(BlockId blockId) {
        this.blockBothHave = blockId;
        this.blockBothHaveUpdateTime = System.currentTimeMillis();
    }

    @Getter
    private volatile long blockBothHaveUpdateTime = System.currentTimeMillis();

    @Setter
    @Getter
    private BlockId lastSyncBlockId;

    @Setter
    @Getter
    private volatile long remainNum;

    @Getter
    private Cache<Sha256Hash, Long> syncBlockIdCache = CacheBuilder.newBuilder()
            .maximumSize(2 * NodeConstant.SYNC_FETCH_BATCH_NUM).recordStats().build();

    @Setter
    @Getter
    private Deque<BlockId> syncBlockToFetch = new ConcurrentLinkedDeque<>();

    @Setter
    @Getter
    private Map<BlockId, Long> syncBlockRequested = new ConcurrentHashMap<>();

    @Setter
    @Getter
    private Pair<Deque<BlockId>, Long> syncChainRequested = null;

    @Setter
    @Getter
    private Set<BlockId> syncBlockInProcess = new HashSet<>();

    @Setter
    @Getter
    private volatile boolean needSyncFromPeer;

    @Setter
    @Getter
    private volatile boolean needSyncFromUs;

    public boolean isIdle() {
        return advInvRequest.isEmpty() && syncBlockRequested.isEmpty() && syncChainRequested == null;
    }

    public void sendMessage(Message message) {
        msgQueue.sendMessage(message);
    }

    public void onConnect() {
        if (getHelloMessage().getHeadBlockId().getNum() > gscNetDelegate.getHeadBlockId().getNum()) {
            setGSCState(GSCState.SYNCING);
            syncService.startSync(this);
        } else {
            setGSCState(GSCState.SYNC_COMPLETED);
        }
    }

    public void onDisconnect() {
        syncService.onDisconnect(this);
        advService.onDisconnect(this);
        advInvReceive.cleanUp();
        advInvSpread.cleanUp();
        advInvRequest.clear();
        syncBlockIdCache.cleanUp();
        syncBlockToFetch.clear();
        syncBlockRequested.clear();
        syncBlockInProcess.clear();
        syncBlockInProcess.clear();
    }

    public String log() {
        long now = System.currentTimeMillis();
        return String.format(
                "Peer %s [%8s]\n"
                        + "ping msg: count %d, max-average-min-last: %d %d %d %d\n"
                        + "connect time: %ds\n"
                        + "last know block num: %s\n"
                        + "needSyncFromPeer:%b\n"
                        + "needSyncFromUs:%b\n"
                        + "syncToFetchSize:%d\n"
                        + "syncToFetchSizePeekNum:%d\n"
                        + "syncBlockRequestedSize:%d\n"
                        + "remainNum:%d\n"
                        + "syncChainRequested:%d\n"
                        + "blockInProcess:%d\n",
                getNode().getHost() + ":" + getNode().getPort(),
                getNode().getHexIdShort(),

                getNodeStatistics().pingMessageLatency.getCount(),
                getNodeStatistics().pingMessageLatency.getMax(),
                getNodeStatistics().pingMessageLatency.getAvrg(),
                getNodeStatistics().pingMessageLatency.getMin(),
                getNodeStatistics().pingMessageLatency.getLast(),

                (now - getStartTime()) / 1000,
                fastForwardBlock != null ? fastForwardBlock.getNum() : blockBothHave.getNum(),
                isNeedSyncFromPeer(),
                isNeedSyncFromUs(),
                syncBlockToFetch.size(),
                syncBlockToFetch.size() > 0 ? syncBlockToFetch.peek().getNum() : -1,
                syncBlockRequested.size(),
                remainNum,
                syncChainRequested == null ? 0 : (now - syncChainRequested.getValue()) / 1000,
                syncBlockInProcess.size())
                + nodeStatistics.toString() + "\n";
    }

}
