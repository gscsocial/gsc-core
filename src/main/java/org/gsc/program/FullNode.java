package org.gsc.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.gsc.common.application.Application;
import org.gsc.common.application.ApplicationFactory;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.services.RpcApiService;
import org.gsc.services.WitnessService;
import org.gsc.services.http.FullNodeHttpApiService;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    System.out.println("***********************************************************************");
    System.out.println("***************************Starting GSC Node***************************");
    System.out.println("***********************************************************************");
    logger.info("GSC node running...");
    //Args.setParam(args, Constant.TESTNET_CONF);
    Args.setParam(args, Constant.KAY_CONF);
    Args cfgArgs = Args.getInstance();

    Args.getInstance().setDebug(true);
    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    if (Args.getInstance().isDebug()) {
      logger.info("in debug mode, it won't check energy time");
    } else {
      logger.info("not in debug mode, it will check energy time");
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    GSCApplicationContext context =
        new GSCApplicationContext(beanFactory);
    context.register(DefaultConfig.class);

    context.refresh();
    Application appT = ApplicationFactory.create(context);
    shutdown(appT);

    // grpc api server
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    // http api server
    FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
    appT.addService(httpApiService);
    System.out.println("**RPC/HTTP API Service Startup completed!******************************");

    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT, context));
      System.out.println("**mining program Startup completed!************************************");
    }

    appT.initServices(cfgArgs);
    appT.startServices();
    System.out.println("**API Service Startup completed!***************************************************");
    System.out.println("***********************************************************************");
    System.out.println("**GSC Node is running...****************************");
    System.out.println("***********************************************************************");
    appT.startup();

    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
