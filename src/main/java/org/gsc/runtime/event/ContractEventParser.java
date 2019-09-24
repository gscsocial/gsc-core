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

package org.gsc.runtime.event;

import java.math.BigInteger;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.crypto.OutputLengthException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.utils.MUtil;
import org.gsc.runtime.vm.DataWord;
import org.gsc.core.Wallet;

@Slf4j(topic = "Parser")
public class ContractEventParser {

    private static final int DATAWORD_UNIT_SIZE = 32;

    private enum Type {
        UNKNOWN,
        INT_NUMBER,
        BOOL,
        FLOAT_NUMBER,
        FIXED_BYTES,
        ADDRESS,
        STRING,
        BYTES,
    }

    protected static String parseDataBytes(byte[] data, String typeStr, int index) {
        try {
            byte[] startBytes = subBytes(data, index * DATAWORD_UNIT_SIZE, DATAWORD_UNIT_SIZE);
            Type type = basicType(typeStr);

            if (type == Type.INT_NUMBER) {
                return new BigInteger(startBytes).toString();
            } else if (type == Type.BOOL) {
                return String.valueOf(!DataWord.isZero(startBytes));
            } else if (type == Type.FIXED_BYTES) {
                return Hex.toHexString(startBytes);
            } else if (type == Type.ADDRESS) {
                byte[] last20Bytes = Arrays.copyOfRange(startBytes, 12, startBytes.length);
                return Wallet.encode58Check(MUtil.convertToGSCAddress(last20Bytes));
            } else if (type == Type.STRING || type == Type.BYTES) {
                int start = intValueExact(startBytes);
                byte[] lengthBytes = subBytes(data, start, DATAWORD_UNIT_SIZE);
                // this length is byte count. no need X 32
                int length = intValueExact(lengthBytes);
                byte[] realBytes =
                        length > 0 ? subBytes(data, start + DATAWORD_UNIT_SIZE, length) : new byte[0];
                return type == Type.STRING ? new String(realBytes) : Hex.toHexString(realBytes);
            }
        } catch (OutputLengthException | ArithmeticException e) {
            logger.debug("parseDataBytes ", e);
        }
        throw new UnsupportedOperationException("unsupported type:" + typeStr);
    }

    // don't support these type yet : bytes32[10][10]  OR  bytes32[][10]
    protected static Type basicType(String type) {
        if (!Pattern.matches("^.*\\[\\d*\\]$", type)) {
            // ignore not valide type such as "int92", "bytes33", these types will be compiled failed.
            if (type.startsWith("int") || type.startsWith("uint") || type.startsWith("grcToken")) {
                return Type.INT_NUMBER;
            } else if ("bool".equals(type)) {
                return Type.BOOL;
            } else if ("address".equals(type)) {
                return Type.ADDRESS;
            } else if (Pattern.matches("^bytes\\d+$", type)) {
                return Type.FIXED_BYTES;
            } else if ("string".equals(type)) {
                return Type.STRING;
            } else if ("bytes".equals(type)) {
                return Type.BYTES;
            }
        }
        return Type.UNKNOWN;
    }

    protected static Integer intValueExact(byte[] data) {
        return new BigInteger(data).intValueExact();
    }

    protected static byte[] subBytes(byte[] src, int start, int length) {
        if (ArrayUtils.isEmpty(src) || start >= src.length || length < 0) {
            throw new OutputLengthException("data start:" + start + ", length:" + length);
        }
        byte[] dst = new byte[length];
        System.arraycopy(src, start, dst, 0, Math.min(length, src.length - start));
        return dst;
    }

    /**
     * support: uint<m> (m ∈ [8, 256], m % 8 == 0), int<m> (m ∈ [8, 256], m % 8 == 0) uint (solidity
     * abi will auto convert to uint256) int (solidity abi will auto convert to int256) bool
     * <p>
     * otherwise, returns hexString
     * <p>
     * This is only for decode Topic. Since Topic and Data use different encode methods when deal
     * dynamic length types, such as bytes and string.
     */
    protected static String parseTopic(byte[] bytes, String typeStr) {
        if (ArrayUtils.isEmpty(bytes) || StringUtils.isNullOrEmpty(typeStr)) {
            return "";
        }
        Type type = basicType(typeStr);
        if (type == Type.INT_NUMBER) {
            return DataWord.bigIntValue(bytes);
        } else if (type == Type.BOOL) {
            return String.valueOf(!DataWord.isZero(bytes));
        } else if (type == Type.ADDRESS) {
            byte[] last20Bytes = Arrays.copyOfRange(bytes, 12, bytes.length);
            return Wallet.encode58Check(MUtil.convertToGSCAddress(last20Bytes));
        }
        return Hex.toHexString(bytes);
    }
}
