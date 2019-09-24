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

import lombok.Getter;
import lombok.Setter;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.runtime.event.FilterQuery;
import org.gsc.runtime.event.trigger.ContractLogTrigger;

public class ContractLogTriggerWrapper extends TriggerWrapper {

    @Getter
    @Setter
    ContractLogTrigger contractLogTrigger;

    public ContractLogTriggerWrapper(ContractLogTrigger contractLogTrigger) {
        this.contractLogTrigger = contractLogTrigger;
    }

    public void setLatestConfirmedBlockNumber(long latestConfirmedBlockNumber) {
        contractLogTrigger.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
    }

    @Override
    public void processTrigger() {
        if (FilterQuery.matchFilter(contractLogTrigger)) {
            EventPluginLoader.getInstance().postContractLogTrigger(contractLogTrigger);
        }
    }
}
