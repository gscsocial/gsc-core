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

package org.gsc.runtime.event.wrapper;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.gsc.runtime.event.ContractEventParserAbi;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.runtime.event.FilterQuery;
import org.gsc.runtime.event.trigger.ContractEventTrigger;
import org.gsc.runtime.vm.LogEventWrapper;
import org.gsc.protos.Protocol.SmartContract.ABI.Entry;

public class ContractEventTriggerWrapper extends TriggerWrapper {

    @Getter
    @Setter
    private List<byte[]> topicList;

    @Getter
    @Setter
    private byte[] data;

    @Getter
    @Setter
    ContractEventTrigger contractEventTrigger;

    @Getter
    @Setter
    private Entry abiEntry;

    public void setLatestConfirmedBlockNumber(long latestConfirmedBlockNumber) {
        contractEventTrigger.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
    }

    public ContractEventTriggerWrapper(LogEventWrapper log) {
        this.contractEventTrigger = new ContractEventTrigger();

        this.contractEventTrigger.setUniqueId(log.getUniqueId());
        this.contractEventTrigger.setTransactionId(log.getTransactionId());
        this.contractEventTrigger.setContractAddress(log.getContractAddress());
        this.contractEventTrigger.setCallerAddress(log.getCallerAddress());
        this.contractEventTrigger.setOriginAddress(log.getOriginAddress());
        this.contractEventTrigger.setCreatorAddress(log.getCreatorAddress());
        this.contractEventTrigger.setBlockNumber(log.getBlockNumber());
        this.contractEventTrigger.setTimeStamp(log.getTimeStamp());

        this.topicList = log.getTopicList();
        this.data = log.getData();
        this.contractEventTrigger.setEventSignature(log.getEventSignature());
        this.contractEventTrigger.setEventSignatureFull(log.getEventSignatureFull());
        this.contractEventTrigger.setEventName(log.getAbiEntry().getName());
        this.abiEntry = log.getAbiEntry();
    }

    @Override
    public void processTrigger() {
        contractEventTrigger.setTopicMap(ContractEventParserAbi.parseTopics(topicList, abiEntry));
        contractEventTrigger
                .setDataMap(ContractEventParserAbi.parseEventData(data, topicList, abiEntry));

        if (FilterQuery.matchFilter(contractEventTrigger)) {
            EventPluginLoader.getInstance().postContractEventTrigger(contractEventTrigger);
        }
    }
}
