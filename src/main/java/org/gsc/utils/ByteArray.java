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

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

@Slf4j(topic = "utils")
public class ByteArray {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static String toHexString(byte[] data) {
        return data == null ? "" : Hex.toHexString(data);
    }

    /**
     * get bytes data from hex string data.
     */
    public static byte[] fromHexString(String data) {
        if (data == null) {
            return EMPTY_BYTE_ARRAY;
        }
        if (data.startsWith("0x")) {
            data = data.substring(2);
        }
        if (data.length() % 2 != 0) {
            data = "0" + data;
        }
        return Hex.decode(data);
    }

    /**
     * get long data from bytes data.
     */
    public static long toLong(byte[] b) {
        return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).longValue();
    }

    /**
     * get int data from bytes data.
     */
    public static int toInt(byte[] b) {
        return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).intValue();
    }

    /**
     * get bytes data from string data.
     */
    public static byte[] fromString(String s) {
        return StringUtils.isBlank(s) ? null : s.getBytes();
    }

    /**
     * get string data from bytes data.
     */
    public static String toStr(byte[] b) {
        return ArrayUtils.isEmpty(b) ? null : new String(b);
    }

    public static byte[] fromLong(long val) {
        return Longs.toByteArray(val);
    }

    public static byte[] fromInt(int val) {
        return Ints.toByteArray(val);
    }

    /**
     * get bytes data from object data.
     */
    public static byte[] fromObject(Object obj) {
        byte[] bytes = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            bytes = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            logger.error("objectToByteArray failed: " + e.getMessage(), e);
        }
        return bytes;
    }

    /**
     * Generate a subarray of a given byte array.
     *
     * @param input the input byte array
     * @param start the start index
     * @param end   the end index
     * @return a subarray of <tt>input</tt>, ranging from <tt>start</tt> (inclusively) to <tt>end</tt>
     * (exclusively)
     */
    public static byte[] subArray(byte[] input, int start, int end) {
        byte[] result = new byte[end - start];
        System.arraycopy(input, start, result, 0, end - start);
        return result;
    }
}
