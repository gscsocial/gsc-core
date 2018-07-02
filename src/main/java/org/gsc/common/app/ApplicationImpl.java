package org.gsc.common.app;

import lombok.extern.slf4j.Slf4j;
import org.gsc.config.Args;
import org.gsc.core.sync.ChainController;
import org.gsc.core.sync.ChainControllerImpl;
import org.gsc.db.BlockStore;
import org.gsc.db.Manager;
import org.gsc.db.UndoStore;
import org.gsc.service.NetService;
import org.gsc.service.Service;
import org.gsc.service.ServiceContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationImpl implements Application {

  @Autowired
  private NetService p2pNode;

  private BlockStore blockStoreDb;
  private ServiceContainer services;
  private ChainController controller;

  @Autowired
  private Manager dbManager;

  private boolean isProducer;

  private void resetP2PNode() {
    p2pNode.listen();
    //p2pNode.connectToP2PNetWork();
    p2pNode.syncFrom(null);
  }

  @Override
  public void setOptions(Args args) {

  }

  @Override
  @Autowired
  public void init(Args args, ApplicationContext ctx) {
    //p2pNode = new NodeImpl();
    //p2pNode = ctx.getBean(NodeImpl.class);
//    dbManager.init();
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
    controller = ctx.getBean(ChainControllerImpl.class);
  }

  @Override
  public void addService(Service service) {
    services.add(service);
  }

  @Override
  public void initServices(Args args) {
    services.init(args);
  }

  /**
   * start up the app.
   */
  public void startup() {
    p2pNode.setChainController(controller);
    resetP2PNode();
  }

  @Override
  public void shutdown() {
    System.err.println("******** begin to shutdown ********");
    synchronized (UndoStore.getInstance()) {
      closeUndoStore();
      closeAllStore();
    }
    closeConnection();
    System.err.println("******** end to shutdown ********");
  }

  @Override
  public void startServices() {
    services.start();
  }

  @Override
  public void shutdownServices() {
    services.stop();
  }

  public NetService getP2pNode() {
    return p2pNode;
  }

  public BlockStore getBlockStoreS() {
    return blockStoreDb;
  }

  @Override
  public Manager getDbManager() {
    return dbManager;
  }

  public boolean isProducer() {
    return isProducer;
  }

  public void setIsProducer(boolean producer) {
    isProducer = producer;
  }

  private void closeConnection() {
    System.err.println("******** begin to shutdown connection ********");
    try {
      p2pNode.close();
    } catch (Exception e) {
      System.err.println("failed to close p2pNode. " + e);
    } finally {
      System.err.println("******** end to shutdown connection ********");
    }
  }

  private void closeUndoStore() { UndoStore.getInstance().shutdown();
  }

  private void closeAllStore() {
    dbManager.closeAllStore();
  }

}
