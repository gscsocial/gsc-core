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

import java.util.ArrayList;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.gsc.net.node.Node;
import org.gsc.net.node.NodeManager;

@Slf4j(topic = "discover")
public class RefreshTask extends DiscoverTask {

    public RefreshTask(NodeManager nodeManager) {
        super(nodeManager);
    }

    @Override
    public void run() {
        discover(Node.getNodeId(), 0, new ArrayList<>());
    }
}
