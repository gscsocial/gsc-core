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

package org.gsc.net.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.gsc.net.node.NodeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.node.Node;
import org.gsc.net.node.NodeManager;
import org.gsc.net.node.statistics.NodeStatistics;
import org.gsc.net.peer.p2p.DisconnectMessage;
import org.gsc.net.peer.p2p.HelloMessage;
import org.gsc.net.peer.p2p.MessageCodec;
import org.gsc.net.peer.p2p.StaticMessages;
import org.gsc.db.ByteArrayWrapper;
import org.gsc.core.exception.P2pException;
import org.gsc.net.GSCNetHandler;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class Channel {

    @Autowired
    protected MessageQueue msgQueue;

    @Autowired
    private MessageCodec messageCodec;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private WireTrafficStats stats;

    @Autowired
    private HandshakeHandler handshakeHandler;

    @Autowired
    private P2pHandler p2pHandler;

    @Autowired
    private GSCNetHandler gscNetHandler;

    private ChannelManager channelManager;

    private ChannelHandlerContext ctx;

    private InetSocketAddress inetSocketAddress;

    private Node node;

    private long startTime;

    private GSCState gscState = GSCState.INIT;

    protected NodeStatistics nodeStatistics;

    private boolean isActive;

    private volatile boolean isDisconnect;

    private boolean isTrustPeer;

    private boolean isFastForwardPeer;

    public void init(ChannelPipeline pipeline, String remoteId, boolean discoveryMode,
                     ChannelManager channelManager) {

        this.channelManager = channelManager;

        isActive = remoteId != null && !remoteId.isEmpty();

        startTime = System.currentTimeMillis();

        //TODO: use config here
        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60, TimeUnit.SECONDS));
        pipeline.addLast(stats.tcp);
        pipeline.addLast("protoPender", new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast("lengthDecode", new GSCProtobufVarint32FrameDecoder(this));

        //handshake first
        pipeline.addLast("handshakeHandler", handshakeHandler);

        messageCodec.setChannel(this);
        msgQueue.setChannel(this);
        handshakeHandler.setChannel(this, remoteId);
        p2pHandler.setChannel(this);
        gscNetHandler.setChannel(this);

        p2pHandler.setMsgQueue(msgQueue);
        gscNetHandler.setMsgQueue(msgQueue);
    }

    public void publicHandshakeFinished(ChannelHandlerContext ctx, HelloMessage msg) {
        isTrustPeer = channelManager.getTrustNodes().getIfPresent(getInetAddress()) != null;
        isFastForwardPeer = channelManager.getFastForwardNodes().containsKey(getInetAddress());
        ctx.pipeline().remove(handshakeHandler);
        msgQueue.activate(ctx);
        ctx.pipeline().addLast("messageCodec", messageCodec);
        ctx.pipeline().addLast("p2p", p2pHandler);
        ctx.pipeline().addLast("data", gscNetHandler);
        setStartTime(msg.getTimestamp());
        setGSCState(GSCState.HANDSHAKE_FINISHED);
        getNodeStatistics().p2pHandShake.add();
        logger.info("Finish handshake with {}.", ctx.channel().remoteAddress());
    }

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    public void initNode(byte[] nodeId, int remotePort) {
        Node n = new Node(nodeId, inetSocketAddress.getHostString(), remotePort);
        NodeHandler handler = nodeManager.getNodeHandler(n);
        node = handler.getNode();
        nodeStatistics = handler.getNodeStatistics();
        handler.getNode().setId(nodeId);
    }

    public void processException(Throwable throwable) {
        Throwable baseThrowable = throwable;
        while (baseThrowable.getCause() != null) {
            baseThrowable = baseThrowable.getCause();
        }
        SocketAddress address = ctx.channel().remoteAddress();
        if (throwable instanceof ReadTimeoutException ||
                throwable instanceof IOException) {
            logger.warn("Close peer {}, reason: {}", address, throwable.getMessage());
        } else if (baseThrowable instanceof P2pException) {
            logger.warn("Close peer {}, type: {}, info: {}",
                    address, ((P2pException) baseThrowable).getType(), baseThrowable.getMessage());
        } else {
            logger.error("Close peer {}, exception caught", address, throwable);
        }
        close();
    }

    public void disconnect(ReasonCode reason) {
        this.isDisconnect = true;
        channelManager.processDisconnect(this, reason);
        DisconnectMessage msg = new DisconnectMessage(reason);
        logger.info("Send to {} online-time {}s, {}",
                ctx.channel().remoteAddress(),
                (System.currentTimeMillis() - startTime) / 1000,
                msg);
        getNodeStatistics().nodeDisconnectedLocal(reason);
        ctx.writeAndFlush(msg.getSendData()).addListener(future -> close());
    }

    public void close() {
        this.isDisconnect = true;
        p2pHandler.close();
        msgQueue.close();
        ctx.close();
    }

    public enum GSCState {
        INIT,
        HANDSHAKE_FINISHED,
        START_TO_SYNC,
        SYNCING,
        SYNC_COMPLETED,
        SYNC_FAILED
    }

    public Node getNode() {
        return node;
    }

    public byte[] getNodeId() {
        return node == null ? null : node.getId();
    }

    public ByteArrayWrapper getNodeIdWrapper() {
        return node == null ? null : new ByteArrayWrapper(node.getId());
    }

    public String getPeerId() {
        return node == null ? "<null>" : node.getHexId();
    }

    public void setChannelHandlerContext(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.inetSocketAddress = ctx == null ? null : (InetSocketAddress) ctx.channel().remoteAddress();
    }

    public InetAddress getInetAddress() {
        return ctx == null ? null : ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
    }

    public NodeStatistics getNodeStatistics() {
        return nodeStatistics;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setGSCState(GSCState gscState) {
        this.gscState = gscState;
        logger.info("Peer {} status change to {}.", inetSocketAddress, gscState);
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isDisconnect() {
        return isDisconnect;
    }

    public boolean isTrustPeer() {
        return isTrustPeer;
    }

    public boolean isFastForwardPeer() {
        return isFastForwardPeer;
    }


    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Channel channel = (Channel) o;
        if (inetSocketAddress != null ? !inetSocketAddress.equals(channel.inetSocketAddress)
                : channel.inetSocketAddress != null) {
            return false;
        }
        if (node != null ? !node.equals(channel.node) : channel.node != null) {
            return false;
        }
        return this == channel;
    }

    @Override
    public int hashCode() {
        int result = inetSocketAddress != null ? inetSocketAddress.hashCode() : 0;
        result = 31 * result + (node != null ? node.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", inetSocketAddress, getPeerId());
    }

}

