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

package org.gsc.net.discover;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.gsc.net.node.NodeManager;
import org.gsc.net.node.table.KademliaOptions;

public class DiscoveryExecutor {

    ScheduledExecutorService discoverer = Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();

    NodeManager nodeManager;

    public DiscoveryExecutor(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void start() {
        discoverer.scheduleWithFixedDelay(
                new DiscoverTask(nodeManager),
                1, KademliaOptions.DISCOVER_CYCLE, TimeUnit.SECONDS);

        refresher.scheduleWithFixedDelay(
                new RefreshTask(nodeManager),
                1, KademliaOptions.BUCKET_REFRESH, TimeUnit.MILLISECONDS);
    }

    public void close() {
        discoverer.shutdownNow();
        refresher.shutdownNow();
    }
}
