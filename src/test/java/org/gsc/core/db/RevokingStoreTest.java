package org.gsc.core.db;

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
import org.gsc.common.utils.DialogOptional;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.capsule.ProtoCapsule;
import org.gsc.core.config.args.Args;
import org.gsc.core.db.AbstractRevokingStore.Dialog;
import org.gsc.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingStoreTest {

  private AbstractRevokingStore revokingDatabase;

  @Before
  public void init() {
    revokingDatabase = new TestRevokingGscDatabase();
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
    TestRevokingGscStore gscDatabase = new TestRevokingGscStore(
        "testrevokinggscstore-testUndo", revokingDatabase);

    DialogOptional dialog = DialogOptional.instance().setValue(revokingDatabase.buildDialog());
    for (int i = 0; i < 10; i++) {
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("undo" + i).getBytes());
      try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
        gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertFalse(gscDatabase.getDbSource().allKeys().isEmpty());
        Assert.assertEquals(revokingDatabase.getStack().size(), 2);
        tmpDialog.merge();
        Assert.assertEquals(revokingDatabase.getStack().size(), 1);
      }
    }

    Assert.assertEquals(revokingDatabase.getStack().size(), 1);

    dialog.reset();

    Assert.assertTrue(revokingDatabase.getStack().isEmpty());
    Assert.assertTrue(gscDatabase.getDbSource().allKeys().isEmpty());
    Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);

    dialog = DialogOptional.instance().setValue(revokingDatabase.buildDialog());
    revokingDatabase.disable();
    TestProtoCapsule testProtoCapsule = new TestProtoCapsule("del".getBytes());
    gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    revokingDatabase.enable();

    try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
      gscDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del2".getBytes()));
      tmpDialog.merge();
    }

    try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
      gscDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del22".getBytes()));
      tmpDialog.merge();
    }

    try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
      gscDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del222".getBytes()));
      tmpDialog.merge();
    }

    try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
      gscDatabase.delete(testProtoCapsule.getData());
      tmpDialog.merge();
    }

    dialog.reset();

    logger.info("**********testProtoCapsule:" + String.valueOf(gscDatabase.get(testProtoCapsule.getData())));
    Assert.assertArrayEquals("del".getBytes(), gscDatabase.get(testProtoCapsule.getData()).getData());
    Assert.assertEquals(testProtoCapsule, gscDatabase.get(testProtoCapsule.getData()));

    gscDatabase.close();
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingGscStore gscDatabase = new TestRevokingGscStore(
        "testrevokinggscstore-testPop", revokingDatabase);

    for (int i = 1; i < 11; i++) {
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("pop" + i).getBytes());
      try (Dialog tmpDialog = revokingDatabase.buildDialog()) {
        gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertFalse(gscDatabase.getDbSource().allKeys().isEmpty());
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 1);
        tmpDialog.commit();
        Assert.assertEquals(revokingDatabase.getStack().size(), i);
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, gscDatabase.getDbSource().allKeys().size());
      Assert.assertEquals(10 - i, revokingDatabase.getStack().size());
    }

    Assert.assertTrue(gscDatabase.getDbSource().allKeys().isEmpty());
    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
  }

  @Test
  public void shutdown() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingGscStore gscDatabase = new TestRevokingGscStore(
        "testrevokinggscstore-shutdown", revokingDatabase);

    List<TestProtoCapsule> capsules = new ArrayList<>();
    for (int i = 1; i < 11; i++) {
      revokingDatabase.buildDialog();
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("test" + i).getBytes());
      capsules.add(testProtoCapsule);
      gscDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
      Assert.assertFalse(gscDatabase.getDbSource().allKeys().isEmpty());
      Assert.assertEquals(revokingDatabase.getActiveDialog(), i);
      Assert.assertEquals(revokingDatabase.getStack().size(), i);
    }

    for (TestProtoCapsule capsule : capsules) {
      logger.info(new String(capsule.getData()));
      Assert.assertEquals(capsule, gscDatabase.get(capsule.getData()));
    }

    revokingDatabase.shutdown();

    for (TestProtoCapsule capsule : capsules) {
      logger.info(gscDatabase.get(capsule.getData()).toString());
      Assert.assertEquals(null, gscDatabase.get(capsule.getData()).getData());
    }

    Assert.assertTrue(gscDatabase.getDbSource().allKeys().isEmpty());
    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
    gscDatabase.close();

  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class TestProtoCapsule implements ProtoCapsule<Object> {

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
      return "TestProtoCapsule{"
          + "value=" + Arrays.toString(value)
          + ", string=" + (value == null ? "" : new String(value))
          + '}';
    }
  }

  private static class TestRevokingGscStore extends GscStoreWithRevoking<TestProtoCapsule> {

    protected TestRevokingGscStore(String dbName, RevokingDatabase revokingDatabase) {
      super(dbName, revokingDatabase);
    }

    @Override
    public TestProtoCapsule get(byte[] key) {
      return new TestProtoCapsule(dbSource.getData(key));
    }

    @Override
    public boolean has(byte[] key) {
      return dbSource.getData(key) != null;
    }
  }

  private static class TestRevokingGscDatabase extends AbstractRevokingStore {

  }
}
