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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.FileUtil;
import org.gsc.utils.SessionOptional;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.GSCStoreWithRevoking;
import org.gsc.db.db2.SnapshotRootTest.ProtoWrapperTest;
import org.gsc.db.db2.core.ISession;
import org.gsc.db.db2.core.SnapshotManager;
import org.gsc.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingDbWithCacheNewValueTest {

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
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    gscDatabase = new TestRevokingGSCStore("testRevokingDBWithCacheNewValue-testPop");
    revokingDatabase.add(gscDatabase.getRevokingDB());

    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    for (int i = 1; i < 11; i++) {
      ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("pop" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        Assert.assertEquals(1, revokingDatabase.getActiveSession());
        tmpSession.commit();
        Assert.assertEquals(i, revokingDatabase.getSize());
        Assert.assertEquals(0, revokingDatabase.getActiveSession());
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, revokingDatabase.getSize());
    }

    Assert.assertEquals(0, revokingDatabase.getSize());
  }

  @Test
  public synchronized void testMerge() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    gscDatabase = new TestRevokingGSCStore("testRevokingDBWithCacheNewValue-testMerge");
    revokingDatabase.add(gscDatabase.getRevokingDB());

    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }
    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    dialog.setValue(revokingDatabase.buildSession());
    ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest("merge".getBytes());
    ProtoWrapperTest testProtoWrapper2 = new ProtoWrapperTest("merge2".getBytes());

    gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper2);
      tmpSession.merge();
    }
    Assert.assertEquals(testProtoWrapper2, gscDatabase.get(testProtoWrapper.getData()));

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.delete(testProtoWrapper.getData());
      tmpSession.merge();
    }
    Assert.assertEquals(null, gscDatabase.get(testProtoWrapper.getData()));
    dialog.reset();
  }


  @Test
  public synchronized void testRevoke() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    gscDatabase = new TestRevokingGSCStore("testRevokingDBWithCacheNewValue-testRevoke");
    revokingDatabase.add(gscDatabase.getRevokingDB());

    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }
    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    for (int i = 0; i < 10; i++) {
      ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("undo" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        Assert.assertEquals(2, revokingDatabase.getSize());
        tmpSession.merge();
        Assert.assertEquals(1, revokingDatabase.getSize());
      }
    }

    Assert.assertEquals(1, revokingDatabase.getSize());
    dialog.reset();
    Assert.assertTrue(revokingDatabase.getSize() == 0);
    Assert.assertEquals(0, revokingDatabase.getActiveSession());

    dialog.setValue(revokingDatabase.buildSession());
    ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest("revoke".getBytes());
    ProtoWrapperTest testProtoWrapper2 = new ProtoWrapperTest("revoke2".getBytes());
    gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
    dialog.setValue(revokingDatabase.buildSession());

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper2);
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), new ProtoWrapperTest("revoke22".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), new ProtoWrapperTest("revoke222".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.delete(testProtoWrapper.getData());
      tmpSession.merge();
    }

    dialog.reset();

    logger.info("**********testProtoWrapper:" + String
        .valueOf(gscDatabase.getUnchecked(testProtoWrapper.getData())));
    Assert.assertEquals(testProtoWrapper, gscDatabase.get(testProtoWrapper.getData()));
  }

  @Test
  public synchronized void testGetValuesNext() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    gscDatabase = new TestRevokingGSCStore("testSnapshotManager-testGetValuesNext");
    revokingDatabase.add(gscDatabase.getRevokingDB());
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    for (int i = 1; i < 10; i++) {
      ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("getValuesNext" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        tmpSession.commit();
      }
    }

    Set<ProtoWrapperTest> result =
        gscDatabase.getRevokingDB().getValuesNext(
            new ProtoWrapperTest("getValuesNext2".getBytes()).getData(), 3
        ).stream().map(ProtoWrapperTest::new).collect(Collectors.toSet());

    for (int i = 2; i < 5; i++) {
      Assert.assertEquals(true,
          result.contains(new ProtoWrapperTest(("getValuesNext" + i).getBytes())));
    }
  }

  @Test
  public synchronized void testGetlatestValues() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    gscDatabase = new TestRevokingGSCStore("testSnapshotManager-testGetlatestValues");
    revokingDatabase.add(gscDatabase.getRevokingDB());
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    for (int i = 1; i < 10; i++) {
      ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("getLastestValues" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        tmpSession.commit();
      }
    }

    Set<ProtoWrapperTest> result = gscDatabase.getRevokingDB().getlatestValues(5).stream()
            .map(ProtoWrapperTest::new)
            .collect(Collectors.toSet());

    for (int i = 9; i >= 5; i--) {
      Assert.assertEquals(true,
              result.contains(new ProtoWrapperTest(("getLastestValues" + i).getBytes())));
    }
  }

  public static class TestRevokingGSCStore extends GSCStoreWithRevoking<ProtoWrapperTest> {

    protected TestRevokingGSCStore(String dbName) {
      super(dbName);
    }

    @Override
    public ProtoWrapperTest get(byte[] key) {
      byte[] value = this.revokingDB.getUnchecked(key);
      return ArrayUtils.isEmpty(value) ? null : new ProtoWrapperTest(value);
    }
  }

  public static class TestSnapshotManager extends SnapshotManager {

  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    gscDatabase.close();
    FileUtil.deleteDir(new File("db_revokingStore_test"));
  }
}
