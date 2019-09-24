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

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.gsc.core.wrapper.BlockWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.gsc.net.backup.BackupManager;
import org.gsc.net.backup.BackupManager.BackupStatusEnum;
import org.gsc.config.args.Args;

@Slf4j
@Aspect
public class BackupRocksDBAspect {
    @Autowired
    private BackupDbUtil util;

    @Autowired
    private BackupManager backupManager;


    @Pointcut("execution(** org.gsc.core.db.Manager.pushBlock(..)) && args(block)")
    public void pointPushBlock(BlockWrapper block) {

    }

    @Before("pointPushBlock(block)")
    public void backupDb(BlockWrapper block) {
        //SR-Master Node do not backup db;
        if (Args.getInstance().isWitness() && backupManager.getStatus() != BackupStatusEnum.SLAVER) {
            return;
        }

        //backup db when reach frequency.
        if (block.getNum() % Args.getInstance().getDbBackupConfig().getFrequency() == 0) {
            try {
                util.doBackup(block);
            } catch (Exception e) {
                logger.error("backup db failure:", e);
            }
        }
    }

    @AfterThrowing("pointPushBlock(block)")
    public void logErrorPushBlock(BlockWrapper block) {
        logger.info("AfterThrowing pushBlock");
    }
}