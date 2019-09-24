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

package org.gsc.db.db2;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.CheckTmpStore;
import org.gsc.db.db2.RevokingDbWithCacheNewValueTest.TestRevokingGSCStore;
import org.gsc.db.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.gsc.db.db2.SnapshotRootTest.ProtoWrapperTest;
import org.gsc.db.db2.core.ISession;
import org.gsc.db.db2.core.SnapshotManager;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;

@Slf4j
public class SnapshotManagerTest {

  private SnapshotManager revokingDatabase;
  private GSCApplicationContext context;
  private Application appT;
  private TestRevokingGSCStore gscDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "db_revokingStore_test"},
        Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    gscDatabase = new TestRevokingGSCStore("testSnapshotManager-test");
    revokingDatabase.add(gscDatabase.getRevokingDB());
    revokingDatabase.setCheckTmpStore(context.getBean(CheckTmpStore.class));
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    gscDatabase.close();
    FileUtil.deleteDir(new File("db_revokingStore_test"));
    revokingDatabase.getCheckTmpStore().getDbSource().closeDB();
    gscDatabase.close();
  }

  @Test
  public synchronized void testRefresh()
      throws BadItemException, ItemNotFoundException {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    SnapshotRootTest.ProtoWrapperTest protoWrapper = new ProtoWrapperTest("refresh".getBytes());
    for (int i = 1; i < 11; i++) {
      SnapshotRootTest.ProtoWrapperTest testProtoWrapper = new SnapshotRootTest.ProtoWrapperTest(("refresh" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(protoWrapper.getData(), testProtoWrapper);
        tmpSession.commit();
      }
    }

    revokingDatabase.flush();
    Assert.assertEquals(new SnapshotRootTest.ProtoWrapperTest("refresh10".getBytes()),
        gscDatabase.get(protoWrapper.getData()));
  }

  @Test
  public synchronized void testClose() {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoWrapperTest protoWrapper = new SnapshotRootTest.ProtoWrapperTest("close".getBytes());
    for (int i = 1; i < 11; i++) {
      SnapshotRootTest.ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("close" + i).getBytes());
      try (ISession iSession = revokingDatabase.buildSession()) {
        gscDatabase.put(protoWrapper.getData(), testProtoWrapper);
      }
    }
    Assert.assertEquals(null,
        gscDatabase.get(protoWrapper.getData()));

  }
}
