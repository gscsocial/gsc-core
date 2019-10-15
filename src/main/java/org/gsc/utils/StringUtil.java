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

import com.google.protobuf.ByteString;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.gsc.core.Wallet;

public class StringUtil {

    /**
     * n-bits hex string.
     *
     * @param str  target string
     * @param bits string bits
     */
    public static boolean isHexString(String str, int bits) {
        String regex = String.format("^[A-Fa-f0-9]{%d}$", bits);
        return str.matches(regex);
    }

    public static byte[] createDbKey(ByteString string) {
        return string.toByteArray();
    }

    public static String createReadableString(byte[] bytes) {
        return ByteArray.toHexString(bytes);
    }

    public static String createReadableString(ByteString string) {
        return createReadableString(string.toByteArray());
    }

    public static List<String> getAddressStringList(Collection<ByteString> collection) {
        return collection.stream()
                .map(bytes -> Wallet.encode58Check(bytes.toByteArray()))
                .collect(Collectors.toList());
    }

    public static List<String> getAddressStringListFromByteArray(Collection<byte[]> collection) {
        return collection.stream()
                .map(bytes -> createReadableString(bytes))
                .collect(Collectors.toList());
    }

    public static ByteString hexString2ByteString(String hexString) {
        return ByteString.copyFrom(ByteArray.fromHexString(hexString));
    }
}
