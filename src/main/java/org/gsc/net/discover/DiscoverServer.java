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

package org.gsc.net.discover;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.node.NodeManager;
import org.gsc.net.discover.handler.MessageHandler;
import org.gsc.net.discover.handler.PacketDecoder;
import org.gsc.net.server.WireTrafficStats;
import org.gsc.config.args.Args;

@Slf4j(topic = "discover")
@Component
public class DiscoverServer {

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private WireTrafficStats stats;

    private Args args = Args.getInstance();

    private int port = args.getNodeListenPort();

    private Channel channel;

    private DiscoveryExecutor discoveryExecutor;

    private volatile boolean shutdown = false;

    @Autowired
    public DiscoverServer(final NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        if (args.isNodeDiscoveryEnable() && !args.isFastForward()) {
            if (port == 0) {
                logger.error("Discovery can't be started while listen port == 0");
            } else {
                new Thread(() -> {
                    try {
                        start();
                    } catch (Exception e) {
                        logger.error("Discovery server start failed.", e);
                    }
                }, "DiscoverServer").start();
            }
        }
    }

    public void start() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(args.getUdpNettyWorkThreadNum());
        try {
            discoveryExecutor = new DiscoveryExecutor(nodeManager);
            discoveryExecutor.start();
            while (!shutdown) {
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            public void initChannel(NioDatagramChannel ch)
                                    throws Exception {
                                ch.pipeline().addLast(stats.udp);
                                ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                                ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                                ch.pipeline().addLast(new PacketDecoder());

                                MessageHandler messageHandler = new MessageHandler(ch, nodeManager);
                                nodeManager.setMessageSender(messageHandler);
                                ch.pipeline().addLast(messageHandler);
                            }
                        });

                channel = b.bind(port).sync().channel();

                logger.info("Discovery server started, bind port {}", port);
                channel.closeFuture().sync();
                if (shutdown) {
                    logger.info("Shutdown discovery server");
                    break;
                }
                logger.warn(" Restart discovery server after 5 sec pause...");
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            logger.error("Start discovery server with port {} failed.", port, e);
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public void close() {
        logger.info("Closing discovery server...");
        shutdown = true;
        if (channel != null) {
            try {
                channel.close().await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.info("Closing discovery server failed.", e);
            }
        }

        if (discoveryExecutor != null) {
            try {
                discoveryExecutor.close();
            } catch (Exception e) {
                logger.info("Closing discovery executor failed.", e);
            }
        }
    }

}
