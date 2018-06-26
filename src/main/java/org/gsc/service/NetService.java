package org.gsc.service;

import static org.gsc.config.GscConstants.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.gsc.config.Parameter.NetConstants.MAX_TRX_PER_PEER;
import static org.gsc.config.Parameter.NetConstants.MSG_CACHE_DURATION_IN_BLOCKS;
import static org.gsc.config.Parameter.NetConstants.NET_MAX_TRX_PER_SECOND;
import static org.gsc.config.Parameter.NodeConstant.MAX_BLOCKS_ALREADY_FETCHED;
import static org.gsc.config.Parameter.NodeConstant.MAX_BLOCKS_IN_PROCESS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.Time;
import org.gsc.config.Args;
import org.gsc.config.Parameter.NetConstants;
import org.gsc.config.Parameter.NodeConstant;
import org.gsc.core.chain.BlockId;
import org.gsc.core.sync.ChainController;
import org.gsc.core.sync.InvToSend;
import org.gsc.core.sync.Item;
import org.gsc.core.sync.PeerConnection;
import org.gsc.core.sync.PriorItem;
import org.gsc.core.sync.SlidingWindowCounter;
import org.gsc.net.message.gsc.BlockMessage;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.message.gsc.InventoryMessage;
import org.gsc.net.message.gsc.TransactionMessage;
import org.gsc.net.server.SyncPool;
import org.gsc.protos.P2p.ReasonCode;
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

  private void consumerAdvObjToSpread() {
    if (advObjToSpread.isEmpty()) {
      try {
        Thread.sleep(100);
        return;
      } catch (InterruptedException e) {
        logger.debug(e.getMessage(), e);
      }
    }
    InvToSend sendPackage = new InvToSend();
    HashMap<Sha256Hash, InventoryType> spread = new HashMap<>();
    synchronized (advObjToSpread) {
      spread.putAll(advObjToSpread);
      advObjToSpread.clear();
    }
    getActivePeer().stream()
        .filter(peer -> !peer.isNeedSyncFromUs())
        .forEach(peer ->
            spread.entrySet().stream()
                .filter(idToSpread ->
                    !peer.getAdvObjSpreadToUs().containsKey(idToSpread.getKey())
                        && !peer.getAdvObjWeSpread().containsKey(idToSpread.getKey()))
                .forEach(idToSpread -> {
                  peer.getAdvObjWeSpread().put(idToSpread.getKey(), Time.getCurrentMillis());
                  sendPackage.add(idToSpread, peer);
                }));
    sendPackage.sendInv();
  }

  private synchronized void handleSyncBlock() {
    if (((ThreadPoolExecutor) handleBackLogBlocksPool).getActiveCount() > MAX_BLOCKS_IN_PROCESS) {
      logger.info("we're already processing too many blocks");
      return;
    } else if (isSuspendFetch) {
      isSuspendFetch = false;
    }

    final boolean[] isBlockProc = {true};

    while (isBlockProc[0]) {

      isBlockProc[0] = false;

      synchronized (blockJustReceived) {
        blockWaitToProc.putAll(blockJustReceived);
        blockJustReceived.clear();
      }

      blockWaitToProc.forEach((msg, peerConnection) -> {

        if (peerConnection.isDisconnect()) {
          logger.error("Peer {} is disconnect, drop block {}", peerConnection.getNode().getHost(),
              msg.getBlockId().getString());
          blockWaitToProc.remove(msg);
          syncBlockIdWeRequested.invalidate(msg.getBlockId());
          isFetchSyncActive = true;
          return;
        }

        synchronized (freshBlockId) {
          final boolean[] isFound = {false};
          getActivePeer().stream()
              .filter(
                  peer -> !peer.getSyncBlockToFetch().isEmpty() && peer.getSyncBlockToFetch().peek()
                      .equals(msg.getBlockId()))
              .forEach(peer -> {
                peer.getSyncBlockToFetch().pop();
                peer.getBlockInProc().add(msg.getBlockId());
                isFound[0] = true;
              });
          if (isFound[0]) {
            blockWaitToProc.remove(msg);
            isBlockProc[0] = true;
            //TODO
//            if (freshBlockId.contains(msg.getBlockId()) || processSyncBlock(
//                msg.getBlockCapsule())) {
//              finishProcessSyncBlock(msg.getBlockCapsule());
//            }
          }
        }
      });

      if (((ThreadPoolExecutor) handleBackLogBlocksPool).getActiveCount() > MAX_BLOCKS_IN_PROCESS) {
        logger.info("we're already processing too many blocks");
        if (blockWaitToProc.size() >= MAX_BLOCKS_ALREADY_FETCHED) {
          isSuspendFetch = true;
        }
        break;
      }

    }
  }

  private synchronized void logNodeStatus() {
    StringBuilder sb = new StringBuilder("LocalNode stats:\n");
    sb.append("============\n");

    sb.append(String.format(
        "MyHeadBlockNum: %d\n"
            + "advObjToSpread: %d\n"
            + "advObjToFetch: %d\n"
            + "advObjWeRequested: %d\n"
            + "unSyncNum: %d\n"
            + "blockWaitToProc: %d\n"
            + "blockJustReceived: %d\n"
            + "syncBlockIdWeRequested: %d\n"
            + "badAdvObj: %d\n",
        controller.getHeadBlockId().getNum(),
        advObjToSpread.size(),
        advObjToFetch.size(),
        advObjWeRequested.size(),
        getUnSyncNum(),
        blockWaitToProc.size(),
        blockJustReceived.size(),
        syncBlockIdWeRequested.size(),
        badAdvObj.size()
    ));

    logger.info(sb.toString());
  }

  private long getUnSyncNum() {
    if (getActivePeer().isEmpty()) {
      return 0;
    }
    return getActivePeer().stream()
        .mapToLong(peer -> peer.getUnfetchSyncNum() + peer.getSyncBlockToFetch().size())
        .max()
        .getAsLong();
  }

  public synchronized void disconnectInactive() {
    //logger.debug("size of activePeer: " + getActivePeer().size());
    getActivePeer().forEach(peer -> {
      final boolean[] isDisconnected = {false};
      final ReasonCode[] reasonCode = {ReasonCode.USER_REASON};

      peer.getAdvObjWeRequested().values().stream()
          .filter(time -> time < Time.getCurrentMillis() - NetConstants.ADV_TIME_OUT)
          .findFirst().ifPresent(time -> {
        isDisconnected[0] = true;
        reasonCode[0] = ReasonCode.FETCH_FAIL;
      });

      if (!isDisconnected[0]) {
        peer.getSyncBlockRequested().values().stream()
            .filter(time -> time < Time.getCurrentMillis() - NetConstants.SYNC_TIME_OUT)
            .findFirst().ifPresent(time -> {
          isDisconnected[0] = true;
          reasonCode[0] = ReasonCode.SYNC_FAIL;
        });
      }

//    TODO:optimize disconnect null connection

      if (isDisconnected[0]) {
        disconnectPeer(peer, ReasonCode.TIME_OUT);
      }
    });
  }


  public void handleMessage(PeerConnection peer, InventoryMessage msg) {
    for (Sha256Hash id : msg.getHashList()) {
      if (msg.getInventoryType().equals(InventoryType.TRX) && TrxCache.getIfPresent(id) != null) {
        logger.info("{} {} from peer {} Already exist.", msg.getInventoryType(), id,
            peer.getNode().getHost());
        continue;
      }
      final boolean[] spreaded = {false};
      final boolean[] requested = {false};
      getActivePeer().forEach(p -> {
        if (p.getAdvObjWeSpread().containsKey(id)) {
          spreaded[0] = true;
        }
        if (p.getAdvObjWeRequested().containsKey(new Item(id, msg.getInventoryType()))) {
          requested[0] = true;
        }
      });

      if (!spreaded[0]
          && !peer.isNeedSyncFromPeer()
          && !peer.isNeedSyncFromUs()) {

        //avoid TRX flood attack here.
        if (msg.getInventoryType().equals(InventoryType.TRX)
            && (peer.isAdvInvFull()
            || isFlooded())) {
          logger.warn("A peer is flooding us, stop handle inv, the peer is: " + peer);
          return;
        }

        peer.getAdvObjSpreadToUs().put(id, System.currentTimeMillis());
        if (!requested[0]) {
          if (!badAdvObj.containsKey(id)) {
            PriorItem targetPriorItem = this.advObjToFetch.get(id);

            if (targetPriorItem != null) {
              //another peer tell this trx to us, refresh its time.
              targetPriorItem.refreshTime();
            } else {
              fetchWaterLine.increase();
              this.advObjToFetch.put(id, new PriorItem(new Item(id, msg.getInventoryType()),
                  fetchSequenceCounter.incrementAndGet()));
            }
          }
        }
      }
    }
  }

  private boolean isFlooded() {
    return fetchWaterLine.totalCount()
        > BLOCK_PRODUCED_INTERVAL * NET_MAX_TRX_PER_SECOND * MSG_CACHE_DURATION_IN_BLOCKS / 1000;
  }

  public void syncFrom(Sha256Hash myHeadBlockHash) {
    try {
      while (getActivePeer().isEmpty()) {
        logger.info("other peer is nil, please wait ... ");
        Thread.sleep(10000L);
      }
    } catch (InterruptedException e) {
      logger.debug(e.getMessage(), e);
    }
    logger.info("wait end");
  }

  private void handleMessage(PeerConnection peer, BlockMessage blkMsg) {
    Map<Item, Long> advObjWeRequested = peer.getAdvObjWeRequested();
    Map<BlockId, Long> syncBlockRequested = peer.getSyncBlockRequested();
    BlockId blockId = blkMsg.getBlockId();
    Item item = new Item(blockId, InventoryType.BLOCK);
    boolean syncFlag = false;
    if (syncBlockRequested.containsKey(blockId)) {
      if (!peer.getSyncFlag()) {
        logger.info("Received a block {} from no need sync peer {}", blockId.getNum(),
            peer.getNode().getHost());
        return;
      }
      peer.getSyncBlockRequested().remove(blockId);
      synchronized (blockJustReceived) {
        blockJustReceived.put(blkMsg, peer);
      }
      isHandleSyncBlockActive = true;
      syncFlag = true;
      if (!peer.isBusy()) {
        if (peer.getUnfetchSyncNum() > 0
            && peer.getSyncBlockToFetch().size() <= NodeConstant.SYNC_FETCH_BATCH_NUM) {
          syncNextBatchChainIds(peer);
        } else {
          isFetchSyncActive = true;
        }
      }
    }

    if (advObjWeRequested.containsKey(item)) {
      advObjWeRequested.remove(item);
      if (!syncFlag) {
        processAdvBlock(peer, blkMsg.getBlockCapsule());
        startFetchItem();
      }
    }

  }

  private void syncNextBatchChainIds(PeerConnection peer) {
    if (peer.getSyncChainRequested() != null) {
      logger.info("Peer {} is in sync.", peer.getNode().getHost());
      return;
    }
    try {
      Deque<BlockId> chainSummary =
          controller.getBlockChainSummary(peer.getHeadBlockWeBothHave(),
              peer.getSyncBlockToFetch());
      peer.setSyncChainRequested(
          new Pair<>(chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage((LinkedList<BlockId>) chainSummary));
    } catch (TronException e) {
      logger.error("Peer {} sync next batch chainIds failed, error: {}", peer.getNode().getHost(),
          e.getMessage());
      disconnectPeer(peer, ReasonCode.FORKED);
    }
  }

  private Collection<PeerConnection> getActivePeer() {
    return pool.getActivePeers();
  }

//  public void handleMessage(Gsc gsc, BlockMessage msg) {
//    logger.info("get block message from " + gsc.getPeer());
//  }
//
//  public void handleMessage(Gsc gsc, TransactionMessage msg) {
//    logger.info("get tx message from " + gsc.getPeer());
//  }
//
//  public void handleMessage(Gsc gsc, FetchMessage msg) {
//    logger.info("get fetch message from " + gsc.getPeer());
//  }
//
//  public void handleMessage(Gsc gsc, SyncMessage msg) {
//    logger.info("get sync message from " + gsc.getPeer());
//  }
//
//  public void handleMessage(Gsc gsc, TimeMessage msg) {
//    logger.info("get time message from " + gsc.getPeer());
//  }
//
//  public void handleMessage(Gsc gsc, AttentionMessage msg) {
//    logger.info("get attention message from " + gsc.getPeer());
//  }

  private void disconnectPeer(PeerConnection peer, ReasonCode reason) {
    peer.setSyncFlag(false);
    peer.disconnect(reason);
  }

}
