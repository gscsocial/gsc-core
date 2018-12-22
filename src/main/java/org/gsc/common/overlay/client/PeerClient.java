package org.gsc.common.overlay.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.common.overlay.discover.node.NodeHandler;
import org.gsc.common.overlay.server.GSCChannelInitializer;
import org.gsc.config.args.Args;
import org.gsc.net.node.NodeImpl;
import org.gsc.protos.Protocol.ReasonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PeerClient {

    private static final Logger logger = LoggerFactory.getLogger("PeerClient");

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    @Lazy
    private NodeImpl node;

    private EventLoopGroup workerGroup;

    public PeerClient() {
        workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
            AtomicInteger cnt = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "gscJClientWorker-" + cnt.getAndIncrement());
            }
        });
    }

    public void connect(String host, int port, String remoteId) {
        try {
            ChannelFuture f = connectAsync(host, port, remoteId, false);
            f.sync().channel().closeFuture().sync();
        } catch (Exception e) {
            logger.info("PeerClient: Can't connect to " + host + ":" + port + " (" + e.getMessage() + ")");
        }
    }

    public ChannelFuture connectAsync(NodeHandler nodeHandler, boolean discoveryMode) {
        Node node = nodeHandler.getNode();
        return connectAsync(node.getHost(), node.getPort(), node.getHexId(), discoveryMode)
            .addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    logger.error("connect to {}:{} fail,cause:{}", node.getHost(), node.getPort(),
                        future.cause().getMessage());
                    nodeHandler.getNodeStatistics().nodeDisconnectedLocal(ReasonCode.CONNECT_FAIL);
                    nodeHandler.getNodeStatistics().notifyDisconnect();
                    future.channel().close();
                }
            });
    }

    public ChannelFuture connectAsync(String host, int port, String remoteId, boolean discoveryMode) {

        logger.info("connect peer {} {} {}", host, port, remoteId);

        GSCChannelInitializer gscChannelInitializer = ctx.getBean(GSCChannelInitializer.class, remoteId);
        gscChannelInitializer.setPeerDiscoveryMode(discoveryMode);
        gscChannelInitializer.setNodeImpl(node);

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Args.getInstance().getNodeConnectionTimeout());
        b.remoteAddress(host, port);

        b.handler(gscChannelInitializer);

        // Start the client.
        return b.connect();
    }

    public void close() {
        workerGroup.shutdownGracefully();
        workerGroup.terminationFuture().syncUninterruptibly();
    }
}
