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

package org.gsc.net.node.statistics;

import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.ReasonCode;

public class NodeStatistics {

    public static final int REPUTATION_PREDEFINED = 100000;
    public static final long TOO_MANY_PEERS_PENALIZE_TIMEOUT = 60 * 1000L;
    private static final long CLEAR_CYCLE_TIME = 60 * 60 * 1000L;
    private final long MIN_DATA_LENGTH = Args.getInstance().getReceiveTcpMinDataLength();

    private boolean isPredefined = false;
    private int persistedReputation = 0;
    @Getter
    private int disconnectTimes = 0;
    @Getter
    private ReasonCode GSCLastRemoteDisconnectReason = null;
    @Getter
    private ReasonCode GSCLastLocalDisconnectReason = null;
    private long lastDisconnectedTime = 0;
    private long firstDisconnectedTime = 0;

    public final MessageStatistics messageStatistics = new MessageStatistics();
    public final MessageCount p2pHandShake = new MessageCount();
    public final MessageCount tcpFlow = new MessageCount();

    public final SimpleStatter discoverMessageLatency;
    public final SimpleStatter pingMessageLatency;

    public final AtomicLong lastPongReplyTime = new AtomicLong(0L); // in milliseconds

    private Reputation reputation;

    public NodeStatistics() {
        discoverMessageLatency = new SimpleStatter();
        pingMessageLatency = new SimpleStatter();
        reputation = new Reputation(this);
    }

    public int getReputation() {
        int score = 0;
        if (!isReputationPenalized()) {
            score += persistedReputation / 5 + reputation.calculate();
        }
        if (isPredefined) {
            score += REPUTATION_PREDEFINED;
        }
        return score;
    }

    public ReasonCode getDisconnectReason() {
        if (GSCLastLocalDisconnectReason != null) {
            return GSCLastLocalDisconnectReason;
        }
        if (GSCLastRemoteDisconnectReason != null) {
            return GSCLastRemoteDisconnectReason;
        }
        return ReasonCode.UNKNOWN;
    }

    public boolean isReputationPenalized() {

        if (wasDisconnected() && GSCLastRemoteDisconnectReason == ReasonCode.TOO_MANY_PEERS
                && System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
            return true;
        }

        if (wasDisconnected() && GSCLastRemoteDisconnectReason == ReasonCode.DUPLICATE_PEER
                && System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
            return true;
        }

        if (firstDisconnectedTime > 0
                && (System.currentTimeMillis() - firstDisconnectedTime) > CLEAR_CYCLE_TIME) {
            GSCLastLocalDisconnectReason = null;
            GSCLastRemoteDisconnectReason = null;
            disconnectTimes = 0;
            persistedReputation = 0;
            firstDisconnectedTime = 0;
        }

        if (GSCLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL
                || GSCLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL
                || GSCLastLocalDisconnectReason == ReasonCode.BAD_PROTOCOL
                || GSCLastRemoteDisconnectReason == ReasonCode.BAD_PROTOCOL
                || GSCLastLocalDisconnectReason == ReasonCode.BAD_BLOCK
                || GSCLastRemoteDisconnectReason == ReasonCode.BAD_BLOCK
                || GSCLastLocalDisconnectReason == ReasonCode.BAD_TX
                || GSCLastRemoteDisconnectReason == ReasonCode.BAD_TX
                || GSCLastLocalDisconnectReason == ReasonCode.FORKED
                || GSCLastRemoteDisconnectReason == ReasonCode.FORKED
                || GSCLastLocalDisconnectReason == ReasonCode.UNLINKABLE
                || GSCLastRemoteDisconnectReason == ReasonCode.UNLINKABLE
                || GSCLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN
                || GSCLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN
                || GSCLastRemoteDisconnectReason == ReasonCode.SYNC_FAIL
                || GSCLastLocalDisconnectReason == ReasonCode.SYNC_FAIL
                || GSCLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION
                || GSCLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION) {
            persistedReputation = 0;
            return true;
        }
        return false;
    }

    public void nodeDisconnectedRemote(ReasonCode reason) {
        lastDisconnectedTime = System.currentTimeMillis();
        GSCLastRemoteDisconnectReason = reason;
    }

    public void nodeDisconnectedLocal(ReasonCode reason) {
        lastDisconnectedTime = System.currentTimeMillis();
        GSCLastLocalDisconnectReason = reason;
    }

    public void notifyDisconnect() {
        lastDisconnectedTime = System.currentTimeMillis();
        if (firstDisconnectedTime <= 0) {
            firstDisconnectedTime = lastDisconnectedTime;
        }
        if (GSCLastLocalDisconnectReason == ReasonCode.RESET) {
            return;
        }
        disconnectTimes++;
        persistedReputation = persistedReputation / 2;
    }

    public boolean wasDisconnected() {
        return lastDisconnectedTime > 0;
    }

    public void setPredefined(boolean isPredefined) {
        this.isPredefined = isPredefined;
    }

    public boolean isPredefined() {
        return isPredefined;
    }

    public void setPersistedReputation(int persistedReputation) {
        this.persistedReputation = persistedReputation;
    }

    @Override
    public String toString() {
        return "NodeStat[reput: " + getReputation() + "(" + persistedReputation + "), discover: "
                + messageStatistics.discoverInPong + "/" + messageStatistics.discoverOutPing + " "
                + messageStatistics.discoverOutPong + "/" + messageStatistics.discoverInPing + " "
                + messageStatistics.discoverInNeighbours + "/" + messageStatistics.discoverOutFindNode
                + " "
                + messageStatistics.discoverOutNeighbours + "/" + messageStatistics.discoverInFindNode
                + " "
                + ((int) discoverMessageLatency.getAvrg()) + "ms"
                + ", p2p: " + p2pHandShake + "/" + messageStatistics.p2pInHello + "/"
                + messageStatistics.p2pOutHello + " "
                + ", gsc: " + messageStatistics.gscInMessage + "/" + messageStatistics.gscOutMessage
                + " "
                + (wasDisconnected() ? "X " + disconnectTimes : "")
                + (GSCLastLocalDisconnectReason != null ? ("<=" + GSCLastLocalDisconnectReason) : " ")
                + (GSCLastRemoteDisconnectReason != null ? ("=>" + GSCLastRemoteDisconnectReason) : " ")
                + ", tcp flow: " + tcpFlow.getTotalCount();
    }

    public class SimpleStatter {
        private long sum;
        @Getter
        private long count;
        @Getter
        private long last;
        @Getter
        private long min;
        @Getter
        private long max;

        public void add(long value) {
            last = value;
            sum += value;
            min = min == 0 ? value : Math.min(min, value);
            max = Math.max(max, value);
            count++;
        }

        public long getAvrg() {
            return count == 0 ? 0 : sum / count;
        }

    }

    public boolean nodeIsHaveDataTransfer() {
        return tcpFlow.getTotalCount() > MIN_DATA_LENGTH;
    }

    public void resetTcpFlow() {
        tcpFlow.reset();
    }

}
