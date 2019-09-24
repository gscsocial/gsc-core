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

import java.math.BigInteger;

import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;
import org.gsc.utils.Utils;

public class Peer {

    byte[] id;
    String host = "127.0.0.1";
    int port = 0;

    public Peer(byte[] id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public Peer(byte[] ip) {
        this.id = ip;
    }

    public byte nextBit(String startPattern) {

        if (this.toBinaryString().startsWith(startPattern + "1")) {
            return 1;
        } else {
            return 0;
        }
    }

    public byte[] calcDistance(Peer toPeer) {

        BigInteger aaPeer = new BigInteger(getId());
        BigInteger bbPeer = new BigInteger(toPeer.getId());

        BigInteger distance = aaPeer.xor(bbPeer);
        return BigIntegers.asUnsignedByteArray(distance);
    }


    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String
                .format("Peer {\n id=%s, \n host=%s, \n port=%d\n}", Hex.toHexString(id), host, port);
    }

    public String toBinaryString() {

        BigInteger bi = new BigInteger(1, id);
        String out = String.format("%512s", bi.toString(2));
        out = out.replace(' ', '0');

        return out;
    }

    public static byte[] randomPeerId() {

        byte[] peerIdBytes = new BigInteger(512, Utils.getRandom()).toByteArray();

        final String peerId;
        if (peerIdBytes.length > 64) {
            peerId = Hex.toHexString(peerIdBytes, 1, 64);
        } else {
            peerId = Hex.toHexString(peerIdBytes);
        }

        return Hex.decode(peerId);
    }

}
