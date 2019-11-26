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

package org.gsc.core;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.net.client.DatabaseGrpcClient;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.services.RpcApiService;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.DynamicProperties;

@Slf4j
public class ConfirmedNodeTest {

  private static GSCApplicationContext context;

  private static RpcApiService rpcApiService;
  private static Application appT;
  private static String dbPath = "db_witness_test";

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    Args.getInstance().setConfirmedNode(true);
    appT = ApplicationFactory.create(context);
    rpcApiService = context.getBean(RpcApiService.class);
  }

  /**
   * init db.
   */
  @BeforeClass
  public static void init() {
    rpcApiService.start();
  }

  private static Boolean deleteFolder(File index) {
    if (!index.isDirectory() || index.listFiles().length <= 0) {
      return index.delete();
    }
    for (File file : index.listFiles()) {
      if (null != file && !deleteFolder(file)) {
        return false;
      }
    }
    return index.delete();
  }

  @Test
  public void testConfirmedArgs() {
    Assert.assertNotNull(Args.getInstance().getTrustNodeAddr());
    Assert.assertTrue(Args.getInstance().isConfirmedNode());
  }

  @Test
  public void testConfirmedGrpcCall() {
    DatabaseGrpcClient databaseGrpcClient = null;
    String addr = Args.getInstance().getTrustNodeAddr();
    try {
      databaseGrpcClient = new DatabaseGrpcClient(addr);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", addr);
    }

    Assert.assertNotNull(databaseGrpcClient);
    DynamicProperties dynamicProperties = databaseGrpcClient.getDynamicProperties();
    Assert.assertNotNull(dynamicProperties);

    Block genisisBlock = databaseGrpcClient.getBlock(0);
    Assert.assertNotNull(genisisBlock);
    Assert.assertFalse(genisisBlock.getTransactionsList().isEmpty());
  }
 /**
   * remove db when after test.
   */
  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    rpcApiService.stop();
    context.destroy();
    File dbFolder = new File(dbPath);
    if (deleteFolder(dbFolder)) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}
