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

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.WitnessWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;

@Slf4j
public class WitnessStoreTest {

  private static final String dbPath = "db-witnessStore-test";
  private static GSCApplicationContext context;
  WitnessStore witnessStore;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @Before
  public void initDb() {
    this.witnessStore = context.getBean(WitnessStore.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void putAndGetWitness() {
    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFromUtf8("100000000x"), 100L,
        "");

    this.witnessStore.put(witnessWrapper.getAddress().toByteArray(), witnessWrapper);
    WitnessWrapper witnessSource = this.witnessStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(witnessWrapper.getAddress(), witnessSource.getAddress());
    Assert.assertEquals(witnessWrapper.getVoteCount(), witnessSource.getVoteCount());

    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), witnessSource.getAddress());
    Assert.assertEquals(100L, witnessSource.getVoteCount());

    witnessWrapper = new WitnessWrapper(ByteString.copyFromUtf8(""), 100L, "");

    this.witnessStore.put(witnessWrapper.getAddress().toByteArray(), witnessWrapper);
    witnessSource = this.witnessStore.get(ByteString.copyFromUtf8("").toByteArray());
    Assert.assertEquals(witnessWrapper.getAddress(), witnessSource.getAddress());
    Assert.assertEquals(witnessWrapper.getVoteCount(), witnessSource.getVoteCount());

    Assert.assertEquals(ByteString.copyFromUtf8(""), witnessSource.getAddress());
    Assert.assertEquals(100L, witnessSource.getVoteCount());
  }


}