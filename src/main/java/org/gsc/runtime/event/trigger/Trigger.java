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

package org.gsc.runtime.event.trigger;

import lombok.Getter;
import lombok.Setter;

public class Trigger {

    @Getter
    @Setter
    protected long timeStamp;

    @Getter
    @Setter
    private String triggerName;

    public static final int BLOCK_TRIGGER = 0;
    public static final int TRANSACTION_TRIGGER = 1;
    public static final int CONTRACTLOG_TRIGGER = 2;
    public static final int CONTRACTEVENT_TRIGGER = 3;

    public static final String BLOCK_TRIGGER_NAME = "blockTrigger";
    public static final String TRANSACTION_TRIGGER_NAME = "transactionTrigger";
    public static final String CONTRACTLOG_TRIGGER_NAME = "contractLogTrigger";
    public static final String CONTRACTEVENT_TRIGGER_NAME = "contractEventTrigger";
}
