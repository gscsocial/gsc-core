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

import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.pf4j.util.StringUtils;
import org.gsc.runtime.event.trigger.ContractTrigger;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.SmartContract.ABI.Entry.Param;

public class LogEventWrapper extends ContractTrigger {

    @Getter
    @Setter
    private List<byte[]> topicList;

    @Getter
    @Setter
    private byte[] data;

    /**
     * decode from sha3($EventSignature) with the ABI of this contract.
     */
    @Getter
    @Setter
    private String eventSignature;

    /**
     * ABI Entry of this event.
     */
    @Getter
    @Setter
    private Protocol.SmartContract.ABI.Entry abiEntry;

    public LogEventWrapper() {
        super();
    }

    public String getEventSignatureFull() {
        if (Objects.isNull(abiEntry)) {
            return "fallback()";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(abiEntry.getName()).append("(");
        StringBuilder sbp = new StringBuilder();
        for (Param param : abiEntry.getInputsList()) {
            if (sbp.length() > 0) {
                sbp.append(",");
            }
            sbp.append(param.getType());
            if (StringUtils.isNotNullOrEmpty(param.getName())) {
                sbp.append(" ").append(param.getName());
            }
        }
        sb.append(sbp.toString()).append(")");
        return sb.toString();
    }
}
