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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class GSCChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ChannelManager channelManager;

    private String remoteId;

    private boolean peerDiscoveryMode = false;

    public GSCChannelInitializer(String remoteId) {
        this.remoteId = remoteId;
    }

    @Override
    public void initChannel(NioSocketChannel ch) {
        try {
            final Channel channel = ctx.getBean(PeerConnection.class);

            channel.init(ch.pipeline(), remoteId, peerDiscoveryMode, channelManager);

            // limit the size of receiving buffer to 1024
            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(256 * 1024));
            ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

            // be aware of channel closing
            ch.closeFuture().addListener((ChannelFutureListener) future -> {
                logger.info("Close channel:" + channel);
                if (!peerDiscoveryMode) {
                    channelManager.notifyDisconnect(channel);
                }
            });

        } catch (Exception e) {
            logger.error("Unexpected error: ", e);
        }
    }

    public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }
}
