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

import org.gsc.net.node.NodeHandler;
import org.gsc.net.node.NodeManager;

/**
 * Allows to handle discovered nodes state changes Created by Anton Nashatyrev on 21.07.2015.
 */
public interface DiscoverListener {

    /**
     * Invoked whenever a new node appeared which meets criteria specified in the {@link
     * NodeManager#addDiscoverListener} method
     */
    void nodeAppeared(NodeHandler handler);

    /**
     * Invoked whenever a node stops meeting criteria.
     */
    void nodeDisappeared(NodeHandler handler);

    class Adapter implements DiscoverListener {

        public void nodeAppeared(NodeHandler handler) {
        }

        public void nodeDisappeared(NodeHandler handler) {
        }
    }
}
