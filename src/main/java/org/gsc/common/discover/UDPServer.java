package org.gsc.common.discover;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.concurrent.TimeUnit;
import org.gsc.common.net.udp.handler.MessageHandler;
import org.gsc.common.net.udp.handler.PacketDecoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.common.overlay.server.WireTrafficStats;
import org.gsc.config.args.Args;

@Component
public class UDPServer {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger("UdpServer");

  private volatile boolean shutdown = false;

  private Args args = Args.getInstance();

  private int port = args.getBackupPort();

  private DiscoverManager discoverManager;

  private Channel channel;

  @Autowired
  private WireTrafficStats stats;

  @Autowired
  public UDPServer(final DiscoverManager discoverManager) {
    this.discoverManager = discoverManager;
  }

  public void initServer(){
    if (port > 0 && args.getBackupMembers().size() > 0) {
      new Thread(() -> {
        try {
          start();
        } catch (Exception e) {
          logger.error("Startup udp server failed, {}", e);
        }
      }, "UdpServer").start();
    }
  }

  private void start() throws Exception {
    NioEventLoopGroup group = new NioEventLoopGroup(1);
    try {
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
                MessageHandler messageHandler = new MessageHandler(ch, discoverManager);
                discoverManager.setMessageHandler(messageHandler);
                ch.pipeline().addLast(messageHandler);
              }
            });

        channel = b.bind(port).sync().channel();

        logger.info("Discover server started, bind port {}", port);

        channel.closeFuture().sync();
        if (shutdown) {
          logger.info("Shutdown discover UdpServer");
          break;
        }
        logger.warn("Restart discover server ...");
      }
    } catch (Exception e) {
      logger.error("Start discover server with port {} failed.", port, e);
    } finally {
      group.shutdownGracefully().sync();
    }
  }

  public void close() {
    logger.info("Closing discover server...");
    shutdown = true;
    if (channel != null) {
      try {
        channel.close().await(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        logger.warn("Closing discover server failed.", e);
      }
    }
  }
}
