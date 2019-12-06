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

import static org.gsc.net.peer.p2p.StaticMessages.PING_MESSAGE;
import static org.gsc.net.peer.p2p.StaticMessages.PONG_MESSAGE;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.node.statistics.MessageStatistics;
import org.gsc.net.peer.p2p.DisconnectMessage;
import org.gsc.net.peer.p2p.P2pMessage;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class P2pHandler extends SimpleChannelInboundHandler<P2pMessage> {

    private static ScheduledExecutorService pingTimer =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "P2pPingTimer"));

    private MessageQueue msgQueue;

    private Channel channel;

    private ScheduledFuture<?> pingTask;

    private volatile boolean hasPing = false;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        pingTask = pingTimer.scheduleAtFixedRate(() -> {
            if (!hasPing) {
                hasPing = msgQueue.sendMessage(PING_MESSAGE);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, P2pMessage msg) {

        msgQueue.receivedMessage(msg);
        MessageStatistics messageStatistics = channel.getNodeStatistics().messageStatistics;
        switch (msg.getType()) {
            case P2P_PING:
                int count = messageStatistics.p2pInPing.getCount(10);
                if (count > 3) {
                    logger.warn("TCP attack found: {} with ping count({})", ctx.channel().remoteAddress(),
                            count);
                    channel.disconnect(ReasonCode.BAD_PROTOCOL);
                    return;
                }
                msgQueue.sendMessage(PONG_MESSAGE);
                break;
            case P2P_PONG:
                if (messageStatistics.p2pInPong.getTotalCount() > messageStatistics.p2pOutPing
                        .getTotalCount()) {
                    logger.warn("TCP attack found: {} with ping count({}), pong count({})",
                            ctx.channel().remoteAddress(),
                            messageStatistics.p2pOutPing.getTotalCount(),
                            messageStatistics.p2pInPong.getTotalCount());
                    channel.disconnect(ReasonCode.BAD_PROTOCOL);
                    return;
                }
                hasPing = false;
                channel.getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
                break;
            case P2P_DISCONNECT:
                channel.getNodeStatistics()
                        .nodeDisconnectedRemote(((DisconnectMessage) msg).getReasonCode());
                channel.close();
                break;
            default:
                channel.close();
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        channel.processException(cause);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setMsgQueue(MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    public void close() {
        if (pingTask != null && !pingTask.isCancelled()) {
            pingTask.cancel(false);
        }
    }
}