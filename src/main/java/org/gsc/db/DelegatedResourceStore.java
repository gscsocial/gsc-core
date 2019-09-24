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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.DelegatedResourceWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DelegatedResourceStore extends GSCStoreWithRevoking<DelegatedResourceWrapper> {

    @Autowired
    public DelegatedResourceStore(@Value("delegated_resource") String dbName) {
        super(dbName);
    }

    @Override
    public DelegatedResourceWrapper get(byte[] key) {

        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new DelegatedResourceWrapper(value);
    }

    @Deprecated
    public List<DelegatedResourceWrapper> getByFrom(byte[] key) {
        return revokingDB.getValuesNext(key, Long.MAX_VALUE).stream()
                .map(DelegatedResourceWrapper::new)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}