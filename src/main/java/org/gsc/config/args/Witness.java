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

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;

public class Witness implements Serializable {

    private static final long serialVersionUID = -7446501098542377380L;

    @Getter
    private byte[] address;

    @Getter
    private String url;

    @Getter
    @Setter
    private long voteCount;

    /**
     * set address.
     */
    public void setAddress(final byte[] address) {
        if (!Wallet.addressValid(address)) {
            throw new IllegalArgumentException(
                    "The address(" + StringUtil.createReadableString(address) + ") must be a 23 bytes.");
        }
        this.address = address;
    }

    /**
     * set url.
     */
    public void setUrl(final String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException(
                    "The url(" + url + ") format error.");
        }

        this.url = url;
    }
}
