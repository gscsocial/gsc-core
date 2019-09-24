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

package org.gsc.net.node.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kest on 5/25/15.
 */
public class NodeBucket {

    private final int depth;
    private List<NodeEntry> nodes = new ArrayList<>();

    NodeBucket(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public synchronized NodeEntry addNode(NodeEntry e) {
        if (!nodes.contains(e)) {
            if (nodes.size() >= KademliaOptions.BUCKET_SIZE) {
                return getLastSeen();
            } else {
                nodes.add(e);
            }
        }

        return null;
    }

    private NodeEntry getLastSeen() {
        List<NodeEntry> sorted = nodes;
        Collections.sort(sorted, new TimeComparator());
        return sorted.get(0);
    }

    public synchronized void dropNode(NodeEntry entry) {
        for (NodeEntry e : nodes) {
            if (e.getId().equals(entry.getId())) {
                nodes.remove(e);
                break;
            }
        }
    }

    public int getNodesCount() {
        return nodes.size();
    }

    public List<NodeEntry> getNodes() {
//        List<NodeEntry> nodes = new ArrayList<>();
//        for (NodeEntry e : this.nodes) {
//            nodes.add(e);
//        }
        return nodes;
    }
}
