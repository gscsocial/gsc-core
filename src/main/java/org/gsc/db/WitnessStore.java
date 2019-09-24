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

import com.google.common.collect.Streams;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.WitnessWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j(topic = "DB")
@Component
public class WitnessStore extends GSCStoreWithRevoking<WitnessWrapper> {

    @Autowired
    protected WitnessStore(@Value("witness") String dbName) {
        super(dbName);
    }

    /**
     * get all witnesses.
     */
    public List<WitnessWrapper> getAllWitnesses() {
        return Streams.stream(iterator())
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public WitnessWrapper get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new WitnessWrapper(value);
    }
}
