package org.gsc.program;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.app.Application;
import org.gsc.common.app.ApplicationImpl;
import org.gsc.config.Args;
import org.gsc.config.DefaultConfig;
import org.gsc.service.NetService;
import org.gsc.service.RpcApiService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


@Slf4j
public class Start {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("gsc node running.");

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    Args config = context.getBean(Args.class);
    config.setParam(args, "config-localtest.conf");

    Application appT = context.getBean(ApplicationImpl.class);
    shutdown(appT);

    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    NetService netService = context.getBean(NetService.class);
    appT.addService(rpcApiService);
    appT.addService(netService);
    //TODO: add producer code

    appT.initServices(config);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }

  private static void shutdown(final Application app) {
  }
}

