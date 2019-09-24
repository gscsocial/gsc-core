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

import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.List;

import org.gsc.protos.Protocol.ReasonCode;

public class Reputation {

    public abstract class Score<T> implements Comparable<Score> {

        protected T t;

        public Score(T t) {
            this.t = t;
        }

        abstract int calculate(int baseScore);

        public boolean isContinue() {
            return true;
        }

        public int getOrder() {
            return 0;
        }

        @Override
        public int compareTo(Score score) {
            if (getOrder() > score.getOrder()) {
                return 1;
            } else if (getOrder() < score.getOrder()) {
                return -1;
            }
            return 0;
        }
    }

    public class DiscoverScore extends Score<MessageStatistics> {

        public DiscoverScore(MessageStatistics messageStatistics) {
            super(messageStatistics);
        }

        @Override
        int calculate(int baseScore) {
            int discoverReput = baseScore;
            discoverReput +=
                    min(t.discoverInPong.getTotalCount(), 1) * (t.discoverOutPing.getTotalCount()
                            == t.discoverInPong.getTotalCount() ? 101 : 1);
            discoverReput +=
                    min(t.discoverInNeighbours.getTotalCount(), 1) * (t.discoverOutFindNode.getTotalCount()
                            == t.discoverInNeighbours.getTotalCount() ? 10 : 1);
            return discoverReput;
        }

        @Override
        public boolean isContinue() {
            return t.discoverOutPing.getTotalCount() == t.discoverInPong.getTotalCount()
                    && t.discoverInNeighbours.getTotalCount() <= t.discoverOutFindNode.getTotalCount();
        }
    }

    public class TcpScore extends Score<NodeStatistics> {

        public TcpScore(NodeStatistics nodeStatistics) {
            super(nodeStatistics);
        }

        @Override
        int calculate(int baseScore) {
            int reput = baseScore;
            reput += t.p2pHandShake.getTotalCount() > 0 ? 10 : 0;
            reput += min(t.tcpFlow.getTotalCount() / 10240, 20);
            reput += t.messageStatistics.p2pOutPing.getTotalCount() == t.messageStatistics.p2pInPong
                    .getTotalCount() ? 10 : 0;
            return reput;
        }
    }

    public class DisConnectScore extends Score<NodeStatistics> {

        public DisConnectScore(NodeStatistics nodeStatistics) {
            super(nodeStatistics);
        }

        @Override
        int calculate(int baseScore) {
            if (t.wasDisconnected()) {
                if (t.getGSCLastLocalDisconnectReason() == null
                        && t.getGSCLastRemoteDisconnectReason() == null) {
                    // means connection was dropped without reporting any reason - bad
                    baseScore *= 0.8;
                } else if (t.getGSCLastLocalDisconnectReason() != ReasonCode.REQUESTED) {
                    // the disconnect was not initiated by discover mode
                    if (t.getGSCLastRemoteDisconnectReason() == ReasonCode.TOO_MANY_PEERS
                            || t.getGSCLastLocalDisconnectReason() == ReasonCode.TOO_MANY_PEERS
                            || t.getGSCLastRemoteDisconnectReason() == ReasonCode.TOO_MANY_PEERS_WITH_SAME_IP
                            || t.getGSCLastLocalDisconnectReason() == ReasonCode.TOO_MANY_PEERS_WITH_SAME_IP
                            || t.getGSCLastRemoteDisconnectReason() == ReasonCode.DUPLICATE_PEER
                            || t.getGSCLastLocalDisconnectReason() == ReasonCode.DUPLICATE_PEER
                            || t.getGSCLastRemoteDisconnectReason() == ReasonCode.TIME_OUT
                            || t.getGSCLastLocalDisconnectReason() == ReasonCode.TIME_OUT
                            || t.getGSCLastRemoteDisconnectReason() == ReasonCode.PING_TIMEOUT
                            || t.getGSCLastLocalDisconnectReason() == ReasonCode.PING_TIMEOUT
                            || t.getGSCLastRemoteDisconnectReason() == ReasonCode.CONNECT_FAIL
                            || t.getGSCLastLocalDisconnectReason() == ReasonCode.CONNECT_FAIL) {
                        // The peer is popular, but we were unlucky
                        baseScore *= 0.9;
                    } else if (t.getGSCLastLocalDisconnectReason() == ReasonCode.RESET) {
                        baseScore *= 0.95;
                    } else if (t.getGSCLastRemoteDisconnectReason() != ReasonCode.REQUESTED) {
                        // other disconnect reasons
                        baseScore *= 0.7;
                    }
                }
            }
            if (t.getDisconnectTimes() > 20) {
                return 0;
            }
            int score = baseScore - (int) Math.pow(2, t.getDisconnectTimes())
                    * (t.getDisconnectTimes() > 0 ? 10 : 0);
            return score;
        }
    }

    public class OtherScore extends Score<NodeStatistics> {

        public OtherScore(NodeStatistics nodeStatistics) {
            super(nodeStatistics);
        }

        @Override
        int calculate(int baseScore) {
            baseScore += (int) t.discoverMessageLatency.getAvrg() == 0 ? 0
                    : min(1000 / t.discoverMessageLatency.getAvrg(), 20);
            return baseScore;
        }
    }

    private List<Score> scoreList = new ArrayList<>();

    public Reputation(NodeStatistics nodeStatistics) {
        Score<MessageStatistics> discoverScore = new DiscoverScore(nodeStatistics.messageStatistics);
        Score<NodeStatistics> otherScore = new OtherScore(nodeStatistics);
        Score<NodeStatistics> tcpScore = new TcpScore(nodeStatistics);
        Score<NodeStatistics> disconnectScore = new DisConnectScore(nodeStatistics);

        scoreList.add(discoverScore);
        scoreList.add(tcpScore);
        scoreList.add(otherScore);
        scoreList.add(disconnectScore);
    }

    public int calculate() {
        int scoreNumber = 0;
        for (Score score : scoreList) {
            scoreNumber = score.calculate(scoreNumber);
            if (!score.isContinue()) {
                break;
            }
        }
        return scoreNumber > 0 ? scoreNumber : 0;
    }

}
