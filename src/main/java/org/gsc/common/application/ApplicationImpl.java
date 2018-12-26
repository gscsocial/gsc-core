package org.gsc.common.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.config.args.Args;
import org.gsc.db.BlockStore;
import org.gsc.db.Manager;
import org.gsc.net.node.Node;
import org.gsc.net.node.NodeDelegate;
import org.gsc.net.node.NodeDelegateImpl;
import org.gsc.net.node.NodeImpl;

@Slf4j
@Component
public class ApplicationImpl implements Application {

  @Autowired
  private NodeImpl p2pNode;

  private BlockStore blockStoreDb;
  private ServiceContainer services;
  private NodeDelegate nodeDelegate;

  @Autowired
  private Manager dbManager;
  
  private boolean isProducer;

  private void resetP2PNode() {
    p2pNode.listen();
    p2pNode.syncFrom(null);
  }

  @Override
  public void setOptions(Args args) {

  }

  @Override
  @Autowired
  public void init(Args args) {
    //p2pNode = new NodeImpl();
    //p2pNode = ctx.getBean(NodeImpl.class);
//    dbManager.init();
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
    nodeDelegate = new NodeDelegateImpl(dbManager);
  }

  @Override
  public void initServices(Args args) {
    services.init(args);
  }
  
  @Override
  public void addService(Service service) {
    services.add(service);
  }

  public void startup() {
    p2pNode.setNodeDelegate(nodeDelegate);
    resetP2PNode();
  }

  @Override
  public void shutdown() {
    System.err.println("******** begin to shutdown ********");
    synchronized (dbManager.getRevokingStore()) {
      closeRevokingStore();
      closeAllStore();
    }
    closeConnection();
    dbManager.stopRepushThread();
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

  @Override
  public BlockStore getBlockStoreS() {
    return blockStoreDb;
  }

  @Override
  public Node getP2pNode() {
    return p2pNode;
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

  private void closeRevokingStore() {
    dbManager.getRevokingStore().shutdown();
  }

  private void closeAllStore() {
    dbManager.closeAllStore();
  }

}
