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
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.gsc.crypto.Hash;
import org.gsc.runtime.event.ContractEventParserAbi;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.runtime.event.FilterQuery;
import org.gsc.runtime.event.trigger.ContractEventTrigger;
import org.gsc.runtime.event.trigger.ContractLogTrigger;
import org.gsc.runtime.event.trigger.ContractTrigger;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.LogInfo;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.SmartContract.ABI;

public class ContractTriggerWrapper extends TriggerWrapper {

    @Getter
    @Setter
    private ContractTrigger contractTrigger;

    public ContractTriggerWrapper(ContractTrigger contractTrigger) {
        this.contractTrigger = contractTrigger;
    }

    public void setLatestConfirmedBlockNumber(long latestConfirmedBlockNumber) {
        contractTrigger.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
    }

    @Override
    public void processTrigger() {
        ContractTrigger event;
        boolean isEvent = false;
        LogInfo logInfo = contractTrigger.getLogInfo();
        ABI abi = contractTrigger.getAbi();
        List<DataWord> topics = logInfo.getTopics();

        String eventSignature = "";
        String eventSignatureFull = "fallback()";
        String entryName = "";
        ABI.Entry eventEntry = null;

        if (abi != null && abi.getEntrysCount() > 0 && topics != null && !topics.isEmpty()
                && !ArrayUtils.isEmpty(topics.get(0).getData()) && Args.getInstance().getStorage()
                .isContractParseSwitch()) {
            String logHash = topics.get(0).toString();

            for (ABI.Entry entry : abi.getEntrysList()) {
                if (entry.getType() != ABI.Entry.EntryType.Event || entry.getAnonymous()) {
                    continue;
                }

                String signature = entry.getName() + "(";
                String signatureFull = entry.getName() + "(";
                StringBuilder signBuilder = new StringBuilder();
                StringBuilder signFullBuilder = new StringBuilder();
                for (ABI.Entry.Param param : entry.getInputsList()) {
                    if (signBuilder.length() > 0) {
                        signBuilder.append(",");
                        signFullBuilder.append(",");
                    }
                    String type = param.getType();
                    String name = param.getName();
                    signBuilder.append(type);
                    signFullBuilder.append(type);
                    if (StringUtils.isNotNullOrEmpty(name)) {
                        signFullBuilder.append(" ").append(name);
                    }
                }
                signature += signBuilder.toString() + ")";
                signatureFull += signFullBuilder.toString() + ")";
                String sha3 = Hex.toHexString(Hash.sha3(signature.getBytes()));
                if (sha3.equals(logHash)) {
                    eventSignature = signature;
                    eventSignatureFull = signatureFull;
                    entryName = entry.getName();
                    eventEntry = entry;
                    isEvent = true;
                    break;
                }
            }
        }

        if (isEvent) {
            if (!EventPluginLoader.getInstance().isContractEventTriggerEnable()) {
                return;
            }
            event = new ContractEventTrigger();
            ((ContractEventTrigger) event).setEventSignature(eventSignature);
            ((ContractEventTrigger) event).setEventSignatureFull(eventSignatureFull);
            ((ContractEventTrigger) event).setEventName(entryName);

            List<byte[]> topicList = logInfo.getClonedTopics();
            byte[] data = logInfo.getClonedData();

            ((ContractEventTrigger) event)
                    .setTopicMap(ContractEventParserAbi.parseTopics(topicList, eventEntry));
            ((ContractEventTrigger) event)
                    .setDataMap(ContractEventParserAbi.parseEventData(data, topicList, eventEntry));
        } else {
            if (!EventPluginLoader.getInstance().isContractLogTriggerEnable()) {
                return;
            }
            event = new ContractLogTrigger();
            ((ContractLogTrigger) event).setTopicList(logInfo.getHexTopics());
            ((ContractLogTrigger) event).setData(logInfo.getHexData());
        }

        RawData rawData = new RawData(logInfo.getAddress(), logInfo.getTopics(), logInfo.getData());

        event.setRawData(rawData);

        event.setLatestConfirmedBlockNumber(contractTrigger.getLatestConfirmedBlockNumber());
        event.setRemoved(contractTrigger.isRemoved());
        event.setUniqueId(contractTrigger.getUniqueId());
        event.setTransactionId(contractTrigger.getTransactionId());
        event.setContractAddress(contractTrigger.getContractAddress());
        event.setOriginAddress(contractTrigger.getOriginAddress());
        event.setCallerAddress("");
        event.setCreatorAddress(contractTrigger.getCreatorAddress());
        event.setBlockNumber(contractTrigger.getBlockNumber());
        event.setTimeStamp(contractTrigger.getTimeStamp());

        if (FilterQuery.matchFilter(contractTrigger)) {
            if (isEvent) {
                EventPluginLoader.getInstance().postContractEventTrigger((ContractEventTrigger) event);
            } else {
                EventPluginLoader.getInstance().postContractLogTrigger((ContractLogTrigger) event);
            }
        }
    }
}
