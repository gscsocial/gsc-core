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

package org.gsc.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.FileUtil;
import org.gsc.utils.ReflectUtils;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.net.peer.PeerConnection;
import org.gsc.services.RpcApiService;

@Slf4j
public abstract class BaseNet {

  private static String dbPath = "db-net";
  private static String dbDirectory = "database";
  private static String indexDirectory = "index";
  private static int port = 10000;

  protected GSCApplicationContext context;

  private RpcApiService rpcApiService;
  private Application appT;
  private GSCNetDelegate gscNetDelegate;

  private ExecutorService executorService = Executors.newFixedThreadPool(1);

  @Before
  public void init() throws Exception {
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        logger.info("Full node running.");
        Args.setParam(
            new String[]{
                "--db-directory", dbPath,
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            "config.conf"
        );
        Args cfgArgs = Args.getInstance();
        cfgArgs.setNodeListenPort(port);
       // cfgArgs.getSeedNode().getIpList().clear();
        cfgArgs.setNodeExternalIp("127.0.0.1");
        context = new GSCApplicationContext(DefaultConfig.class);
        appT = ApplicationFactory.create(context);
        rpcApiService = context.getBean(RpcApiService.class);
        appT.addService(rpcApiService);
        appT.initServices(cfgArgs);
        appT.startServices();
        appT.startup();
        gscNetDelegate = context.getBean(GSCNetDelegate.class);
        rpcApiService.blockUntilShutdown();
      }
    });
    int tryTimes = 0;
    while (++tryTimes < 100 && gscNetDelegate == null) {
      Thread.sleep(3000);
    }
  }

  public static Channel connect(ByteToMessageDecoder decoder) throws InterruptedException {
    NioEventLoopGroup group = new NioEventLoopGroup(1);
    Bootstrap b = new Bootstrap();
    b.group(group).channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(256 * 1024));
            ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);
            ch.pipeline()
                .addLast("readTimeoutHandler", new ReadTimeoutHandler(600, TimeUnit.SECONDS))
                .addLast("writeTimeoutHandler", new WriteTimeoutHandler(600, TimeUnit.SECONDS));
            ch.pipeline().addLast("protoPender", new ProtobufVarint32LengthFieldPrepender());
            ch.pipeline().addLast("lengthDecode", new ProtobufVarint32FrameDecoder());
            ch.pipeline().addLast("handshakeHandler", decoder);
            ch.closeFuture();
          }
        }).option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
        .option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
    return b.connect("127.0.0.1", port).sync().channel();
  }

  @After
  public void destroy() {
    Collection<PeerConnection> peerConnections = ReflectUtils
        .invokeMethod(gscNetDelegate, "getActivePeer");
    for (PeerConnection peer : peerConnections) {
      peer.close();
    }

    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
