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

package org.gsc.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.DelegatedResourceAccountIndexWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DelegatedResourceAccountIndexStore extends
        GSCStoreWithRevoking<DelegatedResourceAccountIndexWrapper> {

    @Autowired
    public DelegatedResourceAccountIndexStore(@Value("delegated_resource_account_index") String dbName) {
        super(dbName);
    }

    @Override
    public DelegatedResourceAccountIndexWrapper get(byte[] key) {

        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new DelegatedResourceAccountIndexWrapper(value);
    }

}