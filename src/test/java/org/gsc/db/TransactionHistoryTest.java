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

package org.gsc.db;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.TransactionInfoWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BadItemException;

public class TransactionHistoryTest {

  private static String dbPath = "db_TransactionHistoryStore_test";
  private static String dbDirectory = "db_TransactionHistoryStore_test";
  private static String indexDirectory = "index_TransactionHistoryStore_test";
  private static GSCApplicationContext context;
  private static TransactionHistoryStore transactionHistoryStore;
  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);

  static {
    Args.setParam(
        new String[]{
            "--db-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_NET_CONF
    );
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    transactionHistoryStore = context.getBean(TransactionHistoryStore.class);
    TransactionInfoWrapper transactionInfoWrapper = new TransactionInfoWrapper();

    transactionInfoWrapper.setId(transactionId);
    transactionInfoWrapper.setFee(1000L);
    transactionInfoWrapper.setBlockNumber(100L);
    transactionInfoWrapper.setBlockTimeStamp(200L);
    transactionHistoryStore.put(transactionId, transactionInfoWrapper);
  }

  @Test
  public void get() throws BadItemException {
    //test get and has Method
    TransactionInfoWrapper resultWrapper = transactionHistoryStore.get(transactionId);
    Assert.assertEquals(1000L, resultWrapper.getFee());
    Assert.assertEquals(100L, resultWrapper.getBlockNumber());
    Assert.assertEquals(200L, resultWrapper.getBlockTimeStamp());
    Assert.assertEquals(ByteArray.toHexString(transactionId),
        ByteArray.toHexString(resultWrapper.getId()));
  }
}