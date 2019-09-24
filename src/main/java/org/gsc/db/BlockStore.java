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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.exception.BadItemException;

@Slf4j(topic = "DB")
@Component
public class BlockStore extends GSCStoreWithRevoking<BlockWrapper> {

    @Autowired
    private BlockStore(@Value("block") String dbName) {
        super(dbName);
    }

    public List<BlockWrapper> getLimitNumber(long startNumber, long limit) {
        BlockId startBlockId = new BlockId(Sha256Hash.ZERO_HASH, startNumber);
        return revokingDB.getValuesNext(startBlockId.getBytes(), limit).stream()
                .map(bytes -> {
                    try {
                        return new BlockWrapper(bytes);
                    } catch (BadItemException ignored) {
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BlockWrapper::getNum))
                .collect(Collectors.toList());
    }

    public List<BlockWrapper> getBlockByLatestNum(long getNum) {

        return revokingDB.getlatestValues(getNum).stream()
                .map(bytes -> {
                    try {
                        return new BlockWrapper(bytes);
                    } catch (BadItemException ignored) {
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BlockWrapper::getNum))
                .collect(Collectors.toList());
    }
}
