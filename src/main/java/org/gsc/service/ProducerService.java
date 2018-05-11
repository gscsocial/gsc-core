package org.gsc.service;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.exception.ContractExeException;
import org.gsc.common.exception.ContractValidateException;
import org.gsc.common.exception.UnLinkedBlockException;
import org.gsc.common.exception.ValidateScheduleException;
import org.gsc.common.exception.ValidateSignatureException;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Args;
import org.gsc.consensus.BlockProductionCondition;
import org.gsc.consensus.Producer;
import org.gsc.core.chain.BlockWrapper;
import org.gsc.crypto.ECKey;
import org.gsc.net.message.gsc.BlockMessage;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProducerService implements Service{

  //private static final int MIN_PARTICIPATION_RATE = Args.getInstance().getMinParticipationRate(); // MIN_PARTICIPATION_RATE * 1%
  private static final int PRODUCE_TIME_OUT = 500; // ms
  @Getter
  protected Map<ByteString, Producer> localWitnessStateMap = Maps
      .newHashMap(); //  <address,WitnessCapsule>
  private Thread generateThread;
  private volatile boolean isRunning = false;
  private Map<ByteString, byte[]> privateKeyMap = Maps.newHashMap();
  //private boolean needSyncCheck = Args.getInstance().isNeedSyncCheck();
  boolean needSyncCheck;

  @Autowired
  private ProducerService controller;

  /**
   * Construction method.
   */
  public ProducerService() {
    generateThread = new Thread(scheduleProductionLoop);
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

    switch (result) {
      case PRODUCED:
        logger.debug("Produced");
        break;
      case NOT_SYNCED:
        logger.info("Not sync");
        break;
      case NOT_MY_TURN:
        logger.debug("It's not my turn");
        break;
      case NOT_TIME_YET:
        logger.info("Not time yet");
        break;
      case NO_PRIVATE_KEY:
        logger.info("No pri key");
        break;
      case LOW_PARTICIPATION:
        logger.info("Low part");
        break;
      case LAG:
        logger.info("Lag");
        break;
      case CONSECUTIVE:
        logger.info("Consecutive");
        break;
      case TIME_OUT:
        logger.debug("Time out");
      case EXCEPTION_PRODUCING_BLOCK:
        logger.info("Exception");
        break;
      default:
        break;
    }
  }

  /**
   * Generate and broadcast blocks
   */
  private BlockProductionCondition tryProduceBlock() throws InterruptedException {
    return BlockProductionCondition.NOT_SYNCED;
  }

  private void broadcastBlock(BlockWrapper block) {

  }

  /**
   * Initialize the local witnesses
   */
  @Override
  public void init() {

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
