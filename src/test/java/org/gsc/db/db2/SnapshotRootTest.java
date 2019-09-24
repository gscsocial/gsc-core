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
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
import org.gsc.core.wrapper.ProtoWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.db2.RevokingDbWithCacheNewValueTest.TestRevokingGSCStore;
import org.gsc.db.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.gsc.db.db2.core.ISession;
import org.gsc.db.db2.core.Snapshot;
import org.gsc.db.db2.core.SnapshotManager;
import org.gsc.db.db2.core.SnapshotRoot;

public class SnapshotRootTest {

  private TestRevokingGSCStore gscDatabase;
  private GSCApplicationContext context;
  private Application appT;
  private SnapshotManager revokingDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "db_revokingStore_test"}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File("db_revokingStore_test"));
  }

  @Test
  public synchronized void testRemove() {
    ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest("test".getBytes());
    gscDatabase = new TestRevokingGSCStore("testSnapshotRoot-testRemove");
    gscDatabase.put("test".getBytes(), testProtoWrapper);
    Assert.assertEquals(testProtoWrapper, gscDatabase.get("test".getBytes()));

    gscDatabase.delete("test".getBytes());
    Assert.assertEquals(null, gscDatabase.get("test".getBytes()));
    gscDatabase.close();
  }

  @Test
  public synchronized void testMerge() {
    gscDatabase = new TestRevokingGSCStore("testSnapshotRoot-testMerge");
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    revokingDatabase.add(gscDatabase.getRevokingDB());

    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest("merge".getBytes());
    gscDatabase.put(testProtoWrapper.getData(), testProtoWrapper);
    revokingDatabase.getDbs().forEach(db -> db.getHead().getRoot().merge(db.getHead()));
    dialog.reset();
    Assert.assertEquals(gscDatabase.get(testProtoWrapper.getData()), testProtoWrapper);

    gscDatabase.close();
  }

  @Test
  public synchronized void testMergeList() {
    gscDatabase = new TestRevokingGSCStore("testSnapshotRoot-testMergeList");
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    revokingDatabase.add(gscDatabase.getRevokingDB());

    SessionOptional.instance().setValue(revokingDatabase.buildSession());
    ProtoWrapperTest testProtoWrapper = new ProtoWrapperTest("test".getBytes());
    gscDatabase.put("merge".getBytes(), testProtoWrapper);
    for (int i = 1; i < 11; i++) {
      ProtoWrapperTest tmpProtoWrapper = new ProtoWrapperTest(("mergeList" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(tmpProtoWrapper.getData(), tmpProtoWrapper);
        tmpSession.commit();
      }
    }
    revokingDatabase.getDbs().forEach(db -> {
      List<Snapshot> snapshots = new ArrayList<>();
      SnapshotRoot root = (SnapshotRoot) db.getHead().getRoot();
      Snapshot next = root;
      for (int i = 0; i < 11; ++i) {
        next = next.getNext();
        snapshots.add(next);
      }
      root.merge(snapshots);
      root.resetConfirmed();

      for (int i = 1; i < 11; i++) {
        ProtoWrapperTest tmpProtoWrapper = new ProtoWrapperTest(("mergeList" + i).getBytes());
        Assert.assertEquals(tmpProtoWrapper, gscDatabase.get(tmpProtoWrapper.getData()));
      }

    });
    revokingDatabase.updateConfirmed(10);
    gscDatabase.close();
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class ProtoWrapperTest implements ProtoWrapper<Object> {

    private byte[] value;

    @Override
    public byte[] getData() {
      return value;
    }

    @Override
    public Object getInstance() {
      return value;
    }

    @Override
    public String toString() {
      return "ProtoWrapperTest{"
          + "value=" + Arrays.toString(value)
          + ", string=" + (value == null ? "" : new String(value))
          + '}';
    }
  }
}
