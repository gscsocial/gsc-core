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

package org.gsc.db.dbsource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RocksDbSettings {

    private static RocksDbSettings settings;

    @Getter
    private int levelNumber;
    @Getter
    private int maxOpenFiles;
    @Getter
    private int compactThreads;
    @Getter
    private long blockSize;
    @Getter
    private long maxBytesForLevelBase;
    @Getter
    private double maxBytesForLevelMultiplier;
    @Getter
    private int level0FileNumCompactionTrigger;
    @Getter
    private long targetFileSizeBase;
    @Getter
    private int targetFileSizeMultiplier;
    @Getter
    private boolean enableStatistics;

    private RocksDbSettings() {

    }

    public static RocksDbSettings getDefaultSettings() {
        RocksDbSettings defaultSettings = new RocksDbSettings();
        return defaultSettings.withLevelNumber(7).withBlockSize(64).withCompactThreads(32)
                .withTargetFileSizeBase(256).withMaxBytesForLevelMultiplier(10)
                .withTargetFileSizeMultiplier(1)
                .withMaxBytesForLevelBase(256).withMaxOpenFiles(-1).withEnableStatistics(false);
    }

    public static RocksDbSettings getSettings() {
        if (settings == null) {
            return getDefaultSettings();
        }
        return settings;
    }

    public static RocksDbSettings initCustomSettings(int levelNumber, int compactThreads,
                                                     int blocksize, long maxBytesForLevelBase,
                                                     double maxBytesForLevelMultiplier, int level0FileNumCompactionTrigger,
                                                     long targetFileSizeBase,
                                                     int targetFileSizeMultiplier) {
        settings = new RocksDbSettings()
                .withMaxOpenFiles(-1)
                .withEnableStatistics(false)
                .withLevelNumber(levelNumber)
                .withCompactThreads(compactThreads)
                .withBlockSize(blocksize)
                .withMaxBytesForLevelBase(maxBytesForLevelBase)
                .withMaxBytesForLevelMultiplier(maxBytesForLevelMultiplier)
                .withLevel0FileNumCompactionTrigger(level0FileNumCompactionTrigger)
                .withTargetFileSizeBase(targetFileSizeBase)
                .withTargetFileSizeMultiplier(targetFileSizeMultiplier);
        return settings;
    }


    public RocksDbSettings withMaxOpenFiles(int maxOpenFiles) {
        this.maxOpenFiles = maxOpenFiles;
        return this;
    }

    public RocksDbSettings withCompactThreads(int compactThreads) {
        this.compactThreads = compactThreads;
        return this;
    }

    public RocksDbSettings withBlockSize(long blockSize) {
        this.blockSize = blockSize * 1024;
        return this;
    }

    public RocksDbSettings withMaxBytesForLevelBase(long maxBytesForLevelBase) {
        this.maxBytesForLevelBase = maxBytesForLevelBase * 1024 * 1024;
        return this;
    }

    public RocksDbSettings withMaxBytesForLevelMultiplier(double maxBytesForLevelMultiplier) {
        this.maxBytesForLevelMultiplier = maxBytesForLevelMultiplier;
        return this;
    }

    public RocksDbSettings withLevel0FileNumCompactionTrigger(int level0FileNumCompactionTrigger) {
        this.level0FileNumCompactionTrigger = level0FileNumCompactionTrigger;
        return this;
    }

    public RocksDbSettings withEnableStatistics(boolean enable) {
        this.enableStatistics = enable;
        return this;
    }

    public RocksDbSettings withLevelNumber(int levelNumber) {
        this.levelNumber = levelNumber;
        return this;
    }


    public RocksDbSettings withTargetFileSizeBase(long targetFileSizeBase) {
        this.targetFileSizeBase = targetFileSizeBase * 1024 * 1024;
        return this;
    }

    public RocksDbSettings withTargetFileSizeMultiplier(int targetFileSizeMultiplier) {
        this.targetFileSizeMultiplier = targetFileSizeMultiplier;
        return this;
    }

    public static void loggingSettings() {
        logger.info(String.format(
                "level number: %d, CompactThreads: %d, Blocksize: %d, maxBytesForLevelBase: %d,"
                        + " withMaxBytesForLevelMultiplier: %f, level0FileNumCompactionTrigger: %d, "
                        + "withTargetFileSizeBase: %d, withTargetFileSizeMultiplier: %d",
                settings.getLevelNumber(),
                settings.getCompactThreads(), settings.getBlockSize(), settings.getMaxBytesForLevelBase(),
                settings.getMaxBytesForLevelMultiplier(), settings.getLevel0FileNumCompactionTrigger(),
                settings.getTargetFileSizeBase(), settings.getTargetFileSizeMultiplier()));
    }
}
