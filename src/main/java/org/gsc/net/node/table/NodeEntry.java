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

import org.gsc.net.node.Node;

/**
 * Created by kest on 5/25/15.
 */
public class NodeEntry {

    private byte[] ownerId;
    private Node node;
    private String entryId;
    private int distance;
    private long modified;

    public NodeEntry(Node n) {
        this.node = n;
        this.ownerId = n.getId();
        entryId = n.getHost();
        distance = distance(ownerId, n.getId());
        touch();
    }

    public NodeEntry(byte[] ownerId, Node n) {
        this.node = n;
        this.ownerId = ownerId;
        entryId = n.getHost();
        distance = distance(ownerId, n.getId());
        touch();
    }

    public void touch() {
        modified = System.currentTimeMillis();
    }

    public int getDistance() {
        return distance;
    }

    public String getId() {
        return entryId;
    }

    public Node getNode() {
        return node;
    }

    public long getModified() {
        return modified;
    }

    @Override
    public boolean equals(Object o) {
        boolean ret = false;

        if (o != null && this.getClass() == o.getClass()) {
            NodeEntry e = (NodeEntry) o;
            ret = this.getId().equals(e.getId());
        }

        return ret;
    }

    @Override
    public int hashCode() {
        return this.node.hashCode();
    }

    public static int distance(byte[] ownerId, byte[] targetId) {
        byte[] h1 = targetId;
        byte[] h2 = ownerId;

        byte[] hash = new byte[Math.min(h1.length, h2.length)];

        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) (((int) h1[i]) ^ ((int) h2[i]));
        }

        int d = KademliaOptions.BINS;

        for (byte b : hash) {
            if (b == 0) {
                d -= 8;
            } else {
                int count = 0;
                for (int i = 7; i >= 0; i--) {
                    boolean a = ((b & 0xff) & (1 << i)) == 0;
                    if (a) {
                        count++;
                    } else {
                        break;
                    }
                }

                d -= count;

                break;
            }
        }
        return d;
    }
}
