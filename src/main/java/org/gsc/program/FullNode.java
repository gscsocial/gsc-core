package org.gsc.program;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.gsc.core.Constant;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.api.GrpcAPI.AddressPrKeyPairMessage;
import org.gsc.common.application.Application;
import org.gsc.common.application.ApplicationFactory;
import org.gsc.crypto.ECKey;
import org.gsc.common.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.services.RpcApiService;
import org.gsc.services.WitnessService;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(beanFactory);
    context.register(DefaultConfig.class);
    context.refresh();
    Application appT = ApplicationFactory.create(context);
    shutdown(appT);
    //appT.init(cfgArgs);

    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT, context));
    }
    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }

  private static void generate(){
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] priKey = ecKey.getPrivKeyBytes();
    byte[] address = ecKey.getAddress();
    String addressStr = Wallet.encode58Check(address);
    String priKeyStr = Hex.encodeHexString(priKey);
    AddressPrKeyPairMessage.Builder builder = AddressPrKeyPairMessage.newBuilder();
    System.out.println(addressStr);
    System.out.println(priKeyStr);
  }
}
