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

import org.pf4j.ExtensionPoint;

public interface IPluginEventListener extends ExtensionPoint {

    void setServerAddress(String address);

    void setTopic(int eventType, String topic);

    void setDBConfig(String dbConfig);

    // start should be called after setServerAddress, setTopic, setDBConfig
    void start();

    void handleBlockEvent(Object trigger);

    void handleTransactionTrigger(Object trigger);

    void handleContractLogTrigger(Object trigger);

    void handleContractEventTrigger(Object trigger);

}
