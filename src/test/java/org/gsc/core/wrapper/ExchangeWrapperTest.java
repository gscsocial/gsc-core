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

package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.db.StorageMarket;
import org.gsc.core.exception.ItemNotFoundException;

@Slf4j
public class ExchangeWrapperTest {

  private static Manager dbManager;
  private static StorageMarket storageMarket;
  private static final String dbPath = "db_exchange_wrapper_test_test";
  private static GSCApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000_000_000L;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    storageMarket = new StorageMarket(dbManager);
    //    Args.setParam(new String[]{"--db-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void createExchangeWrapper() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);

    long now = dbManager.getHeadBlockTimeStamp();
    ExchangeWrapper exchangeWrappere =
        new ExchangeWrapper(
            ByteString.copyFromUtf8("owner"),
            1,
            now,
            "abc".getBytes(),
            "def".getBytes());

    dbManager.getExchangeStore().put(exchangeWrappere.createDbKey(), exchangeWrappere);

  }

  @Test
  public void testExchange() {
    long sellBalance = 100000000L;
    long buyBalance = 100000000L;

    byte[] key = ByteArray.fromLong(1);

    ExchangeWrapper exchangeWrapper;
    try {
      exchangeWrapper = dbManager.getExchangeStore().get(key);
      exchangeWrapper.setBalance(sellBalance, buyBalance);

      long sellQuant = 1_000_000L;
      byte[] sellID = "abc".getBytes();

      long result = exchangeWrapper.transaction(sellID, sellQuant);
      Assert.assertEquals(990_099L, result);
      sellBalance += sellQuant;
      Assert.assertEquals(sellBalance, exchangeWrapper.getFirstTokenBalance());
      buyBalance -= result;
      Assert.assertEquals(buyBalance, exchangeWrapper.getSecondTokenBalance());

      sellQuant = 9_000_000L;
      long result2 = exchangeWrapper.transaction(sellID, sellQuant);
      Assert.assertEquals(9090909L, result + result2);
      sellBalance += sellQuant;
      Assert.assertEquals(sellBalance, exchangeWrapper.getFirstTokenBalance());
      buyBalance -= result2;
      Assert.assertEquals(buyBalance, exchangeWrapper.getSecondTokenBalance());

    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

  }


}
