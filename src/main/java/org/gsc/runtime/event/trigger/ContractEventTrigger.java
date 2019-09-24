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

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class ContractEventTrigger extends ContractTrigger {

    /**
     * decode from sha3($EventSignature) with the ABI of this contract.
     */
    @Getter
    @Setter
    private String eventSignature;

    @Getter
    @Setter
    private String eventSignatureFull;

    @Getter
    @Setter
    private String eventName;

    /**
     * decode from topicList with the ABI of this contract. this item is null if not called
     * ContractEventParserAbi::parseTopics(ContractEventTrigger trigger)
     */
    @Getter
    @Setter
    private Map<String, String> topicMap;

    /**
     * multi data items will be concat into a single string. this item is null if not called
     * ContractEventParserAbi::parseData(ContractEventTrigger trigger)
     */
    @Getter
    @Setter
    private Map<String, String> dataMap;


    public ContractEventTrigger() {
        super();
        setTriggerName(CONTRACTEVENT_TRIGGER_NAME);
    }
}
