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

package org.gsc.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.gsc.services.interfaceOnConfirmed.ConfirmedRpcApiService;
import org.rocksdb.RocksDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.gsc.config.args.Args;
import org.gsc.db.RevokingDatabase;
import org.gsc.db.RevokingStore;
import org.gsc.db.TransactionCache;
import org.gsc.db.api.IndexHelper;
import org.gsc.db.backup.BackupRocksDBAspect;
import org.gsc.db.backup.NeedBeanCondition;
import org.gsc.db.db2.core.SnapshotManager;
import org.gsc.services.interfaceOnConfirmed.http.confirmed.ConfirmedHttpApiService;

@Slf4j(topic = "app")
@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {

    static {
        RocksDB.loadLibrary();
    }

    @Autowired
    public ApplicationContext appCtx;

    @Autowired
    public CommonConfig commonConfig;

    public DefaultConfig() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception", e));
    }

    @Bean
    public IndexHelper indexHelper() {
        if (Args.getInstance().isConfirmedNode()
                && BooleanUtils.toBoolean(Args.getInstance().getStorage().getIndexSwitch())) {
            return new IndexHelper();
        }
        return null;
    }

    @Bean
    public RevokingDatabase revokingDatabase() {
        int dbVersion = Args.getInstance().getStorage().getDbVersion();
        RevokingDatabase revokingDatabase;
        try {
            if (dbVersion == 1) {
                revokingDatabase = RevokingStore.getInstance();
            } else if (dbVersion == 2) {
                revokingDatabase = new SnapshotManager();
            } else {
                throw new RuntimeException("db version is error.");
            }
            return revokingDatabase;
        } finally {
            logger.info("key-value data source created.");
        }
    }


    @Bean
    public ConfirmedRpcApiService getRpcApiServiceOnConfirmed() {
        boolean isConfirmedNode = Args.getInstance().isConfirmedNode();
        int dbVersion = Args.getInstance().getStorage().getDbVersion();
        if (!isConfirmedNode && dbVersion == 2) {
            return new ConfirmedRpcApiService();
        }

        return null;
    }

    @Bean
    public ConfirmedHttpApiService getHttpApiOnConfirmedService() {
        boolean isConfirmedNode = Args.getInstance().isConfirmedNode();
        int dbVersion = Args.getInstance().getStorage().getDbVersion();
        if (!isConfirmedNode && dbVersion == 2) {
            return new ConfirmedHttpApiService();
        }

        return null;
    }

    @Bean
    public TransactionCache transactionCache() {
        int dbVersion = Args.getInstance().getStorage().getDbVersion();
        if (dbVersion == 2) {
            return new TransactionCache("trans-cache");
        }

        return null;
    }

    @Bean
    @Conditional(NeedBeanCondition.class)
    public BackupRocksDBAspect backupRocksDBAspect() {
        return new BackupRocksDBAspect();
    }
}
