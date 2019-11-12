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

package org.gsc.db;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.gsc.net.node.Node;

@Component
public class PeersStore extends GSCDatabase<Set<Node>> {

    @Autowired
    public PeersStore(ApplicationContext ctx) {
        super("peer");
    }

    @Override
    public void put(byte[] key, Set<Node> nodes) {
        StringBuilder sb = new StringBuilder();
        nodes.forEach(node -> sb.append(node.getEnodeURL()).append("&").append(node.getReputation())
                .append("||"));
        dbSource.putData(key, sb.toString().getBytes());
    }

    @Override
    public void delete(byte[] key) {
        dbSource.deleteData(key);
    }

    @Override
    public Set<Node> get(byte[] key) {
        Set<Node> nodes = new HashSet<>();
        byte[] value = dbSource.getData(key);
        if (value != null) {
            StringTokenizer st = new StringTokenizer(new String(value), "||");
            while (st.hasMoreElements()) {
                String strN = st.nextToken();
                int ps = strN.indexOf("&");
                int rept;
                Node n;
                if (ps > 0) {
                    n = new Node(strN.substring(0, ps));
                    try {
                        rept = Integer.parseInt(strN.substring(ps + 1, strN.length()));
                    } catch (NumberFormatException e) {
                        rept = 0;
                    }
                } else {
                    n = new Node(strN);
                    rept = 0;
                }

                n.setReputation(rept);
                nodes.add(n);
            }
        }
        return nodes;
    }

    @Override
    public boolean has(byte[] key) {
        return dbSource.getData(key) != null;
    }
}
