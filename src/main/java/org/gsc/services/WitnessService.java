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

import static org.gsc.core.witness.BlockProductionCondition.NOT_MY_TURN;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.joda.time.DateTime;
import org.gsc.application.Application;
import org.gsc.application.Service;
import org.gsc.application.GSCApplicationContext;
import org.gsc.net.backup.BackupManager;
import org.gsc.net.backup.BackupManager.BackupStatusEnum;
import org.gsc.net.backup.BackupServer;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.StringUtil;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.GSCException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.exception.ValidateScheduleException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.net.GSCNetService;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.core.witness.BlockProductionCondition;
import org.gsc.core.witness.WitnessController;

@Slf4j(topic = "witness")
public class WitnessService implements Service {

    private static final int MIN_PARTICIPATION_RATE = Args.getInstance()
            .getMinParticipationRate(); // MIN_PARTICIPATION_RATE * 1%
    private static final int PRODUCE_TIME_OUT = 500; // ms
    @Getter
    private static volatile boolean syncEnabled = Args.getInstance().isSyncEnabled();

    private Application gscApp;
    @Getter
    protected Map<ByteString, WitnessWrapper> localWitnessStateMap = Maps
            .newHashMap(); //  <witnessAccountAddress,WitnessWrapper>
    private Thread generateThread;

    @Getter
    private volatile boolean isRunning = false;
    private Map<ByteString, byte[]> privateKeyMap = Maps
            .newHashMap();//<witnessAccountAddress,privateKey>
    private Map<byte[], byte[]> privateKeyToAddressMap = Maps
            .newHashMap();//<privateKey,witnessPermissionAccountAddress>

    private Manager manager;

    private WitnessController controller;

    private GSCApplicationContext context;

    private BackupManager backupManager;

    private BackupServer backupServer;

    private GSCNetService gscNetService;

    private AtomicInteger dupBlockCount = new AtomicInteger(0);
    private AtomicLong dupBlockTime = new AtomicLong(0);
    private long blockCycle =
            ChainConstant.BLOCK_PRODUCED_INTERVAL * ChainConstant.MAX_ACTIVE_WITNESS_NUM;
    private Cache<ByteString, Long> blocks = CacheBuilder.newBuilder().maximumSize(10).build();

    /**
     * Construction method.
     */
    public WitnessService(Application gscApp, GSCApplicationContext context) {
        this.gscApp = gscApp;
        this.context = context;
        backupManager = context.getBean(BackupManager.class);
        backupServer = context.getBean(BackupServer.class);
        gscNetService = context.getBean(GSCNetService.class);
        generateThread = new Thread(scheduleProductionLoop);
        manager = gscApp.getDbManager();
        manager.setWitnessService(this);
        controller = manager.getWitnessController();
        new Thread(() -> {
            while (syncEnabled) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            backupServer.initServer();
        }).start();
    }

    /**
     * Cycle thread to generate blocks
     */
    private Runnable scheduleProductionLoop =
            () -> {
                if (localWitnessStateMap == null || localWitnessStateMap.keySet().isEmpty()) {
                    logger.error("LocalWitnesses is null");
                    return;
                }

                while (isRunning) {
                    try {
                        if (this.syncEnabled) {
                            Thread.sleep(500L);
                        } else {
                            DateTime time = DateTime.now();
                            long timeToNextSecond = ChainConstant.BLOCK_PRODUCED_INTERVAL
                                    - (time.getSecondOfMinute() * 1000 + time.getMillisOfSecond())
                                    % ChainConstant.BLOCK_PRODUCED_INTERVAL;
                            if (timeToNextSecond < 50L) {
                                timeToNextSecond = timeToNextSecond + ChainConstant.BLOCK_PRODUCED_INTERVAL;
                            }
                            DateTime nextTime = time.plus(timeToNextSecond);
                            logger.debug(
                                    "ProductionLoop sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
                            Thread.sleep(timeToNextSecond);
                        }
                        this.blockProductionLoop();
                    } catch (Throwable throwable) {
                        logger.error("unknown throwable happened in witness loop", throwable);
                    }
                }
            };

    /**
     * Loop to generate blocks
     */
    private void blockProductionLoop() throws InterruptedException {
        BlockProductionCondition result = this.tryProduceBlock();

        if (result == null) {
            logger.warn("Result is null");
            return;
        }

        if (result.ordinal() <= NOT_MY_TURN.ordinal()) {
            logger.debug(result.toString());
        } else {
            logger.info(result.toString());
        }
    }

    /**
     * Generate and broadcast blocks
     */
    private BlockProductionCondition tryProduceBlock() throws InterruptedException {
        logger.info("Try Produce Block");
        long now = DateTime.now().getMillis() + 50L;
        if (this.syncEnabled) {
            long nexSlotTime = controller.getSlotTime(1);
            if (nexSlotTime > now) { // check sync during first loop
                syncEnabled = false;
                Thread.sleep(nexSlotTime - now); //Processing Time Drift later
                now = DateTime.now().getMillis();
            } else {
                logger.debug("Not sync ,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
                        new DateTime(now),
                        new DateTime(this.gscApp.getDbManager().getDynamicPropertiesStore()
                                .getLatestBlockHeaderTimestamp()),
                        this.gscApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
                        this.gscApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
                return BlockProductionCondition.NOT_SYNCED;
            }
        }

        if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
            return BlockProductionCondition.BACKUP_STATUS_IS_NOT_MASTER;
        }

        if (dupWitnessCheck()) {
            return BlockProductionCondition.DUP_WITNESS;
        }

        final int participation = this.controller.calculateParticipationRate();
        if (participation < MIN_PARTICIPATION_RATE) {
            logger.warn(
                    "Participation[" + participation + "] <  MIN_PARTICIPATION_RATE[" + MIN_PARTICIPATION_RATE
                            + "]");

            if (logger.isDebugEnabled()) {
                this.controller.dumpParticipationLog();
            }

            return BlockProductionCondition.LOW_PARTICIPATION;
        }

        if (!controller.activeWitnessesContain(this.getLocalWitnessStateMap().keySet())) {
            logger.info("Unelected. Elected Witnesses: {}",
                    StringUtil.getAddressStringList(controller.getActiveWitnesses()));
            return BlockProductionCondition.UNELECTED;
        }

        try {

            BlockWrapper block;

            synchronized (gscApp.getDbManager()) {
                long slot = controller.getSlotAtTime(now);
                logger.debug("Slot:" + slot);
                if (slot == 0) {
                    logger.info("Not time yet,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
                            new DateTime(now),
                            new DateTime(
                                    this.gscApp.getDbManager().getDynamicPropertiesStore()
                                            .getLatestBlockHeaderTimestamp()),
                            this.gscApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
                            this.gscApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
                    return BlockProductionCondition.NOT_TIME_YET;
                }

                if (now < controller.getManager().getDynamicPropertiesStore()
                        .getLatestBlockHeaderTimestamp()) {
                    logger.warn("have a timestamp:{} less than or equal to the previous block:{}",
                            new DateTime(now), new DateTime(
                                    this.gscApp.getDbManager().getDynamicPropertiesStore()
                                            .getLatestBlockHeaderTimestamp()));
                    return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
                }

                final ByteString scheduledWitness = controller.getScheduledWitness(slot);

                if (!this.getLocalWitnessStateMap().containsKey(scheduledWitness)) {
                    logger.info("It's not my turn, ScheduledWitness[{}],slot[{}],abSlot[{}],",
                            ByteArray.toHexString(scheduledWitness.toByteArray()), slot,
                            controller.getAbSlotAtTime(now));
                    return NOT_MY_TURN;
                }

                long scheduledTime = controller.getSlotTime(slot);

                if (scheduledTime - now > PRODUCE_TIME_OUT) {
                    return BlockProductionCondition.LAG;
                }

                if (!privateKeyMap.containsKey(scheduledWitness)) {
                    return BlockProductionCondition.NO_PRIVATE_KEY;
                }

                controller.getManager().lastHeadBlockIsMaintenance();

                controller.setGeneratingBlock(true);

                block = generateBlock(scheduledTime, scheduledWitness,
                        controller.lastHeadBlockIsMaintenance());

                if (block == null) {
                    logger.warn("exception when generate block");
                    return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
                }

                int blockProducedTimeOut = Args.getInstance().getBlockProducedTimeOut();

                long timeout = Math
                        .min(ChainConstant.BLOCK_PRODUCED_INTERVAL * blockProducedTimeOut / 100 + 500,
                                ChainConstant.BLOCK_PRODUCED_INTERVAL);
                if (DateTime.now().getMillis() - now > timeout) {
                    logger.warn("Task timeout ( > {}ms)，startTime:{},endTime:{}", timeout, new DateTime(now),
                            DateTime.now());
                    gscApp.getDbManager().eraseBlock();
                    return BlockProductionCondition.TIME_OUT;
                }
            }

            logger.info(
                    "Produce block successfully, blockNumber:{}, abSlot[{}], blockId:{}, transactionSize:{}, blockTime:{}, parentBlockId:{}",
                    block.getNum(), controller.getAbSlotAtTime(now), block.getBlockId(),
                    block.getTransactions().size(),
                    new DateTime(block.getTimeStamp()),
                    block.getParentHash());

            broadcastBlock(block);

            return BlockProductionCondition.PRODUCED;
        } catch (GSCException e) {
            logger.error(e.getMessage(), e);
            return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
        } finally {
            controller.setGeneratingBlock(false);
        }
    }

    //Verify that the private key corresponds to the witness permission
    public boolean validateWitnessPermission(ByteString scheduledWitness) {
        if (manager.getDynamicPropertiesStore().getAllowMultiSign() == 1) {
            byte[] privateKey = privateKeyMap.get(scheduledWitness);
            byte[] witnessPermissionAddress = privateKeyToAddressMap.get(privateKey);
            AccountWrapper witnessAccount = manager.getAccountStore()
                    .get(scheduledWitness.toByteArray());
            if (!Arrays.equals(witnessPermissionAddress, witnessAccount.getWitnessPermissionAddress())) {
                return false;
            }
        }
        return true;
    }

    private BlockWrapper generateBlock(long when, ByteString witnessAddress,
                                       Boolean lastHeadBlockIsMaintenance)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException {
        return gscApp.getDbManager().generateBlock(this.localWitnessStateMap.get(witnessAddress), when,
                this.privateKeyMap.get(witnessAddress), lastHeadBlockIsMaintenance, true);
    }

    private boolean dupWitnessCheck() {
        if (dupBlockCount.get() == 0) {
            return false;
        }

        if (System.currentTimeMillis() - dupBlockTime.get() > dupBlockCount.get() * blockCycle) {
            dupBlockCount.set(0);
            return false;
        }

        return true;
    }

    private void broadcastBlock(BlockWrapper block) {
        try {
            gscNetService.broadcast(new BlockMessage(block.getData()));
        } catch (Exception ex) {
            throw new RuntimeException("BroadcastBlock error");
        }
    }

    public void checkDupWitness(BlockWrapper block) {
        if (block.generatedByMyself) {
            blocks.put(block.getBlockId().getByteString(), System.currentTimeMillis());
            return;
        }

        if (blocks.getIfPresent(block.getBlockId().getByteString()) != null){
            return;
        }

        if (syncEnabled) {
            return;
        }

        if (System.currentTimeMillis() - block.getTimeStamp() > ChainConstant.BLOCK_PRODUCED_INTERVAL) {
            return;
        }

        if (!privateKeyMap.containsKey(block.getWitnessAddress())) {
            return;
        }

        if (backupManager.getStatus() != BackupStatusEnum.MASTER) {
            return;
        }

        if (dupBlockCount.get() == 0) {
            dupBlockCount.set(new Random().nextInt(10));
        } else {
            dupBlockCount.set(10);
        }

        dupBlockTime.set(System.currentTimeMillis());

        logger.warn("Dup block produced: {}", block);
    }

    /**
     * Initialize the local witnesses
     */
    @Override
    public void init() {

        if (Args.getInstance().getLocalWitnesses().getPrivateKeys().size() == 0) {
            return;
        }

        byte[] privateKey = ByteArray
                .fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey());
        byte[] witnessAccountAddress = Args.getInstance().getLocalWitnesses()
                .getWitnessAccountAddress();
        //This address does not need to have an account
        byte[] privateKeyAccountAddress = ECKey.fromPrivate(privateKey).getAddress();

        WitnessWrapper witnessWrapper = this.gscApp.getDbManager().getWitnessStore()
                .get(witnessAccountAddress);
        // need handle init witness
        if (null == witnessWrapper) {
            logger.warn("WitnessWrapper[" + witnessAccountAddress + "] is not in witnessStore");
            witnessWrapper = new WitnessWrapper(ByteString.copyFrom(witnessAccountAddress));
        }

        this.privateKeyMap.put(witnessWrapper.getAddress(), privateKey);
        this.localWitnessStateMap.put(witnessWrapper.getAddress(), witnessWrapper);
        this.privateKeyToAddressMap.put(privateKey, privateKeyAccountAddress);
    }

    @Override
    public void init(Args args) {
        //this.privateKey = args.getPrivateKeys();
        init();
    }

    @Override
    public void start() {
        isRunning = true;
        generateThread.start();

    }

    @Override
    public void stop() {
        isRunning = false;
        generateThread.interrupt();
    }
}
