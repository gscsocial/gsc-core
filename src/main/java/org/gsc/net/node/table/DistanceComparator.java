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

import java.util.Comparator;

/**
 * Created by kest on 5/26/15.
 */
public class DistanceComparator implements Comparator<NodeEntry> {

    private byte[] targetId;

    DistanceComparator(byte[] targetId) {
        this.targetId = targetId;
    }

    @Override
    public int compare(NodeEntry e1, NodeEntry e2) {
        int d1 = NodeEntry.distance(targetId, e1.getNode().getId());
        int d2 = NodeEntry.distance(targetId, e2.getNode().getId());

        if (d1 > d2) {
            return 1;
        } else if (d1 < d2) {
            return -1;
        } else {
            return 0;
        }
    }
}
