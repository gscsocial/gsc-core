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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j(topic = "net")
@Component
public class WireTrafficStats implements Runnable {

    private ScheduledExecutorService executor;
    public final TrafficStatHandler tcp = new TrafficStatHandler();
    public final TrafficStatHandler udp = new TrafficStatHandler();

    public WireTrafficStats() {
        executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("WireTrafficStats-%d").build());
        executor.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }

    @ChannelHandler.Sharable
    static class TrafficStatHandler extends ChannelDuplexHandler {

        private AtomicLong outSize = new AtomicLong();
        private AtomicLong inSize = new AtomicLong();
        private AtomicLong outPackets = new AtomicLong();
        private AtomicLong inPackets = new AtomicLong();

        public String stats() {
            return "";
        }


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            inPackets.incrementAndGet();
            if (msg instanceof ByteBuf) {
                inSize.addAndGet(((ByteBuf) msg).readableBytes());
            } else if (msg instanceof DatagramPacket) {
                inSize.addAndGet(((DatagramPacket) msg).content().readableBytes());
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                throws Exception {
            outPackets.incrementAndGet();
            if (msg instanceof ByteBuf) {
                outSize.addAndGet(((ByteBuf) msg).readableBytes());
            } else if (msg instanceof DatagramPacket) {
                outSize.addAndGet(((DatagramPacket) msg).content().readableBytes());
            }
            super.write(ctx, msg, promise);
        }
    }
}
