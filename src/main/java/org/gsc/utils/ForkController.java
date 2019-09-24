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

package org.gsc.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.config.Parameter.ForkBlockVersionConsts;
import org.gsc.config.Parameter.ForkBlockVersionEnum;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;

@Slf4j(topic = "utils")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ForkController {

    private static final byte VERSION_DOWNGRADE = (byte) 0;
    private static final byte VERSION_UPGRADE = (byte) 1;
    private static final byte[] check;

    static {
        check = new byte[1024];
        Arrays.fill(check, VERSION_UPGRADE);
    }

    @Getter
    private Manager manager;

    public void init(Manager manager) {
        this.manager = manager;
    }

    public boolean pass(ForkBlockVersionEnum forkBlockVersionEnum) {
        return pass(forkBlockVersionEnum.getValue());
    }

    public synchronized boolean pass(int version) {
//        if (version == ForkBlockVersionConsts.CPU_LIMIT) {
//            return checkForCpuLimit();
//        }

        byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
        return check(stats);
    }

    // when block.version = 5,
    // it make block use new cpu to handle transaction when block number >= 4727890L.
    // version !=5, skip this.
//    private boolean checkForCpuLimit() {
//        long blockNum = manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
//        return blockNum >= Args.getInstance().getBlockNumForEneryLimit();
//    }

    private boolean check(byte[] stats) {
        if (stats == null || stats.length == 0) {
            return false;
        }

        for (int i = 0; i < stats.length; i++) {
            if (check[i] != stats[i]) {
                return false;
            }
        }

        return true;
    }

    private void downgrade(int version, int slot) {
        for (ForkBlockVersionEnum versionEnum : ForkBlockVersionEnum.values()) {
            int versionValue = versionEnum.getValue();
            if (versionValue > version) {
                byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(versionValue);
                if (!check(stats) && Objects.nonNull(stats)) {
                    stats[slot] = VERSION_DOWNGRADE;
                    manager.getDynamicPropertiesStore().statsByVersion(versionValue, stats);
                }
            }
        }
    }

    private void upgrade(int version, int slotSize) {
        for (ForkBlockVersionEnum versionEnum : ForkBlockVersionEnum.values()) {
            int versionValue = versionEnum.getValue();
            if (versionValue < version) {
                byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(versionValue);
                if (!check(stats)) {
                    if (stats == null || stats.length == 0) {
                        stats = new byte[slotSize];
                    }
                    Arrays.fill(stats, VERSION_UPGRADE);
                    manager.getDynamicPropertiesStore().statsByVersion(versionValue, stats);
                }
            }
        }
    }

    public synchronized void update(BlockWrapper blockWrapper) {
        List<ByteString> witnesses = manager.getWitnessController().getActiveWitnesses();
        ByteString witness = blockWrapper.getWitnessAddress();
        int slot = witnesses.indexOf(witness);
        if (slot < 0) {
            return;
        }

        int version = blockWrapper.getInstance().getBlockHeader().getRawData().getVersion();
//        if (version < ForkBlockVersionConsts.CPU_LIMIT) {
//            return;
//        }

        downgrade(version, slot);

        byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
        if (check(stats)) {
            upgrade(version, stats.length);
            return;
        }

        if (Objects.isNull(stats) || stats.length != witnesses.size()) {
            stats = new byte[witnesses.size()];
        }

        stats[slot] = VERSION_UPGRADE;
        manager.getDynamicPropertiesStore().statsByVersion(version, stats);
        logger.info(
                "*******update hard fork:{}, witness size:{}, solt:{}, witness:{}, version:{}",
                Streams.zip(witnesses.stream(), Stream.of(ArrayUtils.toObject(stats)), Maps::immutableEntry)
                        .map(e -> Maps
                                .immutableEntry(Wallet.encode58Check(e.getKey().toByteArray()), e.getValue()))
                        .map(e -> Maps
                                .immutableEntry(StringUtils.substring(e.getKey(), e.getKey().length() - 4),
                                        e.getValue()))
                        .collect(Collectors.toList()),
                witnesses.size(),
                slot,
                Wallet.encode58Check(witness.toByteArray()),
                version);
    }

    public synchronized void reset() {
        for (ForkBlockVersionEnum versionEnum : ForkBlockVersionEnum.values()) {
            int versionValue = versionEnum.getValue();
            byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(versionValue);
            if (!check(stats) && Objects.nonNull(stats)) {
                Arrays.fill(stats, VERSION_DOWNGRADE);
                manager.getDynamicPropertiesStore().statsByVersion(versionValue, stats);
            }
        }
    }

    public static ForkController instance() {
        return ForkControllerEnum.INSTANCE.getInstance();
    }

    private enum ForkControllerEnum {
        INSTANCE;

        private ForkController instance;

        ForkControllerEnum() {
            instance = new ForkController();
        }

        private ForkController getInstance() {
            return instance;
        }
    }
}
