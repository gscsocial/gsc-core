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
package org.gsc.config.args;

import lombok.Getter;
import org.apache.commons.lang3.Range;

public class Overlay {

    @Getter
    private int port;

    /**
     * Monitor port number.
     */
    public void setPort(final int port) {
        Range<Integer> range = Range.between(0, 65535);
        if (!range.contains(port)) {
            throw new IllegalArgumentException("Port(" + port + ") must in [0, 65535]");
        }

        this.port = port;
    }
}
