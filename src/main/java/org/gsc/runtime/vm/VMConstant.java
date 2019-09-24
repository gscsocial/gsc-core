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

package org.gsc.runtime.vm;

public class VMConstant {

    public static final double TX_CPU_LIMIT_DEFAULT_RATIO = 1.0;

    public static final String REASON_ALREADY_TIME_OUT = "Haven Time Out";
    public static final int CONTRACT_NAME_LENGTH = 32;
    public static final int MIN_TOKEN_ID = 1000_000;

    private VMConstant() {
    }
}
