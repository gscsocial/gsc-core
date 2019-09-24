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

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class InternalTransactionPojo {

    @Getter
    @Setter
    private String hash;

    @Getter
    @Setter
    /* the amount of gsc to transfer (calculated as dot) */
    private long callValue;

    @Getter
    @Setter
    private Map<String, Long> tokenInfo = new HashMap<>();

    /* the address of the destination account (for message)
     * In creation transaction the receive address is - 0 */
    @Getter
    @Setter
    private String transferTo_address;

    /* An unlimited size byte array specifying
     * input [data] of the message call or
     * Initialization code for a new contract */
    @Getter
    @Setter
    private String data;

    /*  Message sender address */
    @Getter
    @Setter
    private String caller_address;

    @Getter
    @Setter
    private boolean rejected;

    @Getter
    @Setter
    private String note;
}
