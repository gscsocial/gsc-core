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

package org.gsc.utils;

import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

@Slf4j(topic = "utils")
public class TypeConversion {

    public static byte[] longToBytes(long x) {
        return Longs.toByteArray(x);
    }

    public static long bytesToLong(byte[] bytes) {
        return Longs.fromByteArray(bytes);
    }

    public static String bytesToHexString(byte[] src) {
        return Hex.encodeHexString(src);
    }

    public static byte[] hexStringToBytes(String hexString) {
        try {
            return Hex.decodeHex(hexString);
        } catch (DecoderException e) {
            logger.debug(e.getMessage(), e);
            return null;
        }
    }

    public static boolean increment(byte[] bytes) {
        final int startIndex = 0;
        int i;
        for (i = bytes.length - 1; i >= startIndex; i--) {
            bytes[i]++;
            if (bytes[i] != 0) {
                break;
            }
        }

        return (i >= startIndex || bytes[startIndex] != 0);
    }
}
