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

package org.gsc.net.node;

import static org.gsc.crypto.Hash.sha3;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.crypto.Hash;

public class Node implements Serializable {

    private static final long serialVersionUID = -4267600517925770636L;

    private byte[] id;

    private String host;

    private int port;

    private boolean isFakeNodeId = false;

    public int getReputation() {
        return reputation;
    }

    public void setReputation(int reputation) {
        this.reputation = reputation;
    }

    private int reputation = 0;

    public static Node instanceOf(String addressOrEnode) {
        try {
            URI uri = new URI(addressOrEnode);
            if ("enode".equals(uri.getScheme())) {
                return new Node(addressOrEnode);
            }
        } catch (URISyntaxException e) {
            // continue
        }

        final ECKey generatedNodeKey = ECKey.fromPrivate(Hash.sha3(addressOrEnode.getBytes()));
        final String generatedNodeId = Hex.toHexString(generatedNodeKey.getNodeId());
        final Node node = new Node("enode://" + generatedNodeId + "@" + addressOrEnode);
        node.isFakeNodeId = true;
        return node;
    }

    public String getEnodeURL() {
        return new StringBuilder("enode://")
                .append(ByteArray.toHexString(id)).append("@")
                .append(host).append(":")
                .append(port).toString();
    }

    public Node(String enodeURL) {
        try {
            URI uri = new URI(enodeURL);
            if (!"enode".equals(uri.getScheme())) {
                throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");
            }
            this.id = Hex.decode(uri.getUserInfo());
            this.host = uri.getHost();
            this.port = uri.getPort();
        } catch (URISyntaxException e) {
            throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e);
        }
    }

    public Node(byte[] id, String host, int port) {
        if (id != null) {
            this.id = id.clone();
        }
        this.host = host;
        this.port = port;
    }

    public String getHexId() {
        return Hex.toHexString(id);
    }

    public String getHexIdShort() {
        return Utils.getIdShort(getHexId());
    }

    public boolean isDiscoveryNode() {
        return isFakeNodeId;
    }

    public byte[] getId() {
        return id == null ? id : id.clone();
    }

    public void setId(byte[] id) {
        this.id = id == null ? null : id.clone();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIdString() {
        if (id == null) {
            return null;
        }
        return new String(id);
    }

    @Override
    public String toString() {
        return "Node{" + " host='" + host + '\'' + ", port=" + port
                + ", id=" + ByteArray.toHexString(id) + '}';
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o.getClass() == getClass()) {
            return StringUtils.equals(getIdString(), ((Node) o).getIdString());
        }

        return false;
    }
}
