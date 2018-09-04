/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.gsc.common.overlay.discover.node.statistics;

import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.ReasonCode;

public class NodeStatistics {

  public final static int REPUTATION_PREDEFINED = 100000;
  public final static long TOO_MANY_PEERS_PENALIZE_TIMEOUT = 60 * 1000L;
  private static final long CLEAR_CYCLE_TIME = 60 * 60 * 1000L;
  private final long MIN_DATA_LENGTH = Args.getInstance().getReceiveTcpMinDataLength();

  private boolean isPredefined = false;
  private int persistedReputation = 0;
  @Getter
  private int disconnectTimes = 0;
  @Getter
  private ReasonCode gscLastRemoteDisconnectReason = null;
  @Getter
  private ReasonCode gscLastLocalDisconnectReason = null;
  private long lastDisconnectedTime = 0;
  private long firstDisconnectedTime = 0;

  public final MessageStatistics messageStatistics = new MessageStatistics();
  public final MessageCount p2pHandShake = new MessageCount();
  public final MessageCount tcpFlow = new MessageCount();

  public final SimpleStatter discoverMessageLatency;
  public final AtomicLong lastPongReplyTime = new AtomicLong(0l); // in milliseconds

  private Reputation reputation;

  public NodeStatistics(Node node) {
    discoverMessageLatency = new SimpleStatter(node.getIdString());
    reputation = new Reputation(this);
  }

  public int getReputation() {
    int score = 0;
    if (!isReputationPenalized()){
      score += persistedReputation / 5 + reputation.calculate();
    }
    if (isPredefined){
      score += REPUTATION_PREDEFINED;
    }
    return score;
  }

  public ReasonCode getDisconnectReason() {
    if (gscLastLocalDisconnectReason != null) {
      return gscLastLocalDisconnectReason;
    }
    if (gscLastRemoteDisconnectReason != null) {
      return gscLastRemoteDisconnectReason;
    }
    return ReasonCode.UNKNOWN;
  }

  public boolean isReputationPenalized() {

    if (wasDisconnected() && gscLastRemoteDisconnectReason == ReasonCode.TOO_MANY_PEERS &&
        System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (wasDisconnected() && gscLastRemoteDisconnectReason == ReasonCode.DUPLICATE_PEER &&
        System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (firstDisconnectedTime > 0
        && (System.currentTimeMillis() - firstDisconnectedTime) > CLEAR_CYCLE_TIME) {
      gscLastLocalDisconnectReason = null;
      gscLastRemoteDisconnectReason = null;
      disconnectTimes = 0;
      persistedReputation = 0;
      firstDisconnectedTime = 0;
    }

    if (gscLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL ||
        gscLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL ||
        gscLastLocalDisconnectReason == ReasonCode.BAD_PROTOCOL ||
        gscLastRemoteDisconnectReason == ReasonCode.BAD_PROTOCOL ||
        gscLastLocalDisconnectReason == ReasonCode.BAD_BLOCK ||
        gscLastRemoteDisconnectReason == ReasonCode.BAD_BLOCK ||
        gscLastLocalDisconnectReason == ReasonCode.BAD_TX ||
        gscLastRemoteDisconnectReason == ReasonCode.BAD_TX ||
        gscLastLocalDisconnectReason == ReasonCode.FORKED ||
        gscLastRemoteDisconnectReason == ReasonCode.FORKED ||
        gscLastLocalDisconnectReason == ReasonCode.UNLINKABLE ||
        gscLastRemoteDisconnectReason == ReasonCode.UNLINKABLE ||
        gscLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN ||
        gscLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN ||
        gscLastRemoteDisconnectReason == ReasonCode.SYNC_FAIL ||
        gscLastLocalDisconnectReason == ReasonCode.SYNC_FAIL ||
        gscLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION ||
        gscLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION) {
      persistedReputation = 0;
      return true;
    }
    return false;
  }

  public void nodeDisconnectedRemote(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    gscLastRemoteDisconnectReason = reason;
  }

  public void nodeDisconnectedLocal(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    gscLastLocalDisconnectReason = reason;
  }

  public void notifyDisconnect() {
    lastDisconnectedTime = System.currentTimeMillis();
    if (firstDisconnectedTime <= 0) {
      firstDisconnectedTime = lastDisconnectedTime;
    }
    if (gscLastLocalDisconnectReason == ReasonCode.RESET) {
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
    return "NodeStat[reput: " + getReputation() + "(" + persistedReputation + "), discover: " +
        messageStatistics.discoverInPong + "/" + messageStatistics.discoverOutPing + " " +
        messageStatistics.discoverOutPong + "/" + messageStatistics.discoverInPing + " " +
        messageStatistics.discoverInNeighbours + "/" + messageStatistics.discoverOutFindNode + " " +
        messageStatistics.discoverOutNeighbours + "/" + messageStatistics.discoverInFindNode + " " +
        ((int) discoverMessageLatency.getAvrg()) + "ms" +
        ", p2p: " + p2pHandShake + "/" + messageStatistics.p2pInHello + "/" + messageStatistics.p2pOutHello + " " +
        ", gsc: " + messageStatistics.gscInMessage + "/" + messageStatistics.gscOutMessage + " " +
        (wasDisconnected() ? "X " + disconnectTimes : "") +
        (gscLastLocalDisconnectReason != null ? ("<=" + gscLastLocalDisconnectReason) : " ") +
        (gscLastRemoteDisconnectReason != null ? ("=>" + gscLastRemoteDisconnectReason) : " ") +
        ", tcp flow: " + tcpFlow.getTotalCount();
  }

  public class SimpleStatter {

    private final String name;
    private volatile double last;
    private volatile double sum;
    private volatile int count;

    public SimpleStatter(String name) {
      this.name = name;
    }

    public void add(double value) {
      last = value;
      sum += value;
      count++;
    }

    public double getLast() {
      return last;
    }

    public int getCount() {
      return count;
    }

    public double getSum() {
      return sum;
    }

    public double getAvrg() {
      return count == 0 ? 0 : sum / count;
    }

    public String getName() {
      return name;
    }

  }

  public boolean nodeIsHaveDataTransfer() {
    return tcpFlow.getTotalCount() > MIN_DATA_LENGTH;
  }

  public void resetTcpFlow() {
    tcpFlow.reset();
  }

}
