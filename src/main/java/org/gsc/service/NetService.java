package org.gsc.service;

import static org.gsc.config.GscConstants.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.gsc.config.Parameter.NetConstants.MAX_TRX_PER_PEER;
import static org.gsc.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.Time;
import org.gsc.config.Args;
import org.gsc.config.Parameter.NetConstants;
import org.gsc.core.chain.BlockId;
import org.gsc.core.sync.ChainController;
import org.gsc.core.sync.InvToSend;
import org.gsc.core.sync.PeerConnection;
import org.gsc.core.sync.PriorItem;
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
import org.gsc.protos.Protocol.Inventory.InventoryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NetService implements Service{

  @Autowired
  private SyncPool pool;

  @Autowired
  private Args conifg;

  @Autowired
  private ChainController controller;

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

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  private ExecutorService trxsHandlePool = Executors
      .newFixedThreadPool(conifg.getValidateSignThreadNum(),
          new ThreadFactoryBuilder()
              .setNameFormat("TrxsHandlePool-%d").build());

  //public
  //TODO:need auto erase oldest block

  private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
    @Override
    public boolean offer(BlockId blockId) {
      if (size() > 200) {
        super.poll();
      }
      return super.offer(blockId);
    }
  };

  private ConcurrentHashMap<Sha256Hash, PeerConnection> syncMap = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Sha256Hash, PeerConnection> fetchMap = new ConcurrentHashMap<>();

  //private NodeDelegate del;

  private volatile boolean isAdvertiseActive;

  private volatile boolean isFetchActive;


  private ScheduledExecutorService disconnectInactiveExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private ScheduledExecutorService cleanInventoryExecutor = Executors
      .newSingleThreadScheduledExecutor();

  //broadcast
  private ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = new ConcurrentHashMap<>();

  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  //private ConcurrentHashMap<Sha256Hash, InventoryType> advObjToFetch = new ConcurrentHashMap<>();

  //private ConcurrentLinkedQueue<PriorItem> advObjToFetch = new ConcurrentLinkedQueue<PriorItem>();

  private ConcurrentHashMap<Sha256Hash, PriorItem> advObjToFetch = new ConcurrentHashMap<Sha256Hash, PriorItem>();

  private ExecutorService broadPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "broad-msg-");
    }
  });

  private HashMap<Sha256Hash, Long> badAdvObj = new HashMap<>(); //TODO:need auto erase oldest obj

  //blocks we requested but not received

  private Cache<BlockId, Long> syncBlockIdWeRequested = CacheBuilder.newBuilder()
      .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(10000)
      .recordStats().build();

  private Long unSyncNum = 0L;

  private Thread handleSyncBlockLoop;

  private Map<BlockMessage, PeerConnection> blockWaitToProc = new ConcurrentHashMap<>();

  private Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();

//  private ExecutorLoop<SyncMessage> loopSyncBlockChain;
//
//  private ExecutorLoop<FetchMessage> loopFetchBlocks;
//
//  private ExecutorLoop<Message> loopAdvertiseInv;

  private ExecutorService handleBackLogBlocksPool = Executors.newCachedThreadPool();


  private ScheduledExecutorService fetchSyncBlocksExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private ScheduledExecutorService handleSyncBlockExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private ScheduledExecutorService fetchWaterLineExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private volatile boolean isHandleSyncBlockActive = false;

  private AtomicLong fetchSequenceCounter = new AtomicLong(0L);

  private volatile boolean isSuspendFetch = false;

  private volatile boolean isFetchSyncActive = false;

  @Override
  public void init() {

  }

  @Override
  public void init(Args args) {
    //TODO need refatore
    //pool.init(this);
    isAdvertiseActive = true;
    isFetchActive = true;
    activeTronPump();
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {
    //TODO
    //getActivePeer().forEach(peer -> disconnectPeer(peer, ReasonCode.REQUESTED));
  }

  //TODO: override
  public void setChainController(ChainController controller) {
    this.controller = controller;
  }

  // for test only
  public void setPool(SyncPool pool) {
    this.pool = pool;
  }

  /**
   * broadcast msg.
   *
   * @param msg msg to broadcast
   */
  public void broadcast(GscMessage msg) {
    InventoryType type;
    if (msg instanceof BlockMessage) {
      logger.info("Ready to broadcast block {}", ((BlockMessage) msg).getBlockId());
      freshBlockId.offer(((BlockMessage) msg).getBlockId());
      BlockCache.put(msg.getMessageId(), (BlockMessage) msg);
      type = InventoryType.BLOCK;
    } else if (msg instanceof TransactionMessage) {
      TrxCache.put(msg.getMessageId(), (TransactionMessage) msg);
      type = InventoryType.TRX;
    } else {
      return;
    }
    synchronized (advObjToSpread) {
      advObjToSpread.put(msg.getMessageId(), type);
    }
  }


  private void activeTronPump() {
    // broadcast inv
    //TODO
//    loopAdvertiseInv = new ExecutorLoop<>(2, 10, b -> {
//      //logger.info("loop advertise inv");
//      for (PeerConnection peer : getActivePeer()) {
//        if (!peer.isNeedSyncFromUs()) {
//          logger.info("Advertise adverInv to " + peer);
//          peer.sendMessage(b);
//        }
//      }
//    }, throwable -> logger.error("Unhandled exception: ", throwable));
//
//    // fetch blocks
//    loopFetchBlocks = new ExecutorLoop<>(2, 10, c -> {
//      logger.info("loop fetch blocks");
//      if (fetchMap.containsKey(c.getMessageId())) {
//        fetchMap.get(c.getMessageId()).sendMessage(c);
//      }
//    }, throwable -> logger.error("Unhandled exception: ", throwable));
//
//    // sync block chain
//    loopSyncBlockChain = new ExecutorLoop<>(2, 10, d -> {
//      //logger.info("loop sync block chain");
//      if (syncMap.containsKey(d.getMessageId())) {
//        syncMap.get(d.getMessageId()).sendMessage(d);
//      }
//    }, throwable -> logger.error("Unhandled exception: ", throwable));

    broadPool.submit(() -> {
      while (isAdvertiseActive) {
        //TODO
        //consumerAdvObjToSpread();
      }
    });

    broadPool.submit(() -> {
      while (isFetchActive) {
        consumerAdvObjToFetch();
      }
    });

    //TODO: wait to refactor these threads.
    //handleSyncBlockLoop.start();

    handleSyncBlockExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (isHandleSyncBlockActive) {
          isHandleSyncBlockActive = false;
          //Thread handleSyncBlockThread = new Thread(() -> handleSyncBlock());
          //TODO
          //handleSyncBlock();
        }
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 10, 1, TimeUnit.SECONDS);

    //terminate inactive loop
    disconnectInactiveExecutor.scheduleWithFixedDelay(() -> {
      try {
        //TODO
        //disconnectInactive();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 30000, BLOCK_PRODUCED_INTERVAL / 2, TimeUnit.MILLISECONDS);

    logExecutor.scheduleWithFixedDelay(() -> {
      try {
        //TODO
        //logNodeStatus();
      } catch (Throwable t) {
        logger.error("Exception in log worker", t);
      }
    }, 10, 10, TimeUnit.SECONDS);

    cleanInventoryExecutor.scheduleWithFixedDelay(() -> {
      try {
        getActivePeer().forEach(p -> p.cleanInvGarbage());
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 2, NetConstants.MAX_INVENTORY_SIZE_IN_MINUTES / 2, TimeUnit.MINUTES);

    fetchSyncBlocksExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (isFetchSyncActive) {
          if (!isSuspendFetch) {
            //TODO
       //     startFetchSyncBlock();
          } else {
            logger.debug("suspend");
          }
        }
        isFetchSyncActive = false;
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 10, 1, TimeUnit.SECONDS);

    //fetchWaterLine:
    fetchWaterLineExecutor.scheduleWithFixedDelay(() -> {
      try {
        fetchWaterLine.advance();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 1000, 100, TimeUnit.MILLISECONDS);
  }

  private void consumerAdvObjToFetch() {
    Collection<PeerConnection> filterActivePeer = getActivePeer().stream()
        .filter(peer -> !peer.isBusy()).collect(Collectors.toList());

    if (advObjToFetch.isEmpty() || filterActivePeer.isEmpty()) {
      try {
        Thread.sleep(100);
        return;
      } catch (InterruptedException e) {
        logger.debug(e.getMessage(), e);
      }
    }
    InvToSend sendPackage = new InvToSend();
    long now = Time.getCurrentMillis();
    advObjToFetch.values().stream().sorted(PriorItem::compareTo).forEach(idToFetch -> {
      Sha256Hash hash = idToFetch.getHash();
      if (idToFetch.getTime() < now - MSG_CACHE_DURATION_IN_BLOCKS * BLOCK_PRODUCED_INTERVAL) {
        logger.info("This obj is too late to fetch: " + idToFetch);
        advObjToFetch.remove(hash);
        return;
      }
      filterActivePeer.stream()
          .filter(peer -> peer.getAdvObjSpreadToUs().containsKey(hash)
              && sendPackage.getSize(peer) < MAX_TRX_PER_PEER)
          .sorted(Comparator.comparingInt(peer -> sendPackage.getSize(peer)))
          .findFirst().ifPresent(peer -> {
        sendPackage.add(idToFetch, peer);
        peer.getAdvObjWeRequested().put(idToFetch.getItem(), now);
        advObjToFetch.remove(hash);
      });
    });

    sendPackage.sendFetch();
  }

  private Collection<PeerConnection> getActivePeer() {
    return pool.getActivePeers();
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
