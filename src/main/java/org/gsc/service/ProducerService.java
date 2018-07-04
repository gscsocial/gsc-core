package org.gsc.service;

import static org.gsc.consensus.BlockProductionCondition.NOT_MY_TURN;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.app.Application;
import org.gsc.common.exception.AccountResourceInsufficientException;
import org.gsc.common.exception.ContractExeException;
import org.gsc.common.exception.ContractValidateException;
import org.gsc.common.exception.GscException;
import org.gsc.common.exception.UnLinkedBlockException;
import org.gsc.common.exception.ValidateScheduleException;
import org.gsc.common.exception.ValidateSignatureException;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.StringUtil;
import org.gsc.config.Args;
import org.gsc.config.GscConstants.ChainConstant;
import org.gsc.consensus.BlockProductionCondition;
import org.gsc.consensus.ProducerController;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.ProducerWrapper;
import org.gsc.crypto.ECKey;
import org.gsc.db.Manager;
import org.gsc.net.message.gsc.BlockMessage;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class ProducerService implements Service {

  @Autowired
  private static Args config;

  @Autowired
  private static Manager manger;

  @Autowired
  private NetService netService;

  private static final int MIN_PARTICIPATION_RATE = config
      .getMinParticipationRate(); // MIN_PARTICIPATION_RATE * 1%
  private static final int PRODUCE_TIME_OUT = 500; // ms
  private Application app;
  @Getter
  protected Map<ByteString, ProducerWrapper> localWitnessStateMap = Maps
      .newHashMap(); //  <address,WitnessWrapper>
  private Thread generateThread;
  private volatile boolean isRunning = false;
  private Map<ByteString, byte[]> privateKeyMap = Maps.newHashMap();
  private volatile boolean needSyncCheck = config.isNeedSyncCheck();

  private ProducerController controller;

  private AnnotationConfigApplicationContext context;

  //TODO backup
//  private BackupManager backupManager;
//
//  private BackupServer backupServer;

  /**
   * Construction method.
   */
  public ProducerService(Application app, AnnotationConfigApplicationContext context) {
    this.app = app;
    this.context = context;
//    backupManager = context.getBean(BackupManager.class);
//    backupServer = context.getBean(BackupServer.class);
    generateThread = new Thread(scheduleProductionLoop);
    controller = app.getDbManager().getProdController();
    new Thread(()->{
      while (needSyncCheck){
        try{
          Thread.sleep(100);
        }catch (Exception e){}
      }
      //TODO bakcup
      //backupServer.initServer();
    }).start();
  }

  /**
   * Cycle thread to generate blocks
   */
  private Runnable scheduleProductionLoop =
      () -> {
        if (localWitnessStateMap == null || localWitnessStateMap.keySet().size() == 0) {
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
    //TODO back up
//    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)){
//      return BlockProductionCondition.BACKUP_STATUS_IS_NOT_MASTER;
//    }
    long now = DateTime.now().getMillis() + 50L;
    if (this.needSyncCheck) {
      long nexSlotTime = controller.getSlotTime(1);
      if (nexSlotTime > now) { // check sync during first loop
        needSyncCheck = false;
        Thread.sleep(nexSlotTime - now); //Processing Time Drift later
        now = DateTime.now().getMillis();
      } else {
        logger.debug("Not sync ,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
            new DateTime(now),
            new DateTime(this.app.getDbManager().getGlobalPropertiesStore()
                .getLatestBlockHeaderTimestamp()),
            this.app.getDbManager().getGlobalPropertiesStore().getLatestBlockHeaderNumber(),
            this.app.getDbManager().getGlobalPropertiesStore().getLatestBlockHeaderHash());
        return BlockProductionCondition.NOT_SYNCED;
      }
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

    long slot = controller.getSlotAtTime(now);
    logger.debug("Slot:" + slot);

    if (slot == 0) {
      logger.info("Not time yet,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
          new DateTime(now),
          new DateTime(
              this.app.getDbManager().getGlobalPropertiesStore()
                  .getLatestBlockHeaderTimestamp()),
          this.app.getDbManager().getGlobalPropertiesStore().getLatestBlockHeaderNumber(),
          this.app.getDbManager().getGlobalPropertiesStore().getLatestBlockHeaderHash());
      return BlockProductionCondition.NOT_TIME_YET;
    }

    if (now < manger.getGlobalPropertiesStore().getLatestBlockHeaderTimestamp()) {
      logger.warn("have a timestamp:{} less than or equal to the previous block:{}",
          new DateTime(now), new DateTime(
              this.app.getDbManager().getGlobalPropertiesStore()
                  .getLatestBlockHeaderTimestamp()));
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    }

    if (!controller.activeWitnessesContain(this.getLocalWitnessStateMap().keySet())) {
      logger.info("Unelected. Elected Witnesses: {}",
          StringUtil.getAddressStringList(controller.getActiveProducers()));
      return BlockProductionCondition.UNELECTED;
    }

    final ByteString scheduledWitness = controller.getScheduledProducer(slot);

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
          > ChainConstant.BLOCK_PRODUCED_INTERVAL * ChainConstant.BLOCK_PRODUCED_TIME_OUT) {
        logger.warn("Task timeout ( > {}ms)，startTime:{},endTime:{}",
            ChainConstant.BLOCK_PRODUCED_INTERVAL * ChainConstant.BLOCK_PRODUCED_TIME_OUT,
            new DateTime(now), DateTime.now());
        return BlockProductionCondition.TIME_OUT;
      }

      logger.info(
          "Produce block successfully, blockNumber:{}, abSlot[{}], blockId:{}, transactionSize:{}, blockTime:{}, parentBlockId:{}",
          block.getNum(), controller.getAbSlotAtTime(now), block.getBlockId(),
          block.getTransactions().size(),
          new DateTime(block.getTimeStamp()),
          this.app.getDbManager().getGlobalPropertiesStore().getLatestBlockHeaderHash());
      broadcastBlock(block);

      return BlockProductionCondition.PRODUCED;
    } catch (GscException e) {
      logger.error(e.getMessage(), e);
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    } finally {
      controller.setGeneratingBlock(false);
    }

  }

  private void broadcastBlock(BlockWrapper block) {
    try {
      netService.broadcast(new BlockMessage(block.getData()));
    } catch (Exception ex) {
      throw new RuntimeException("BroadcastBlock error");
    }
  }

  private BlockWrapper generateBlock(long when, ByteString witnessAddress)
      throws ValidateSignatureException, ContractValidateException, ContractExeException, UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException {
    return app.getDbManager().generateBlock(this.localWitnessStateMap.get(witnessAddress), when,
        this.privateKeyMap.get(witnessAddress));
  }

  /**
   * Initialize the local witnesses
   */
  @Override
  public void init() {
    config.getLocalWitnesses().getPrivateKeys().forEach(key -> {
      byte[] privateKey = ByteArray.fromHexString(key);
      final ECKey ecKey = ECKey.fromPrivate(privateKey);
      byte[] address = ecKey.getAddress();
      ProducerWrapper witnessWrapper = this.app.getDbManager().getProdStore()
          .get(address);
      // need handle init witness
      if (null == witnessWrapper) {
        logger.warn("WitnessWrapper[" + address + "] is not in witnessStore");
        witnessWrapper = new ProducerWrapper(ByteString.copyFrom(address));
      }

      this.privateKeyMap.put(witnessWrapper.getAddress(), privateKey);
      this.localWitnessStateMap.put(witnessWrapper.getAddress(), witnessWrapper);
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
