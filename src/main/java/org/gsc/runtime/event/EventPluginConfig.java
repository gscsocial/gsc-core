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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class EventPluginConfig {

    public static final String BLOCK_TRIGGER_NAME = "block";
    public static final String TRANSACTION_TRIGGER_NAME = "transaction";
    public static final String CONTRACTEVENT_TRIGGER_NAME = "contractevent";
    public static final String CONTRACTLOG_TRIGGER_NAME = "contractlog";

    @Getter
    @Setter
    private String pluginPath;

    @Getter
    @Setter
    private String serverAddress;

    @Getter
    @Setter
    private String dbConfig;

    @Getter
    @Setter
    private boolean useNativeQueue;

    @Getter
    @Setter
    private int bindPort;

    @Getter
    @Setter
    private int sendQueueLength;


    @Getter
    @Setter
    private List<TriggerConfig> triggerConfigList;

    public EventPluginConfig() {
        pluginPath = "";
        serverAddress = "";
        dbConfig = "";
        useNativeQueue = false;
        bindPort = 0;
        sendQueueLength = 0;
        triggerConfigList = new ArrayList<>();
    }
}
