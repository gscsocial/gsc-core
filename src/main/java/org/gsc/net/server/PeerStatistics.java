package org.gsc.net.server;

public class PeerStatistics {

    private double avgLatency = 0;
    private long pingCount = 0;

    public void pong(long pingStamp) {
        long latency = System.currentTimeMillis() - pingStamp;
        avgLatency = ((avgLatency * pingCount) + latency) / ++pingCount;
    }

    public double getAvgLatency() {
        return avgLatency;
    }
}
