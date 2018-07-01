package org.gsc.net.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.gsc.config.Args;
import org.gsc.core.sync.PeerConnection;
import org.gsc.core.sync.SyncManager;
import org.gsc.net.client.PeerClient;
import org.gsc.net.discover.Node;
import org.gsc.net.discover.NodeHandler;
import org.gsc.net.discover.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SyncPool {

  public static final Logger logger = LoggerFactory.getLogger("SyncPool");

  private static final double factor = 0.4;

  private final List<PeerConnection> activePeers = Collections.synchronizedList(new ArrayList<PeerConnection>());
  private final AtomicInteger passivePeersCount = new AtomicInteger(0);
  private final AtomicInteger activePeersCount = new AtomicInteger(0);

  private Cache<NodeHandler, Long> nodeHandlerCache = CacheBuilder.newBuilder()
      .maximumSize(1000).expireAfterWrite(120, TimeUnit.SECONDS).recordStats().build();

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private ApplicationContext ctx;

  private ChannelManager channelManager;

  private SyncManager syncManager;

  @Autowired
  private Args args = Args.getInstance();

  private int maxActiveNodes = args.getNodeMaxActiveNodes() > 0 ? args.getNodeMaxActiveNodes() : 30;

  private ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();

  private PeerClient peerClient;

  public void init(SyncManager syncManager) {
    this.syncManager = syncManager;

    channelManager = ctx.getBean(ChannelManager.class);

    peerClient = ctx.getBean(PeerClient.class);

    poolLoopExecutor.scheduleWithFixedDelay(() -> {
      try {
        fillUp();
      } catch (Throwable t) {
        logger.error("Exception in sync worker", t);
      }
    }, 30, 16, TimeUnit.SECONDS);

    logExecutor.scheduleWithFixedDelay(() -> {
      try {
        logActivePeers();
      } catch (Throwable t) {
      }
    }, 30, 10, TimeUnit.SECONDS);
  }

  private void fillUp() {
    int lackSize = (int) (maxActiveNodes * factor) - activePeers.size();
    if(lackSize <= 0) return;

    final Set<String> nodesInUse = new HashSet<>();
    channelManager.getActivePeers().forEach(channel -> nodesInUse.add(channel.getPeerId()));
    nodesInUse.add(nodeManager.getPublicHomeNode().getHexId());

    List<NodeHandler> newNodes = nodeManager.getNodes(new NodeSelector(nodesInUse), lackSize);
    newNodes.forEach(n -> {
      peerClient.connectAsync(n, false);
      nodeHandlerCache.put(n, System.currentTimeMillis());
    });
  }

  // for test only
  public void addActivePeers(PeerConnection p) {
    activePeers.add(p);
  }


  synchronized void logActivePeers() {

    logger.info("-------- active connect channel {}", activePeersCount.get());
    logger.info("-------- passive connect channel {}", passivePeersCount.get());
    logger.info("-------- all connect channel {}", channelManager.getActivePeers().size());
    for (Channel channel : channelManager.getActivePeers()) {
      logger.info(channel.toString());
    }

    if (logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder("Peer stats:\n");
      sb.append("Active peers\n");
      sb.append("============\n");
      Set<Node> activeSet = new HashSet<>();
      for (PeerConnection peer : new ArrayList<>(activePeers)) {
        sb.append(peer.logSyncStats()).append('\n');
        activeSet.add(peer.getNode());
      }
      sb.append("Other connected peers\n");
      sb.append("============\n");
      for (Channel peer : new ArrayList<>(channelManager.getActivePeers())) {
        if (!activeSet.contains(peer.getNode())) {
          sb.append(peer.getNode()).append('\n');
        }
      }
      logger.info(sb.toString());
    }
  }

  public synchronized List<PeerConnection> getActivePeers() {
    List<PeerConnection> peers = Lists.newArrayList();
    activePeers.forEach(peer -> {
      if (!peer.isDisconnect()) {
        peers.add(peer);
      }
    });
    return peers;
  }

  public synchronized void onConnect(Channel peer) {
    if (!activePeers.contains(peer)) {
      if (!peer.isActive()) {
        passivePeersCount.incrementAndGet();
      } else {
        activePeersCount.incrementAndGet();
      }
      activePeers.add((PeerConnection) peer);
      activePeers.sort(Comparator.comparingDouble(c -> c.getPeerStats().getAvgLatency()));
      syncManager.onConnectPeer((PeerConnection) peer);
    }
  }

  public synchronized void onDisconnect(Channel peer) {
    if (activePeers.contains(peer)) {
      if (!peer.isActive()) {
        passivePeersCount.decrementAndGet();
      } else {
        activePeersCount.decrementAndGet();
      }
      activePeers.remove(peer);
      syncManager.onDisconnectPeer((PeerConnection) peer);
    }
  }

  public boolean isCanConnect() {
    if (activePeers.size() >= maxActiveNodes) {
      return false;
    }
    return true;
  }

  public void close() {
    try {
      poolLoopExecutor.shutdownNow();
      logExecutor.shutdownNow();
    } catch (Exception e) {
      logger.warn("Problems shutting down executor", e);
    }
  }

  class NodeSelector implements Predicate<NodeHandler> {

    Set<String> nodesInUse;

    public NodeSelector(Set<String> nodesInUse) {
      this.nodesInUse = nodesInUse;
    }

    @Override
    public boolean test(NodeHandler handler) {

      if (handler.getNode().getHost().equals(nodeManager.getPublicHomeNode().getHost()) &&
          handler.getNode().getPort() == nodeManager.getPublicHomeNode().getPort()) {
        return false;
      }

      InetAddress inetAddress = handler.getInetSocketAddress().getAddress();
      if (channelManager.getRecentlyDisconnected().getIfPresent(inetAddress) != null) {
        return false;
      }
      if (channelManager.getBadPeers().getIfPresent(inetAddress) != null) {
        return false;
      }

      if (nodesInUse != null && nodesInUse.contains(handler.getNode().getHexId())) {
        return false;
      }

      if (nodeHandlerCache.getIfPresent(handler) != null) {
        return false;
      }

      if (handler.getNodeStatistics().getReputation() < 100) {
        return false;
      }

      return true;
    }
  }

}
