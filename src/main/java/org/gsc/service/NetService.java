package org.gsc.service;

import static org.gsc.config.GscConstants.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.gsc.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.config.Args;
import org.gsc.core.sync.SlidingWindowCounter;
import org.gsc.net.gsc.Gsc;
import org.gsc.net.message.gsc.AttentionMessage;
import org.gsc.net.message.gsc.BlockMessage;
import org.gsc.net.message.gsc.FetchMessage;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.message.gsc.SyncMessage;
import org.gsc.net.message.gsc.TimeMessage;
import org.gsc.net.message.gsc.TransactionMessage;
import org.gsc.net.server.SyncPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NetService implements Service{

  @Autowired
  private SyncPool pool;

  private Cache<Sha256Hash, TransactionMessage> TrxCache = CacheBuilder.newBuilder()
      .maximumSize(100_000).expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(100_000)
      .recordStats().build();

  private Cache<Sha256Hash, BlockMessage> BlockCache = CacheBuilder.newBuilder()
      .maximumSize(10).expireAfterWrite(60, TimeUnit.SECONDS)
      .recordStats().build();

  private SlidingWindowCounter fetchWaterLine =
      new SlidingWindowCounter(BLOCK_PRODUCED_INTERVAL * MSG_CACHE_DURATION_IN_BLOCKS / 100);

  private int maxTrxsSize = 1_000_000;

  private int maxTrxsCnt = 100;

  @Override
  public void init() {

  }

  @Override
  public void init(Args args) {

  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }

  public void broadcast(GscMessage msg) {

  }

  public void handleMessage(Gsc gsc, GscMessage msg) {

  }

  public void handleMessage(Gsc gsc, BlockMessage msg) {
    logger.info("get block message from " + gsc.getPeer());
  }

  public void handleMessage(Gsc gsc, TransactionMessage msg) {
    logger.info("get tx message from " + gsc.getPeer());
  }

  public void handleMessage(Gsc gsc, FetchMessage msg) {
    logger.info("get fetch message from " + gsc.getPeer());
  }

  public void handleMessage(Gsc gsc, SyncMessage msg) {
    logger.info("get sync message from " + gsc.getPeer());
  }

  public void handleMessage(Gsc gsc, TimeMessage msg) {
    logger.info("get time message from " + gsc.getPeer());
  }

  public void handleMessage(Gsc gsc, AttentionMessage msg) {
    logger.info("get attention message from " + gsc.getPeer());
  }

}
