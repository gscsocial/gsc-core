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

package org.gsc.net.node;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.node.statistics.NodeStatistics;
import org.gsc.net.node.table.NodeTable;
import org.gsc.net.discover.handler.EventHandler;
import org.gsc.net.discover.handler.UdpEvent;
import org.gsc.net.discover.message.Message;
import org.gsc.net.discover.message.discover.FindNodeMessage;
import org.gsc.net.discover.message.discover.NeighborsMessage;
import org.gsc.net.discover.message.discover.PingMessage;
import org.gsc.net.discover.message.discover.PongMessage;
import org.gsc.net.discover.DiscoverListener;
import org.gsc.net.discover.RefreshTask;
import org.gsc.utils.CollectionUtils;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;

@Slf4j(topic = "discover")
@Component
public class NodeManager implements EventHandler {

    private Args args = Args.getInstance();

    private Manager dbManager;

    private static final long LISTENER_REFRESH_RATE = 1000L;
    private static final long DB_COMMIT_RATE = 1 * 60 * 1000L;
    private static final int MAX_NODES = 2000;
    private static final int NODES_TRIM_THRESHOLD = 3000;

    private Consumer<UdpEvent> messageSender;

    private NodeTable table;
    private Node homeNode;
    private Map<String, NodeHandler> nodeHandlerMap = new ConcurrentHashMap<>();
    private List<Node> bootNodes = new ArrayList<>();

    // option to handle inbounds only from known peers (i.e. which were discovered by ourselves)
    private boolean inboundOnlyFromKnownNodes = false;

    private boolean discoveryEnabled;

    private Map<DiscoverListener, ListenerHandler> listeners = new IdentityHashMap<>();

    private boolean inited = false;
    private Timer nodeManagerTasksTimer = new Timer("NodeManagerTasks");
    private ScheduledExecutorService pongTimer;

    @Autowired
    public NodeManager(Manager dbManager) {
        this.dbManager = dbManager;
        discoveryEnabled = args.isNodeDiscoveryEnable();

        homeNode = new Node(RefreshTask.getNodeId(), args.getNodeExternalIp(),
                args.getNodeListenPort());

        for (String boot : args.getBootNodes()) {
            bootNodes.add(Node.instanceOf(boot));
        }

        logger.info("homeNode : {}", homeNode);
        logger.info("bootNodes : size= {}", bootNodes.size());

        table = new NodeTable(homeNode);

        this.pongTimer = Executors.newSingleThreadScheduledExecutor();
    }

    public ScheduledExecutorService getPongTimer() {
        return pongTimer;
    }

    @Override
    public void channelActivated() {
        if (!inited) {
            inited = true;
            nodeManagerTasksTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    processListeners();
                }
            }, LISTENER_REFRESH_RATE, LISTENER_REFRESH_RATE);

            if (args.isNodeDiscoveryPersist()) {
                dbRead();
                nodeManagerTasksTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        dbWrite();
                    }
                }, DB_COMMIT_RATE, DB_COMMIT_RATE);
            }

            for (Node node : bootNodes) {
                getNodeHandler(node);
            }
        }
    }

    public boolean isNodeAlive(NodeHandler nodeHandler) {
        return nodeHandler.getState().equals(NodeHandler.State.Alive)
                || nodeHandler.getState().equals(NodeHandler.State.Active)
                || nodeHandler.getState().equals(NodeHandler.State.EvictCandidate);
    }

    private void dbRead() {
        Set<Node> nodes = this.dbManager.readNeighbours();
        logger.info("Reading Node statistics from PeersStore: " + nodes.size() + " nodes.");
        nodes.forEach(node -> getNodeHandler(node).getNodeStatistics()
                .setPersistedReputation(node.getReputation()));
    }

    private void dbWrite() {
        Set<Node> batch = new HashSet<>();
        synchronized (this) {
            for (NodeHandler nodeHandler : nodeHandlerMap.values()) {
                int reputation = nodeHandler.getNodeStatistics().getReputation();
                nodeHandler.getNode().setReputation(reputation);
                batch.add(nodeHandler.getNode());
            }
        }
        logger.info("Write Node statistics to PeersStore: " + batch.size() + " nodes.");
        dbManager.clearAndWriteNeighbours(batch);
    }

    public void setMessageSender(Consumer<UdpEvent> messageSender) {
        this.messageSender = messageSender;
    }

    private String getKey(Node n) {
        return getKey(new InetSocketAddress(n.getHost(), n.getPort()));
    }

    private String getKey(InetSocketAddress address) {
        InetAddress addr = address.getAddress();
        return (addr == null ? address.getHostString() : addr.getHostAddress()) + ":" + address
                .getPort();
    }

    public synchronized NodeHandler getNodeHandler(Node n) {
        String key = getKey(n);
        NodeHandler ret = nodeHandlerMap.get(key);
        if (ret == null) {
            trimTable();
            ret = new NodeHandler(n, this);
            nodeHandlerMap.put(key, ret);
        } else if (ret.getNode().isDiscoveryNode() && !n.isDiscoveryNode()) {
            ret.setNode(n);
        }
        return ret;
    }

    private void trimTable() {
        if (nodeHandlerMap.size() > NODES_TRIM_THRESHOLD) {
            List<NodeHandler> sorted = new ArrayList<>(nodeHandlerMap.values());
            // reverse sort by reputation
            sorted.sort(Comparator.comparingInt(o -> o.getNodeStatistics().getReputation()));
            for (NodeHandler handler : sorted) {
                nodeHandlerMap.values().remove(handler);
                if (nodeHandlerMap.size() <= MAX_NODES) {
                    break;
                }
            }
        }
    }

    public boolean hasNodeHandler(Node n) {
        return nodeHandlerMap.containsKey(getKey(n));
    }

    public NodeTable getTable() {
        return table;
    }

    public NodeStatistics getNodeStatistics(Node n) {
        return getNodeHandler(n).getNodeStatistics();
    }

    @Override
    public void handleEvent(UdpEvent udpEvent) {
        Message m = udpEvent.getMessage();
        InetSocketAddress sender = udpEvent.getAddress();

        Node n = new Node(m.getFrom().getId(), sender.getHostString(), sender.getPort());
        if (inboundOnlyFromKnownNodes && !hasNodeHandler(n)) {
            logger.warn("Receive packet from unknown node {}.", sender.getAddress());
            return;
        }

        NodeHandler nodeHandler = getNodeHandler(n);
        nodeHandler.getNodeStatistics().messageStatistics.addUdpInMessage(m.getType());

        switch (m.getType()) {
            case DISCOVER_PING:
                nodeHandler.handlePing((PingMessage) m);
                break;
            case DISCOVER_PONG:
                nodeHandler.handlePong((PongMessage) m);
                break;
            case DISCOVER_FIND_NODE:
                nodeHandler.handleFindNode((FindNodeMessage) m);
                break;
            case DISCOVER_NEIGHBORS:
                nodeHandler.handleNeighbours((NeighborsMessage) m);
                break;
            default:
                break;
        }
    }

    public void sendOutbound(UdpEvent udpEvent) {
        if (discoveryEnabled && messageSender != null) {
            messageSender.accept(udpEvent);
        }
    }

    public synchronized List<NodeHandler> getNodes(int minReputation) {
        List<NodeHandler> ret = new ArrayList<>();
        for (NodeHandler nodeHandler : nodeHandlerMap.values()) {
            if (nodeHandler.getNodeStatistics().getReputation() >= minReputation) {
                ret.add(nodeHandler);
            }
        }
        return ret;
    }

    public List<NodeHandler> getNodes(Predicate<NodeHandler> predicate, int limit) {
        ArrayList<NodeHandler> filtered = new ArrayList<>();
        synchronized (this) {
            for (NodeHandler handler : nodeHandlerMap.values()) {
                if (predicate.test(handler)) {
                    filtered.add(handler);
                }
            }
        }

        logger.debug("nodeHandlerMap size {} filter peer  size {}", nodeHandlerMap.size(),
                filtered.size());

        //TODO: here can use head num sort.
        filtered.sort(Comparator.comparingInt((NodeHandler o) -> o.getNodeStatistics().getReputation())
                .reversed());

        return CollectionUtils.truncate(filtered, limit);
    }

    public List<NodeHandler> dumpActiveNodes() {
        List<NodeHandler> handlers = new ArrayList<>();
        for (NodeHandler handler :
                this.nodeHandlerMap.values()) {
            if (isNodeAlive(handler)) {
                handlers.add(handler);
            }
        }

        return handlers;
    }

    private synchronized void processListeners() {
        for (ListenerHandler handler : listeners.values()) {
            try {
                handler.checkAll();
            } catch (Exception e) {
                logger.error("Exception processing listener: " + handler, e);
            }
        }
    }

    public synchronized void addDiscoverListener(DiscoverListener listener,
                                                 Predicate<NodeStatistics> filter) {
        listeners.put(listener, new ListenerHandler(listener, filter));
    }

    public Node getPublicHomeNode() {
        return homeNode;
    }

    public void close() {
        try {
            nodeManagerTasksTimer.cancel();
            pongTimer.shutdownNow();
        } catch (Exception e) {
            logger.warn("close failed.", e);
        }
    }

    private class ListenerHandler {

        private Map<NodeHandler, Object> discoveredNodes = new IdentityHashMap<>();
        private DiscoverListener listener;
        private Predicate<NodeStatistics> filter;

        ListenerHandler(DiscoverListener listener, Predicate<NodeStatistics> filter) {
            this.listener = listener;
            this.filter = filter;
        }

        void checkAll() {
            for (NodeHandler handler : nodeHandlerMap.values()) {
                boolean has = discoveredNodes.containsKey(handler);
                boolean test = filter.test(handler.getNodeStatistics());
                if (!has && test) {
                    listener.nodeAppeared(handler);
                    discoveredNodes.put(handler, null);
                } else if (has && !test) {
                    listener.nodeDisappeared(handler);
                    discoveredNodes.remove(handler);
                }
            }
        }
    }

}
