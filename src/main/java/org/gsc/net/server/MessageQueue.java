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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.peer.p2p.Message;
import org.gsc.net.peer.p2p.PingMessage;
import org.gsc.net.peer.p2p.PongMessage;
import org.gsc.net.peer.message.InventoryMessage;
import org.gsc.net.peer.message.TransactionsMessage;
import org.gsc.protos.Protocol.Inventory.InventoryType;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class MessageQueue {

    private volatile boolean sendMsgFlag = false;

    private volatile long sendTime;

    private volatile long sendPing;

    private Thread sendMsgThread;

    private Channel channel;

    private ChannelHandlerContext ctx = null;

    private Queue<MessageRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();

    private BlockingQueue<Message> msgQueue = new LinkedBlockingQueue<>();

    private static ScheduledExecutorService sendTimer = Executors.
            newSingleThreadScheduledExecutor(r -> new Thread(r, "sendTimer"));

    private ScheduledFuture<?> sendTask;


    public void activate(ChannelHandlerContext ctx) {

        this.ctx = ctx;

        sendMsgFlag = true;

        sendTask = sendTimer.scheduleAtFixedRate(() -> {
            try {
                if (sendMsgFlag) {
                    send();
                }
            } catch (Exception e) {
                logger.error("Unhandled exception", e);
            }
        }, 10, 10, TimeUnit.MILLISECONDS);

        sendMsgThread = new Thread(() -> {
            while (sendMsgFlag) {
                try {
                    if (msgQueue.isEmpty()) {
                        Thread.sleep(10);
                        continue;
                    }
                    Message msg = msgQueue.take();
                    ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess() && !channel.isDisconnect()) {
                            logger.error("Fail send to {}, {}", ctx.channel().remoteAddress(), msg);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Fail send to {}, error info: {}", ctx.channel().remoteAddress(),
                            e.getMessage());
                }
            }
        });
        sendMsgThread.setName("sendMsgThread-" + ctx.channel().remoteAddress());
        sendMsgThread.start();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean sendMessage(Message msg) {
        long now = System.currentTimeMillis();
        if (msg instanceof PingMessage) {
            if (now - sendTime < 10_000 && now - sendPing < 60_000) {
                return false;
            }
            sendPing = now;
        }
        if (needToLog(msg)) {
            logger.info("Send to {}, {} ", ctx.channel().remoteAddress(), msg);
        }
        channel.getNodeStatistics().messageStatistics.addTcpOutMessage(msg);
        sendTime = System.currentTimeMillis();
        if (msg.getAnswerMessage() != null) {
            requestQueue.add(new MessageRoundtrip(msg));
        } else {
            msgQueue.offer(msg);
        }
        return true;
    }

    public void receivedMessage(Message msg) {
        if (needToLog(msg)) {
            logger.info("Receive from {}, {}", ctx.channel().remoteAddress(), msg);
        }
        channel.getNodeStatistics().messageStatistics.addTcpInMessage(msg);
        MessageRoundtrip rt = requestQueue.peek();
        if (rt != null && rt.getMsg().getAnswerMessage() == msg.getClass()) {
            requestQueue.remove();
            if (rt.getMsg() instanceof PingMessage) {
                channel.getNodeStatistics().pingMessageLatency
                        .add(System.currentTimeMillis() - rt.getTime());
            }
        }
    }

    public void close() {
        sendMsgFlag = false;
        if (sendTask != null && !sendTask.isCancelled()) {
            sendTask.cancel(false);
            sendTask = null;
        }
        if (sendMsgThread != null) {
            try {
                sendMsgThread.join(20);
                sendMsgThread = null;
            } catch (Exception e) {
                logger.warn("Join send thread failed, peer {}", ctx.channel().remoteAddress());
            }
        }
    }

    private boolean needToLog(Message msg) {
        if (msg instanceof PingMessage ||
                msg instanceof PongMessage ||
                msg instanceof TransactionsMessage) {
            return false;
        }
        if (msg instanceof InventoryMessage &&
                ((InventoryMessage) msg).getInventoryType().equals(InventoryType.TRX)) {
            return false;
        }
        return true;
    }

    private void send() {
        MessageRoundtrip rt = requestQueue.peek();
        if (!sendMsgFlag || rt == null) {
            return;
        }
        if (rt.getRetryTimes() > 0 && !rt.hasToRetry()) {
            return;
        }
        if (rt.getRetryTimes() > 0) {
            channel.getNodeStatistics().nodeDisconnectedLocal(ReasonCode.PING_TIMEOUT);
            logger.warn("Wait {} timeout. close channel {}.",
                    rt.getMsg().getAnswerMessage(), ctx.channel().remoteAddress());
            channel.close();
            return;
        }

        Message msg = rt.getMsg();

        ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.error("Fail send to {}, {}", ctx.channel().remoteAddress(), msg);
            }
        });

        rt.incRetryTimes();
        rt.saveTime();
    }

}
