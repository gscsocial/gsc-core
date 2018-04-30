package org.gsc.program;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.app.Application;
import org.gsc.common.app.ApplicationFactory;
import org.gsc.config.Args;
import org.gsc.config.DefaultConfig;
import org.gsc.core.Constant;
import org.gsc.service.RpcApiService;
import org.gsc.service.NetService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j

public class Start {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    Application appT = ApplicationFactory.create(context);
    shutdown(appT);
    //appT.init(cfgArgs);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT));
    }
    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }

  private static void shutdown(final Application app) {
  }
}

