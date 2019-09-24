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

package org.gsc.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.springframework.stereotype.Service;
import org.gsc.utils.ByteArray;

@Slf4j(topic = "witness")
@Service
public class WitnessProductBlockService {

    private Cache<Long, BlockWrapper> historyBlockWrapperCache = CacheBuilder.newBuilder()
            .initialCapacity(200).maximumSize(200).build();

    private Map<String, CheatWitnessInfo> cheatWitnessInfoMap = new HashMap<>();

    public static class CheatWitnessInfo {

        private AtomicInteger times = new AtomicInteger(0);
        private long latestBlockNum;
        private Set<BlockWrapper> blockWrapperSet = new HashSet<>();
        private long time;

        public CheatWitnessInfo increment() {
            times.incrementAndGet();
            return this;
        }

        public AtomicInteger getTimes() {
            return times;
        }

        public CheatWitnessInfo setTimes(AtomicInteger times) {
            this.times = times;
            return this;
        }

        public long getLatestBlockNum() {
            return latestBlockNum;
        }

        public CheatWitnessInfo setLatestBlockNum(long latestBlockNum) {
            this.latestBlockNum = latestBlockNum;
            return this;
        }

        public Set<BlockWrapper> getBlockWrapperSet() {
            return new HashSet<>(blockWrapperSet);
        }

        public CheatWitnessInfo clear() {
            blockWrapperSet.clear();
            return this;
        }

        public CheatWitnessInfo add(BlockWrapper blockWrapper) {
            blockWrapperSet.add(blockWrapper);
            return this;
        }

        public CheatWitnessInfo setBlockWrapperSet(Set<BlockWrapper> blockWrapperSet) {
            this.blockWrapperSet = new HashSet<>(blockWrapperSet);
            return this;
        }

        public long getTime() {
            return time;
        }

        public CheatWitnessInfo setTime(long time) {
            this.time = time;
            return this;
        }

        @Override
        public String toString() {
            return "{" +
                    "times=" + times.get() +
                    ", time=" + time +
                    ", latestBlockNum=" + latestBlockNum +
                    ", blockWrapperSet=" + blockWrapperSet +
                    '}';
        }
    }

    public void validWitnessProductTwoBlock(BlockWrapper block) {
        try {
            BlockWrapper blockWrapper = historyBlockWrapperCache.getIfPresent(block.getNum());
            if (blockWrapper != null && Arrays.equals(blockWrapper.getWitnessAddress().toByteArray(),
                    block.getWitnessAddress().toByteArray()) && !Arrays.equals(block.getBlockId().getBytes(),
                    blockWrapper.getBlockId().getBytes())) {
                String key = ByteArray.toHexString(block.getWitnessAddress().toByteArray());
                if (!cheatWitnessInfoMap.containsKey(key)) {
                    CheatWitnessInfo cheatWitnessInfo = new CheatWitnessInfo();
                    cheatWitnessInfoMap.put(key, cheatWitnessInfo);
                }
                cheatWitnessInfoMap.get(key).clear().setTime(System.currentTimeMillis())
                        .setLatestBlockNum(block.getNum()).add(block).add(blockWrapper).increment();
            } else {
                historyBlockWrapperCache.put(block.getNum(), block);
            }
        } catch (Exception e) {
            logger.error("valid witness same time product two block fail! blockNum: {}, blockHash: {}",
                    block.getNum(), block.getBlockId().toString(), e);
        }
    }

    public Map<String, CheatWitnessInfo> queryCheatWitnessInfo() {
        return cheatWitnessInfoMap;
    }
}
