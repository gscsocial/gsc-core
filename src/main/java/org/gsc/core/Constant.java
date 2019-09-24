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



package org.gsc.core;

import org.gsc.utils.ByteArray;

public class Constant {

    // whole
    public static final byte[] LAST_HASH = ByteArray.fromString("lastHash");
    public static final String DIFFICULTY = "2001";

    // DB
    public static final String BLOCK_DB_NAME = "block_data";
    public static final String TRANSACTION_DB_NAME = "transaction_data";

    //config for testnet, mainnet, beta
    public static final String MAIN_NET_CONF = "config.conf";
    public static final String TEST_NET_CONF = "config-test.conf";

    public static final byte[] ADD_PRE_FIX_BYTE = new byte[]{(byte) 0x01, (byte) 0xf8, (byte) 0x0c};   //26 + address
    public static final String ADD_PRE_FIX_STRING = "01f80c";
    public static final int ADDRESS_SIZE = 46;

    // config for transaction
    public static final long TRANSACTION_MAX_BYTE_SIZE = 500 * 1_024L;
    public static final long MAXIMUM_TIME_UNTIL_EXPIRATION = 24 * 60 * 60 * 1_000L; //one day
    public static final long TRANSACTION_DEFAULT_EXPIRATION_TIME = 60 * 1_000L; //60 seconds
    // config for smart contract
    public static final long DOT_PER_CPU = 10; // 1 us = 10 DROP = 100 * 10^-6 GSC
    public static final long CPU_LIMIT_IN_CONSTANT_TX = 3_000_000L; // ref: 1 us = 1 cpu
    public static final long MAX_RESULT_SIZE_IN_TX = 64; // max 8 * 8 items in result
    public static final long PB_DEFAULT_CPU_LIMIT = 0L;
    public static final long CREATOR_DEFAULT_CPU_LIMIT = 1000 * 10_000L;

    // Numbers
    public static final int ONE_HUNDRED = 100;
    public static final int ONE_THOUSAND = 1000;

    /**
     * normal transaction is 0 representing normal transaction
     * unexecuted deferred transaction is 1 representing unexecuted deferred transaction
     * executing deferred transaction is 2 representing executing deferred transaction
     */
    public static final int NORMALTRANSACTION = 0;
    public static final int UNEXECUTEDDEFERREDTRANSACTION = 1;
    public static final int EXECUTINGDEFERREDTRANSACTION = 2;
}
