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

package org.gsc.common.overlay.discover.node;

import static java.lang.Math.min;

import java.util.concurrent.atomic.AtomicLong;
import org.gsc.protos.Protocol.ReasonCode;

public class NodeStatistics {

  public final static int REPUTATION_PREDEFINED = 100000;
  public final static long TOO_MANY_PEERS_PENALIZE_TIMEOUT = 60 * 1000L;
  private static final long CLEAR_CYCLE_TIME = 60 * 60 * 1000L;

  public class StatHandler {

    private AtomicLong count = new AtomicLong(0);

    public void add() {
      count.incrementAndGet();
    }

    public void add(long delta) {
      count.addAndGet(delta);
    }

    public long get() {
      return count.get();
    }

    public String toString() {
      return count.toString();
    }
  }

  private boolean isPredefined = false;

  private int persistedReputation = 0;

  private int disconnectTimes = 0;

  // discovery stat
  public final StatHandler discoverOutPing = new StatHandler();
  public final StatHandler discoverInPong = new StatHandler();
  public final StatHandler discoverOutPong = new StatHandler();
  public final StatHandler discoverInPing = new StatHandler();
  public final StatHandler discoverInFind = new StatHandler();
  public final StatHandler discoverOutFind = new StatHandler();
  public final StatHandler discoverInNeighbours = new StatHandler();
  public final StatHandler discoverOutNeighbours = new StatHandler();
  public final SimpleStatter discoverMessageLatency;
  public final AtomicLong lastPongReplyTime = new AtomicLong(0l); // in milliseconds

  //  stat
  public final StatHandler p2pOutHello = new StatHandler();
  public final StatHandler p2pInHello = new StatHandler();
  public final StatHandler p2pHandShake = new StatHandler();
  public final StatHandler gscOutMessage = new StatHandler();
  public final StatHandler gscInMessage = new StatHandler();

  private ReasonCode gscLastRemoteDisconnectReason = null;
  private ReasonCode gscLastLocalDisconnectReason = null;
  private long lastDisconnectedTime = 0;
  private long firstDisconnectedTime = 0;


  public NodeStatistics(Node node) {
    discoverMessageLatency = new SimpleStatter(node.getIdString());
  }

  private int getSessionFairReputation() {
    int discoverReput = 0;

    discoverReput +=
        min(discoverInPong.get(), 1) * (discoverOutPing.get() == discoverInPong.get() ? 50 : 1);

    discoverReput +=
        min(discoverInNeighbours.get(), 1) * (discoverOutFind.get() == discoverInNeighbours.get() ? 50 : 1);

    discoverReput += (int)discoverMessageLatency.getAvrg() == 0 ? 0 : 1000 / discoverMessageLatency.getAvrg();

    int reput = 0;
    reput += p2pHandShake.get() > 0 ? 20 : 0;
    reput += min(gscInMessage.get(), 10) * 3;

    if (wasDisconnected()) {
      if (gscLastLocalDisconnectReason == null && gscLastRemoteDisconnectReason == null) {
        // means connection was dropped without reporting any reason - bad
        reput *= 0.3;
      } else if (gscLastLocalDisconnectReason != ReasonCode.REQUESTED) {
        // the disconnect was not initiated by discover mode
        if (gscLastRemoteDisconnectReason == ReasonCode.TOO_MANY_PEERS) {
          // The peer is popular, but we were unlucky
          reput *= 0.3;
        } else if (gscLastRemoteDisconnectReason != ReasonCode.REQUESTED) {
          // other disconnect reasons
          reput *= 0.2;
        }
      }
    }
    if (disconnectTimes > 20) {
      return 0;
    }
    int score =
        discoverReput + 10 * reput - (int) Math.pow(2, disconnectTimes) * (disconnectTimes > 0 ? 10
            : 0);
    return score > 0 ? score : 0;
  }

  public int getReputation() {
    int score = 0;
    if (!isReputationPenalized()){
      score += persistedReputation / 2 + getSessionFairReputation();
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

    if (gscLastLocalDisconnectReason == ReasonCode.NULL_IDENTITY ||
        gscLastRemoteDisconnectReason == ReasonCode.NULL_IDENTITY ||
        gscLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL ||
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
        gscLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION ||
        gscLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION ||
        gscLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN ||
        gscLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN ||
        gscLastRemoteDisconnectReason == ReasonCode.SYNC_FAIL ||
        gscLastLocalDisconnectReason == ReasonCode.SYNC_FAIL) {
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
    disconnectTimes++;
    persistedReputation = persistedReputation / 2;
  }

  public boolean wasDisconnected() {
    return lastDisconnectedTime > 0;
  }

  public void setPredefined(boolean isPredefined) {
    this.isPredefined = isPredefined;
  }

  public void setPersistedReputation(int persistedReputation) {
    this.persistedReputation = persistedReputation;
  }

  @Override
  public String toString() {
    return "NodeStat[reput: " + getReputation() + "(" + persistedReputation + "), discover: " +
        discoverInPong + "/" + discoverOutPing + " " +
        discoverOutPong + "/" + discoverInPing + " " +
        discoverInNeighbours + "/" + discoverOutFind + " " +
        discoverOutNeighbours + "/" + discoverInFind + " " +
        ((int) discoverMessageLatency.getAvrg()) + "ms" +
        ", p2p: " + p2pHandShake + "/" + p2pInHello + "/" + p2pOutHello + " " +
        ", gsc: " + gscInMessage + "/" + gscOutMessage + " " +
        (wasDisconnected() ? "X " + disconnectTimes : "") +
        (gscLastLocalDisconnectReason != null ? ("<=" + gscLastLocalDisconnectReason) : " ") +
        (gscLastRemoteDisconnectReason != null ? ("=>" + gscLastRemoteDisconnectReason) : " ");
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

}
