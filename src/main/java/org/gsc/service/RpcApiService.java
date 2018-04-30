package org.gsc.service;

import io.grpc.ServerBuilder;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.config.Args;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RpcApiService implements Service{

  private Args args;

  private io.grpc.Server apiServer;

  @Override
  public void init() {

  }

  @Override
  public void init(Args args) {
    this.args = args;
  }

  @Override
  public void start() {
    try {
      ServerBuilder serverBuilder = ServerBuilder.forPort(args.getRpcPort());
      apiServer = serverBuilder.build().start();
    } catch (IOException e) {
      logger.debug(e.getMessage(), e);
    }

    logger.info("Server started, listening on " + args.getRpcPort());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      //server.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  @Override
  public void stop() {

  }

  public void blockUntilShutdown() {
    if (apiServer != null) {
      try {
        apiServer.awaitTermination();
      } catch (InterruptedException e) {
        logger.debug(e.getMessage(), e);
      }
    }
  }
}
