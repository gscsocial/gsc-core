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

package org.gsc.net.peer.message;

import java.util.HashMap;
import java.util.Map;

public enum MessageTypes {

    FIRST(0x00),

    TRX(0x01),

    BLOCK(0x02),

    TRXS(0x03),

    BLOCKS(0x04),

    BLOCKHEADERS(0x05),

    INVENTORY(0x06),

    FETCH_INV_DATA(0x07),

    SYNC_BLOCK_CHAIN(0x08),

    BLOCK_CHAIN_INVENTORY(0x09),

    ITEM_NOT_FOUND(0x10),

    FETCH_BLOCK_HEADERS(0x11),

    BLOCK_INVENTORY(0x12),

    TRX_INVENTORY(0x13),

    P2P_HELLO(0x20),

    P2P_DISCONNECT(0x21),

    P2P_PING(0x22),

    P2P_PONG(0x23),

    DISCOVER_PING(0x30),

    DISCOVER_PONG(0x31),

    DISCOVER_FIND_PEER(0x32),

    DISCOVER_PEERS(0x33),

    LAST(0xFF);

    private final int type;

    private static final Map<Integer, MessageTypes> intToTypeMap = new HashMap<>();

    static {
        for (MessageTypes type : MessageTypes.values()) {
            intToTypeMap.put(type.type, type);
        }
    }

    private MessageTypes(int type) {
        this.type = type;
    }

    public static MessageTypes fromByte(byte i) {
        return intToTypeMap.get((int) i);
    }

    public static boolean inRange(byte code) {
        return code < LAST.asByte();
    }

    public byte asByte() {
        return (byte) (type);
    }

    public static boolean inP2pRange(byte code) {
        return code <= P2P_PONG.asByte() && code >= P2P_HELLO.asByte();
    }

    public static boolean inTRXRange(byte code) {
        return code <= TRX_INVENTORY.asByte() && code >= FIRST.asByte();
    }

    @Override
    public String toString() {
        switch (type) {
            case 1:
                return "TRX";
            case 2:
                return "BLOCK";
            case 6:
                return "INVENTORY";
            case 7:
                return "FETCH_INV_DATA";
            case 8:
                return "SYNC_BLOCK_CHAIN";
            case 11:
                return "BLOCK_INVENTORY";
            default:
                break;
        }
        return super.toString();
    }
}


