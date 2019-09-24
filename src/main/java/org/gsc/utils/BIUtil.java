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

import java.math.BigInteger;

public class BIUtil {

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is less than valueB is zero
     */
    public static boolean isLessThan(BigInteger valueA, BigInteger valueB) {
        return valueA.compareTo(valueB) < 0;
    }

    /**
     * @param value - not null
     * @return true - if the param is zero
     */
    public static boolean isZero(BigInteger value) {
        return value.compareTo(BigInteger.ZERO) == 0;
    }

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is equal to valueB is zero
     */
    public static boolean isEqual(BigInteger valueA, BigInteger valueB) {
        return valueA.compareTo(valueB) == 0;
    }

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is not equal to valueB is zero
     */
    public static boolean isNotEqual(BigInteger valueA, BigInteger valueB) {
        return !isEqual(valueA, valueB);
    }

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is more than valueB is zero
     */
    public static boolean isMoreThan(BigInteger valueA, BigInteger valueB) {
        return valueA.compareTo(valueB) > 0;
    }


    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return sum - valueA + valueB
     */
    public static BigInteger sum(BigInteger valueA, BigInteger valueB) {
        return valueA.add(valueB);
    }


    /**
     * @param data = not null
     * @return new positive BigInteger
     */
    public static BigInteger toBI(byte[] data) {
        return new BigInteger(1, data);
    }

    /**
     * @param data = not null
     * @return new positive BigInteger
     */
    public static BigInteger toBI(long data) {
        return BigInteger.valueOf(data);
    }


    public static boolean isPositive(BigInteger value) {
        return value.signum() > 0;
    }

    public static boolean isNotCovers(BigInteger covers, BigInteger value) {
        return covers.compareTo(value) < 0;
    }

    public static BigInteger max(BigInteger first, BigInteger second) {
        return first.compareTo(second) < 0 ? second : first;
    }

    /**
     * Returns a result of safe addition of two {@code int} values {@code Integer.MAX_VALUE} is
     * returned if overflow occurs
     */
    public static int addSafely(int a, int b) {
        long res = (long) a + (long) b;
        return res > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) res;
    }
}
