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
import java.util.ArrayList;
import java.util.List;
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
import org.gsc.db.AbstractRevokingStore;
import org.gsc.db.RevokingDatabase;
import org.gsc.db.GSCStoreWithRevoking;
import org.gsc.db.db2.SnapshotRootTest.ProtoWrapperTest;
import org.gsc.db.db2.core.ISession;
import org.gsc.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingDbWithCacheOldValueTest {

  private AbstractRevokingStore revokingDatabase;
  private GSCApplicationContext context;
  private Application appT;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "db_revokingStore_test"}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = new TestRevokingGSCDatabase();
    revokingDatabase.enable();
  }

  @Test
  public synchronized void testReset() {
    revokingDatabase.getStack().clear();
    TestRevokingGSCStore gscDatabase = new TestRevokingGSCStore(
        "testrevokinggscstore-testReset", revokingDatabase);
    SnapshotRootTest.ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("reset").getBytes());
    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
      tmpSession.commit();
    }
    Assert.assertEquals(true, gscDatabase.has(testProtoWrapper.getData()));
    gscDatabase.reset();
    Assert.assertEquals(false, gscDatabase.has(testProtoWrapper.getData()));
    gscDatabase.reset();
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingGSCStore gscDatabase = new TestRevokingGSCStore(
        "testrevokinggscstore-testPop", revokingDatabase);

    for (int i = 1; i < 11; i++) {
      ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("pop" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        Assert.assertEquals(1, revokingDatabase.getActiveDialog());
        tmpSession.commit();
        Assert.assertEquals(i, revokingDatabase.getStack().size());
        Assert.assertEquals(0, revokingDatabase.getActiveDialog());
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, revokingDatabase.getStack().size());
    }

    gscDatabase.close();

    Assert.assertEquals(0, revokingDatabase.getStack().size());
  }

  @Test
  public synchronized void testUndo() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingGSCStore gscDatabase = new TestRevokingGSCStore(
        "testrevokinggscstore-testUndo", revokingDatabase);

    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    for (int i = 0; i < 10; i++) {
      ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("undo" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        Assert.assertEquals(2, revokingDatabase.getStack().size());
        tmpSession.merge();
        Assert.assertEquals(1, revokingDatabase.getStack().size());
      }
    }

    Assert.assertEquals(1, revokingDatabase.getStack().size());

    dialog.reset();

    Assert.assertTrue(revokingDatabase.getStack().isEmpty());
    Assert.assertEquals(0, revokingDatabase.getActiveDialog());

    dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    revokingDatabase.disable();
    ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest("del".getBytes());
    gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
    revokingDatabase.enable();

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), new SnapshotRootTest.ProtoWrapperTest("del2".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), new ProtoWrapperTest("del22".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoWrapper.getData(), new ProtoWrapperTest("del222".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.delete(testProtoWrapper.getData());
      tmpSession.merge();
    }

    dialog.reset();

    logger.info("**********testProtoWrapper:" + String
        .valueOf(gscDatabase.getUnchecked(testProtoWrapper.getData())));
    Assert.assertArrayEquals("del".getBytes(),
        gscDatabase.getUnchecked(testProtoWrapper.getData()).getData());
    Assert.assertEquals(testProtoWrapper, gscDatabase.getUnchecked(testProtoWrapper.getData()));

    gscDatabase.close();
  }

  @Test
  public synchronized void testGetlatestValues() {
    revokingDatabase.getStack().clear();
    TestRevokingGSCStore gscDatabase = new TestRevokingGSCStore(
        "testrevokinggscstore-testGetlatestValues", revokingDatabase);

    for (int i = 0; i < 10; i++) {
      ProtoWrapperTest testProtoWrapper = new SnapshotRootTest.ProtoWrapperTest(("getLastestValues" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        tmpSession.commit();
      }
    }
    Set<SnapshotRootTest.ProtoWrapperTest> result = gscDatabase.getRevokingDB().getlatestValues(5).stream()
        .map(SnapshotRootTest.ProtoWrapperTest::new)
        .collect(Collectors.toSet());

    for (int i = 9; i >= 5; i--) {
      Assert.assertEquals(true,
          result.contains(new ProtoWrapperTest(("getLastestValues" + i).getBytes())));
    }
    gscDatabase.close();
  }

  @Test
  public synchronized void testGetValuesNext() {
    revokingDatabase.getStack().clear();
    TestRevokingGSCStore gscDatabase = new TestRevokingGSCStore(
        "testrevokinggscstore-testGetValuesNext", revokingDatabase);

    for (int i = 0; i < 10; i++) {
      SnapshotRootTest.ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("getValuesNext" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
        tmpSession.commit();
      }
    }
    Set<ProtoWrapperTest> result =
        gscDatabase.getRevokingDB().getValuesNext(
            new ProtoWrapperTest("getValuesNext2".getBytes()).getData(), 3)
            .stream()
            .map(ProtoWrapperTest::new)
            .collect(Collectors.toSet());

    for (int i = 2; i < 5; i++) {
      Assert.assertEquals(true,
          result.contains(new ProtoWrapperTest(("getValuesNext" + i).getBytes())));
    }
    gscDatabase.close();
  }

  @Test
  public void shutdown() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingGSCStore gscDatabase = new TestRevokingGSCStore(
        "testrevokinggscstore-shutdown", revokingDatabase);

    List<SnapshotRootTest.ProtoWrapperTest> wrappers = new ArrayList<>();
    for (int i = 1; i < 11; i++) {
      revokingDatabase.buildSession();
      SnapshotRootTest.ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest(("test" + i).getBytes());
      wrappers.add(testProtoWrapper);
      gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
      Assert.assertEquals(revokingDatabase.getActiveDialog(), i);
      Assert.assertEquals(revokingDatabase.getStack().size(), i);
    }

    for (ProtoWrapperTest wrapper : wrappers) {
      logger.info(new String(wrapper.getData()));
      Assert.assertEquals(wrapper, gscDatabase.getUnchecked(wrapper.getData()));
    }

    revokingDatabase.shutdown();

    for (ProtoWrapperTest wrapper : wrappers) {
      logger.info(gscDatabase.getUnchecked(wrapper.getData()).toString());
      Assert.assertEquals(null, gscDatabase.getUnchecked(wrapper.getData()).getData());
    }

    Assert.assertEquals(0, revokingDatabase.getStack().size());
    gscDatabase.close();

  }

  private static class TestRevokingGSCStore extends GSCStoreWithRevoking<SnapshotRootTest.ProtoWrapperTest> {

    protected TestRevokingGSCStore(String dbName, RevokingDatabase revokingDatabase) {
      super(dbName, revokingDatabase);
    }

    @Override
    public ProtoWrapperTest get(byte[] key) {
      byte[] value = this.revokingDB.getUnchecked(key);
      return ArrayUtils.isEmpty(value) ? null : new SnapshotRootTest.ProtoWrapperTest(value);
    }
  }

  private static class TestRevokingGSCDatabase extends AbstractRevokingStore {

  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File("db_revokingStore_test"));
  }
}
