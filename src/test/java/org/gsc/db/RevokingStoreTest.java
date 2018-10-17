package org.gsc.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.common.utils.SessionOptional;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.ProtoWrapper;
import org.gsc.config.args.Args;
import org.gsc.core.db2.core.ISession;
import org.gsc.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingStoreTest {

  private AbstractRevokingStore revokingDatabase;

  @Before
  public void init() {
    revokingDatabase = new TestRevokinggscDatabase();
    revokingDatabase.enable();
    Args.setParam(new String[]{"-d", "output_revokingStore_test"},
        Constant.TEST_CONF);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testUndo() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokinggscStore gscDatabase = new TestRevokinggscStore(
        "testrevokinggscstore-testUndo", revokingDatabase);

    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    for (int i = 0; i < 10; i++) {
      TestProtoWrapper testProtoCapsule = new TestProtoWrapper(("undo" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(revokingDatabase.getStack().size(), 2);
        tmpSession.merge();
        Assert.assertEquals(revokingDatabase.getStack().size(), 1);
      }
    }

    Assert.assertEquals(revokingDatabase.getStack().size(), 1);

    dialog.reset();

    Assert.assertTrue(revokingDatabase.getStack().isEmpty());
    Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);

    dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    revokingDatabase.disable();
    TestProtoWrapper testProtoCapsule = new TestProtoWrapper("del".getBytes());
    gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    revokingDatabase.enable();

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoCapsule.getData(), new TestProtoWrapper("del2".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoCapsule.getData(), new TestProtoWrapper("del22".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.put(testProtoCapsule.getData(), new TestProtoWrapper("del222".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      gscDatabase.delete(testProtoCapsule.getData());
      tmpSession.merge();
    }

    dialog.reset();

    logger.info("**********testProtoCapsule:" + String.valueOf(gscDatabase.getUnchecked(testProtoCapsule.getData())));
    Assert.assertArrayEquals("del".getBytes(), gscDatabase.getUnchecked(testProtoCapsule.getData()).getData());
    Assert.assertEquals(testProtoCapsule, gscDatabase.getUnchecked(testProtoCapsule.getData()));

    gscDatabase.close();
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokinggscStore gscDatabase = new TestRevokinggscStore(
        "testrevokinggscstore-testPop", revokingDatabase);

    for (int i = 1; i < 11; i++) {
      TestProtoWrapper testProtoCapsule = new TestProtoWrapper(("pop" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 1);
        tmpSession.commit();
        Assert.assertEquals(revokingDatabase.getStack().size(), i);
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, revokingDatabase.getStack().size());
    }

    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
  }

  @Test
  public void shutdown() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokinggscStore gscDatabase = new TestRevokinggscStore(
        "testrevokinggscstore-shutdown", revokingDatabase);

    List<TestProtoWrapper> capsules = new ArrayList<>();
    for (int i = 1; i < 11; i++) {
      revokingDatabase.buildSession();
      TestProtoWrapper testProtoCapsule = new TestProtoWrapper(("test" + i).getBytes());
      capsules.add(testProtoCapsule);
      gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
      Assert.assertEquals(revokingDatabase.getActiveDialog(), i);
      Assert.assertEquals(revokingDatabase.getStack().size(), i);
    }

    for (TestProtoWrapper capsule : capsules) {
      logger.info(new String(capsule.getData()));
      Assert.assertEquals(capsule, gscDatabase.getUnchecked(capsule.getData()));
    }

    revokingDatabase.shutdown();

    for (TestProtoWrapper capsule : capsules) {
      logger.info(gscDatabase.getUnchecked(capsule.getData()).toString());
      Assert.assertEquals(null, gscDatabase.getUnchecked(capsule.getData()).getData());
    }

    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
    gscDatabase.close();

  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class TestProtoWrapper implements ProtoWrapper<Object> {

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
      return "TestProtoWrapper{"
          + "value=" + Arrays.toString(value)
          + ", string=" + (value == null ? "" : new String(value))
          + '}';
    }
  }

  private static class TestRevokinggscStore extends GSCStoreWithRevoking<TestProtoWrapper> {

    protected TestRevokinggscStore(String dbName, RevokingDatabase revokingDatabase) {
      super(dbName, revokingDatabase);
    }
  }

  private static class TestRevokinggscDatabase extends AbstractRevokingStore {

  }
}
