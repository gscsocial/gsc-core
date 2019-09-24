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

package org.gsc.db.backup;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.utils.PropUtil;
import org.gsc.config.args.Args;
import org.gsc.db.RevokingDatabase;
import org.gsc.db.db2.core.RevokingDBWithCachingNewValue;
import org.gsc.db.db2.core.SnapshotManager;
import org.gsc.db.db2.core.SnapshotRoot;

@Slf4j
@Component
public class BackupDbUtil {

    @Getter
    private static final String DB_BACKUP_STATE = "DB";
    private static final int DB_BACKUP_INDEX1 = 1;
    private static final int DB_BACKUP_INDEX2 = 2;

    @Getter
    private static final int DB_BACKUP_STATE_DEFAULT = 11;

    public enum State {
        BAKINGONE(1), BAKEDONE(11), BAKINGTWO(2), BAKEDTWO(22);
        private int status;

        State(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public static State valueOf(int value) {
            switch (value) {
                case 1:
                    return BAKINGONE;
                case 11:
                    return BAKEDONE;
                case 2:
                    return BAKINGTWO;
                case 22:
                    return BAKEDTWO;
                default:
                    return BAKEDONE;
            }
        }
    }

    @Getter
    @Autowired
    private RevokingDatabase db;

    private Args args = Args.getInstance();

    private int getBackupState() {
        try {
            return Integer.valueOf(PropUtil
                    .readProperty(args.getDbBackupConfig().getPropPath(), BackupDbUtil.DB_BACKUP_STATE)
            );
        } catch (NumberFormatException ignore) {
            return DB_BACKUP_STATE_DEFAULT;  //get default state if prop file is newly created
        }
    }

    private void setBackupState(int status) {
        PropUtil.writeProperty(args.getDbBackupConfig().getPropPath(), BackupDbUtil.DB_BACKUP_STATE,
                String.valueOf(status));
    }

    private void switchBackupState() {
        switch (State.valueOf(getBackupState())) {
            case BAKINGONE:
                setBackupState(State.BAKEDONE.getStatus());
                break;
            case BAKEDONE:
                setBackupState(State.BAKEDTWO.getStatus());
                break;
            case BAKINGTWO:
                setBackupState(State.BAKEDTWO.getStatus());
                break;
            case BAKEDTWO:
                setBackupState(State.BAKEDONE.getStatus());
                break;
            default:
                break;
        }
    }

    public void doBackup(BlockWrapper block) {
        long t1 = System.currentTimeMillis();
        try {
            switch (State.valueOf(getBackupState())) {
                case BAKINGONE:
                    deleteBackup(DB_BACKUP_INDEX1);
                    backup(DB_BACKUP_INDEX1);
                    switchBackupState();
                    deleteBackup(DB_BACKUP_INDEX2);
                    break;
                case BAKEDONE:
                    deleteBackup(DB_BACKUP_INDEX2);
                    backup(DB_BACKUP_INDEX2);
                    switchBackupState();
                    deleteBackup(DB_BACKUP_INDEX1);
                    break;
                case BAKINGTWO:
                    deleteBackup(DB_BACKUP_INDEX2);
                    backup(DB_BACKUP_INDEX2);
                    switchBackupState();
                    deleteBackup(DB_BACKUP_INDEX1);
                    break;
                case BAKEDTWO:
                    deleteBackup(DB_BACKUP_INDEX1);
                    backup(DB_BACKUP_INDEX1);
                    switchBackupState();
                    deleteBackup(DB_BACKUP_INDEX2);
                    break;
                default:
                    logger.warn("invalid backup state");
            }
        } catch (RocksDBException | SecurityException e) {
            logger.warn("backup db error:" + e);
        }
        long timeUsed = System.currentTimeMillis() - t1;
        logger
                .info("current block number is {}, backup all store use {} ms!", block.getNum(), timeUsed);
        if (timeUsed >= 3000) {
            logger.warn("backup db use too much time.");
        }
    }

    private void backup(int i) throws RocksDBException {
        String path = "";
        if (i == DB_BACKUP_INDEX1) {
            path = args.getDbBackupConfig().getBak1path();
        } else if (i == DB_BACKUP_INDEX2) {
            path = args.getDbBackupConfig().getBak2path();
        } else {
            throw new RuntimeException("Error backup with undefined index");
        }
        List<RevokingDBWithCachingNewValue> stores = ((SnapshotManager) db).getDbs();
        for (RevokingDBWithCachingNewValue store : stores) {
            if (((SnapshotRoot) (store.getHead().getRoot())).getDb().getClass()
                    == org.gsc.db.db2.common.RocksDB.class) {
                ((org.gsc.db.db2.common.RocksDB) ((SnapshotRoot) (store.getHead().getRoot())).getDb())
                        .getDb().backup(path);
            }
        }
    }

    private void deleteBackup(int i) {
        String path = "";
        if (i == DB_BACKUP_INDEX1) {
            path = args.getDbBackupConfig().getBak1path();
        } else if (i == DB_BACKUP_INDEX2) {
            path = args.getDbBackupConfig().getBak2path();
        } else {
            throw new RuntimeException("Error deleteBackup with undefined index");
        }
        List<RevokingDBWithCachingNewValue> stores = ((SnapshotManager) db).getDbs();
        for (RevokingDBWithCachingNewValue store : stores) {
            if (((SnapshotRoot) (store.getHead().getRoot())).getDb().getClass()
                    == org.gsc.db.db2.common.RocksDB.class) {
                ((org.gsc.db.db2.common.RocksDB) (((SnapshotRoot) (store.getHead().getRoot())).getDb()))
                        .getDb().deleteDbBakPath(path);
            }
        }
    }
}
