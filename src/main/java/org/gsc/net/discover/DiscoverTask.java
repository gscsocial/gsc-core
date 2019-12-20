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
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.gsc.net.node.Node;
import org.gsc.net.node.NodeManager;
import org.gsc.net.node.table.KademliaOptions;
import org.gsc.net.node.table.NodeEntry;

@Slf4j(topic = "discover")
public class DiscoverTask implements Runnable {

    private NodeManager nodeManager;

    private byte[] nodeId;

    public DiscoverTask(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.nodeId = nodeManager.getPublicHomeNode().getId();
    }

    @Override
    public void run() {
        discover(nodeId, 0, new ArrayList<>());
    }

    public synchronized void discover(byte[] nodeId, int round, List<Node> prevTried) {

        try {
            if (round == KademliaOptions.MAX_STEPS) {
                logger.debug("Node table contains [{}] peers", nodeManager.getTable().getNodesCount());
                logger.debug("{}", String
                        .format("(KademliaOptions.MAX_STEPS) Terminating discover after %d rounds.", round));
                logger.trace("{}\n{}",
                        String.format("Nodes discovered %d ", nodeManager.getTable().getNodesCount()),
                        dumpNodes());
                return;
            }

            List<Node> closest = nodeManager.getTable().getClosestNodes(nodeId);
            List<Node> tried = new ArrayList<>();
            for (Node n : closest) {
                if (!tried.contains(n) && !prevTried.contains(n)) {
                    try {
                        nodeManager.getNodeHandler(n).sendFindNode(nodeId);
                        tried.add(n);
                        wait(50);
                    } catch (Exception ex) {
                        logger.error("Unexpected Exception " + ex, ex);
                    }
                }
                if (tried.size() == KademliaOptions.ALPHA) {
                    break;
                }
            }

            if (tried.isEmpty()) {
                logger.debug("{}",
                        String.format("(tried.isEmpty()) Terminating discover after %d rounds.", round));
                logger.trace("{}\n{}",
                        String.format("Nodes discovered %d ", nodeManager.getTable().getNodesCount()),
                        dumpNodes());
                return;
            }
            tried.addAll(prevTried);

            discover(nodeId, round + 1, tried);
        } catch (Exception ex) {
            logger.error("{}", ex);
        }
    }

    private String dumpNodes() {
        String ret = "";
        for (NodeEntry entry : nodeManager.getTable().getAllNodes()) {
            ret += "    " + entry.getNode() + "\n";
        }
        return ret;
    }
}
