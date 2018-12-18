package org.gsc.services;

import static org.gsc.consensus.BlockProductionCondition.NOT_MY_TURN;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;

import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.gsc.common.application.Application;
import org.gsc.common.application.Service;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.common.backup.BackupManager;
import org.gsc.common.backup.BackupManager.BackupStatusEnum;
import org.gsc.common.backup.BackupServer;
import org.gsc.crypto.ECKey;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptException;
import org.gsc.core.exception.TransactionTraceException;
import org.gsc.core.exception.GSCException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.exception.ValidateScheduleException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.net.message.BlockMessage;
import org.gsc.consensus.BlockProductionCondition;
import org.gsc.core.witness.WitnessController;

@Slf4j
public class WitnessService implements Service {

    private static final int MIN_PARTICIPATION_RATE = Args.getInstance()
            .getMinParticipationRate(); // MIN_PARTICIPATION_RATE * 1%
    private static final int PRODUCE_TIME_OUT = 500; // ms
    private Application gscApp;
    @Getter
    protected Map<ByteString, WitnessWrapper> localWitnessStateMap = Maps
            .newHashMap(); //  <address,WitnessWrapper>
    private Thread generateThread;

    private volatile boolean isRunning = false;
    private Map<ByteString, byte[]> privateKeyMap = Maps.newHashMap();
    private volatile boolean needSyncCheck = Args.getInstance().isNeedSyncCheck();

    private WitnessController controller;

    private GSCApplicationContext context;

    private BackupManager backupManager;

    private BackupServer backupServer;

    /**
     * Construction method.
     */
    public WitnessService(Application gscApp, GSCApplicationContext context) {
        this.gscApp = gscApp;
        this.context = context;
        backupManager = context.getBean(BackupManager.class);
        backupServer = context.getBean(BackupServer.class);
        generateThread = new Thread(scheduleProductionLoop);
        controller = gscApp.getDbManager().getWitnessController();
        new Thread(() -> {
            while (needSyncCheck) {
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
                        if (this.needSyncCheck) {
                            Thread.sleep(500L);
                        } else {
                            DateTime time = DateTime.now();
                            long timeToNextSecond = ChainConstant.BLOCK_PRODUCED_INTERVAL
                                    - (time.getSecondOfMinute() * 1000 + time.getMillisOfSecond())
                                    % ChainConstant.BLOCK_PRODUCED_INTERVAL; // 3000 - (second*1000 + ms)%3000
                            if (timeToNextSecond < 50L) {
                                timeToNextSecond = timeToNextSecond + ChainConstant.BLOCK_PRODUCED_INTERVAL;
                            }
                            DateTime nextTime = time.plus(timeToNextSecond);
                            logger.debug(
                                    "ProductionLoop sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
                            Thread.sleep(timeToNextSecond);
                        }
                        this.blockProductionLoop();
                    } catch (InterruptedException ex) {
                        logger.info("ProductionLoop interrupted");
                    } catch (Exception ex) {
                        logger.error("unknown exception happened in witness loop", ex);
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
        if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
            return BlockProductionCondition.BACKUP_STATUS_IS_NOT_MASTER;
        }
        long now = DateTime.now().getMillis() + 50L;
        if (this.needSyncCheck) { // check node has full sync
            long nextSlotTime = controller.getSlotTime(1);
            if (nextSlotTime > now) { // check sync during first loop
                needSyncCheck = false;
                Thread.sleep(nextSlotTime - now); //Processing Time Drift later
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

        final int participation = this.controller.calculateParticipationRate(); //check node could participate or not
        if (participation < MIN_PARTICIPATION_RATE) {
            logger.warn(
                    "Participation[" + participation + "] <  MIN_PARTICIPATION_RATE[" + MIN_PARTICIPATION_RATE
                            + "]");

            if (logger.isDebugEnabled()) {
                this.controller.dumpParticipationLog();
            }

            return BlockProductionCondition.LOW_PARTICIPATION;
        }

        long slot = controller.getSlotAtTime(now);
        logger.debug("Slot:" + slot);

        if (slot == 0) { //check until time or not
            logger.info("Not time yet,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
                    new DateTime(now),
                    new DateTime(
                            this.gscApp.getDbManager().getDynamicPropertiesStore()
                                    .getLatestBlockHeaderTimestamp()),
                    this.gscApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
                    this.gscApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
            return BlockProductionCondition.NOT_TIME_YET;
        }
        //check time now and latest time on block
        if (now < controller.getManager().getDynamicPropertiesStore().getLatestBlockHeaderTimestamp()) {
            logger.warn("have a timestamp:{} less than or equal to the previous block:{}",
                    new DateTime(now), new DateTime(
                            this.gscApp.getDbManager().getDynamicPropertiesStore()
                                    .getLatestBlockHeaderTimestamp()));
            return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
        }

        // check if belongs witness active list
        if (!controller.activeWitnessesContain(this.getLocalWitnessStateMap().keySet())) {
            logger.info("Unelected. Elected Witnesses: {}",
                    StringUtil.getAddressStringList(controller.getActiveWitnesses()));
            return BlockProductionCondition.UNELECTED;
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

        try {
            controller.setGeneratingBlock(true);
            BlockWrapper block = generateBlock(scheduledTime, scheduledWitness);

            if (block == null) {
                logger.warn("exception when generate block");
                return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
            }
            if (DateTime.now().getMillis() - now
                    > ChainConstant.BLOCK_PRODUCED_INTERVAL * ChainConstant.BLOCK_PRODUCED_TIME_OUT / 100) {
                logger.warn("Task timeout ( > {}ms)，startTime:{},endTime:{}",
                        ChainConstant.BLOCK_PRODUCED_INTERVAL * ChainConstant.BLOCK_PRODUCED_TIME_OUT / 100,
                        new DateTime(now), DateTime.now());
                return BlockProductionCondition.TIME_OUT;
            }

            logger.info(
                    "Produce block successfully, blockNumber:{}, abSlot[{}], blockId:{}, transactionSize:{}, blockTime:{}, parentBlockId:{}",
                    block.getNum(), controller.getAbSlotAtTime(now), block.getBlockId(),
                    block.getTransactions().size(),
                    new DateTime(block.getTimeStamp()),
                    this.gscApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
            broadcastBlock(block);

            return BlockProductionCondition.PRODUCED;
        } catch (GSCException e) {
            logger.error(e.getMessage(), e);
            return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
        } finally {
            controller.setGeneratingBlock(false);
        }
    }

    private BlockWrapper generateBlock(long when, ByteString witnessAddress)
            throws ValidateSignatureException, ContractValidateException, ContractExeException, UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException, ReceiptException, TransactionTraceException {
        return gscApp.getDbManager().generateBlock(this.localWitnessStateMap.get(witnessAddress), when,
                this.privateKeyMap.get(witnessAddress));
    }
    
    private void broadcastBlock(BlockWrapper block) {
        try {
            gscApp.getP2pNode().broadcast(new BlockMessage(block.getData()));
        } catch (Exception ex) {
            throw new RuntimeException("BroadcastBlock error");
        }
    }

    /**
     * Initialize the local witnesses
     */
    @Override
    public void init() {
        Args.getInstance().getLocalWitnesses().getPrivateKeys().forEach(key -> {
            byte[] privateKey = ByteArray.fromHexString(key);
            final ECKey ecKey = ECKey.fromPrivate(privateKey);
            byte[] address = ecKey.getAddress();
            WitnessWrapper witnessCapsule = this.gscApp.getDbManager().getWitnessStore()
                    .get(address);
            // need handle init witness
            if (null == witnessCapsule) {
                logger.warn("WitnessWrapper[" + address + "] is not in witnessStore");
                witnessCapsule = new WitnessWrapper(ByteString.copyFrom(address));
            }

            this.privateKeyMap.put(witnessCapsule.getAddress(), privateKey);
            this.localWitnessStateMap.put(witnessCapsule.getAddress(), witnessCapsule);
        });
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
