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



package org.gsc.net.dht;

import java.util.List;

public class DHTUtils {

    public static void printAllLeafs(Bucket root) {
        Bucket.SaveLeaf saveLeaf = new Bucket.SaveLeaf();
        root.traverseTree(saveLeaf);

        for (Bucket bucket : saveLeaf.getLeafs()) {
            System.out.println(bucket);
        }
    }

    public static List<Bucket> getAllLeafs(Bucket root) {
        Bucket.SaveLeaf saveLeaf = new Bucket.SaveLeaf();
        root.traverseTree(saveLeaf);

        return saveLeaf.getLeafs();
    }
}
