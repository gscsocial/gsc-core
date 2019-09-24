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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.gsc.config.args.Args;

@Slf4j(topic = "net")
@Component
public class PeerServer {

    private Args args = Args.getInstance();

    private ApplicationContext ctx;

    private boolean listening;

    private ChannelFuture channelFuture;

    @Autowired
    public PeerServer(final Args args, final ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public void start(int port) {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(args.getTcpNettyWorkThreadNum());
        GSCChannelInitializer GSCChannelInitializer = ctx.getBean(GSCChannelInitializer.class, "");

        try {
            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);

            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.args.getNodeConnectionTimeout());

            b.handler(new LoggingHandler());
            b.childHandler(GSCChannelInitializer);

            // Start the client.
            logger.info("TCP listener started, bind port {}", port);

            channelFuture = b.bind(port).sync();

            listening = true;

            // Wait until the connection is closed.
            channelFuture.channel().closeFuture().sync();

            logger.info("TCP listener is closed");

        } catch (Exception e) {
            logger.error("Start TCP server failed.", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            listening = false;
        }
    }

    public void close() {
        if (listening && channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                logger.info("Closing TCP server...");
                channelFuture.channel().close().sync();
            } catch (Exception e) {
                logger.warn("Closing TCP server failed.", e);
            }
        }
    }
}
