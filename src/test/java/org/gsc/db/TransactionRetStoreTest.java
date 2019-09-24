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

import org.gsc.core.wrapper.TransactionInfoWrapper;
import org.gsc.core.wrapper.TransactionRetWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol.Transaction;

public class TransactionRetStoreTest {

  private static String dbPath = "db_TransactionRetStore_test";
  private static String dbDirectory = "db_TransactionRetStore_test";
  private static String indexDirectory = "index_TransactionRetStore_test";
  private static GSCApplicationContext context;
  private static TransactionRetStore transactionRetStore;
  private static Transaction transaction;
  private static TransactionStore transactionStore;
  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);
  private static final byte[] blockNum = ByteArray.fromLong(1);

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
    transactionRetStore = context.getBean(TransactionRetStore.class);
    transactionStore = context.getBean(TransactionStore.class);
    TransactionInfoWrapper transactionInfoWrapper = new TransactionInfoWrapper();

    transactionInfoWrapper.setId(transactionId);
    transactionInfoWrapper.setFee(1000L);
    transactionInfoWrapper.setBlockNumber(100L);
    transactionInfoWrapper.setBlockTimeStamp(200L);

    TransactionRetWrapper transactionRetWrapper = new TransactionRetWrapper();
    transactionRetWrapper.addTransactionInfo(transactionInfoWrapper.getInstance());
    transactionRetStore.put(blockNum, transactionRetWrapper);
    transaction = Transaction.newBuilder().build();
    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
    transactionWrapper.setBlockNum(1);
    transactionStore.put(transactionId, transactionWrapper);
  }

  @Test
  public void get() throws BadItemException {
    TransactionInfoWrapper resultWrapper = transactionRetStore.getTransactionInfo(transactionId);
    Assert.assertNotNull("get transaction ret store", resultWrapper);
  }

  @Test
  public void put() {
    TransactionInfoWrapper transactionInfoWrapper = new TransactionInfoWrapper();
    transactionInfoWrapper.setId(transactionId);
    transactionInfoWrapper.setFee(1000L);
    transactionInfoWrapper.setBlockNumber(100L);
    transactionInfoWrapper.setBlockTimeStamp(200L);

    TransactionRetWrapper transactionRetWrapper = new TransactionRetWrapper();
    transactionRetWrapper.addTransactionInfo(transactionInfoWrapper.getInstance());
    Assert.assertNull("put transaction info error", transactionRetStore.getUnchecked(transactionInfoWrapper.getId()));
    transactionRetStore.put(transactionInfoWrapper.getId(), transactionRetWrapper);
    Assert.assertNotNull("get transaction info error", transactionRetStore.getUnchecked(transactionInfoWrapper.getId()));
  }
}