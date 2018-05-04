/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.gsc.net.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.sync.ChainContorller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
@Component
@Slf4j
@Scope("prototype")
public class GscChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    ChannelManager channelManager;

    private ChainContorller chainContorller;

    private String remoteId;

    private boolean peerDiscoveryMode = false;

    public GscChannelInitializer(String remoteId) {
        this.remoteId = remoteId;
    }

    @Override
    public void initChannel(NioSocketChannel ch) throws Exception {
        try {
            if (isInbound() && channelManager.isRecentlyDisconnected(ch.remoteAddress().getAddress())) {
                // avoid too frequent connection attempts
                logger.info("Drop connection - the same IP was disconnected recently, channel: {}", ch.toString());
                ch.disconnect();
                return;
            }

            final Channel channel = ctx.getBean(Channel.class);

            channel.init(ch.pipeline(), remoteId, peerDiscoveryMode, channelManager);

            if(!peerDiscoveryMode) {
                channelManager.add(channel);
            }

            // limit the size of receiving buffer to 1024
            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(256 * 1024));
            ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

            // be aware of channel closing
            ch.closeFuture().addListener((ChannelFutureListener) future -> {
                logger.info("Close channel:" + channel.getInetSocketAddress());
                if (!peerDiscoveryMode) {
                    channelManager.notifyDisconnect(channel);
                }
            });

        } catch (Exception e) {
            logger.error("Unexpected error: ", e);
        }
    }

    private boolean isInbound() {
        return remoteId == null || remoteId.isEmpty();
    }

    public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }

}
