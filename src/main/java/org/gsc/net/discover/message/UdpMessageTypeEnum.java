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

package org.gsc.net.discover.message;

import java.util.HashMap;
import java.util.Map;

public enum UdpMessageTypeEnum {

    DISCOVER_PING((byte) 0x01),

    DISCOVER_PONG((byte) 0x02),

    DISCOVER_FIND_NODE((byte) 0x03),

    DISCOVER_NEIGHBORS((byte) 0x04),

    BACKUP_KEEP_ALIVE((byte) 0x05),

    UNKNOWN((byte) 0xFF);

    private final byte type;

    private static final Map<Byte, UdpMessageTypeEnum> intToTypeMap = new HashMap<>();

    static {
        for (UdpMessageTypeEnum value : values()) {
            intToTypeMap.put(value.type, value);
        }
    }

    UdpMessageTypeEnum(byte type) {
        this.type = type;
    }

    public static UdpMessageTypeEnum fromByte(byte type) {
        UdpMessageTypeEnum typeEnum = intToTypeMap.get(type);
        return typeEnum == null ? UNKNOWN : typeEnum;
    }

    public byte getType() {
        return type;
    }
}
