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

package org.gsc.net.node.table;

/**
 * Created by kest on 5/25/15.
 */
public class KademliaOptions {

    public static final int BUCKET_SIZE = 16;
    public static final int ALPHA = 3;
    public static final int BINS = 256;
    public static final int MAX_STEPS = 8;

    public static final long REQ_TIMEOUT = 300;
    public static final long BUCKET_REFRESH = 7200;     //bucket refreshing interval in millis
    public static final long DISCOVER_CYCLE = 30;       //discovery cycle interval in seconds
}
