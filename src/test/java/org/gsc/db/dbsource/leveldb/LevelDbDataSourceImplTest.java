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


package org.gsc.db.dbsource.leveldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Sets;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.args.Args;

@Slf4j
public class LevelDbDataSourceImplTest {

  private static final String dbPath = "db-levelDb-test";
  private static LevelDbDataSourceImpl dataSourceTest;

  private byte[] value1 = "10000".getBytes();
  private byte[] value2 = "20000".getBytes();
  private byte[] value3 = "30000".getBytes();
  private byte[] value4 = "40000".getBytes();
  private byte[] value5 = "50000".getBytes();
  private byte[] value6 = "60000".getBytes();
  private byte[] value7 = "70000".getBytes();

  private byte[] key1 = "00000001aa".getBytes();
  private byte[] key2 = "00000002aa".getBytes();
  private byte[] key3 = "00000003aa".getBytes();
  private byte[] key4 = "00000004aa".getBytes();
  private byte[] key5 = "00000005aa".getBytes();
  private byte[] key6 = "00000006aa".getBytes();
  private byte[] key7 = "00000003ab".getBytes();
  @Before
  public void initDb() {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    dataSourceTest = new LevelDbDataSourceImpl(dbPath + File.separator, "test_levelDb");
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testPutGet() {
    dataSourceTest.resetDb();
    String key1 = "2c0937534dd1b3832d05d865e8e6f2bf23218300b33a992740d45ccab7d4f519";
    byte[] key = key1.getBytes();
    dataSourceTest.initDB();
    String value1 = "50000";
    byte[] value = value1.getBytes();

    dataSourceTest.putData(key, value);

    assertNotNull(dataSourceTest.getData(key));
    assertEquals(1, dataSourceTest.allKeys().size());
    assertEquals("50000", ByteArray.toStr(dataSourceTest.getData(key1.getBytes())));
    dataSourceTest.closeDB();
  }

  @Test
  public void testReset() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_reset");
    dataSource.resetDb();
    assertEquals(0, dataSource.allKeys().size());
    dataSource.closeDB();
  }

  @Test
  public void testupdateByBatchInner() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_updateByBatch");
    dataSource.initDB();
    dataSource.resetDb();
    String key1 = "431cd8c8d5abe5cb5944b0889b32482d85772fbb98987b10fbb7f17110757350";
    String value1 = "50000";
    String key2 = "431cd8c8d5abe5cb5944b0889b32482d85772fbb98987b10fbb7f17110757351";
    String value2 = "10000";

    Map<byte[], byte[]> rows = new HashMap<>();
    rows.put(key1.getBytes(), value1.getBytes());
    rows.put(key2.getBytes(), value2.getBytes());

    dataSource.updateByBatch(rows);

    assertEquals("50000", ByteArray.toStr(dataSource.getData(key1.getBytes())));
    assertEquals("10000", ByteArray.toStr(dataSource.getData(key2.getBytes())));
    assertEquals(2, dataSource.allKeys().size());
    dataSource.closeDB();
  }

  @Test
  public void testallKeys() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_find_key");
    dataSource.initDB();
    dataSource.resetDb();

    String key1 = "431cd8c8d5abe5cb5944b0889b32482d85772fbb98987b10fbb7f17110757321";
    byte[] key = key1.getBytes();

    String value1 = "50000";
    byte[] value = value1.getBytes();

    dataSource.putData(key, value);
    String key3 = "431cd8c8d5abe5cb5944b0889b32482d85772fbb98987b10fbb7f17110757091";
    byte[] key2 = key3.getBytes();

    String value3 = "30000";
    byte[] value2 = value3.getBytes();

    dataSource.putData(key2, value2);
    assertEquals(2, dataSource.allKeys().size());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test(timeout = 1000)
  public void testLockReleased() {
    dataSourceTest.initDB();
    // normal close
    dataSourceTest.closeDB();
    // closing already closed db.
    dataSourceTest.closeDB();
    // closing again to make sure the lock is free. If not test will hang.
    dataSourceTest.closeDB();

    assertFalse("Database is still alive after closing.", dataSourceTest.isAlive());
  }

  @Test
  public void testdeleteData() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
            Args.getInstance().getOutputDirectory(), "test_delete");
    dataSource.initDB();
    String key1 = "431cd8c8d5abe5cb5944b0889b32482d85772fbb98987b10fbb7f17110757350";
    byte[] key = key1.getBytes();
    dataSource.deleteData(key);
    byte[] value = dataSource.getData(key);
    String s = ByteArray.toStr(value);
    assertNull(s);
    dataSource.closeDB();
  }

  @Test
  public void allKeysTest() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_allKeysTest_key");
    dataSource.initDB();
    dataSource.resetDb();

    byte[] key = "0000000987b10fbb7f17110757321".getBytes();
    byte[] value = "50000".getBytes();
    byte[] key2 = "000000431cd8c8d5a".getBytes();
    byte[] value2 = "30000".getBytes();

    dataSource.putData(key, value);
    dataSource.putData(key2, value2);
    dataSource.allKeys().forEach(keyOne -> {
      logger.info(ByteArray.toStr(keyOne));
    });
    assertEquals(2, dataSource.allKeys().size());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  private void putSomeKeyValue(LevelDbDataSourceImpl dataSource) {
    value1 = "10000".getBytes();
    value2 = "20000".getBytes();
    value3 = "30000".getBytes();
    value4 = "40000".getBytes();
    value5 = "50000".getBytes();
    value6 = "60000".getBytes();
    key1 = "00000001aa".getBytes();
    key2 = "00000002aa".getBytes();
    key3 = "00000003aa".getBytes();
    key4 = "00000004aa".getBytes();
    key5 = "00000005aa".getBytes();
    key6 = "00000006aa".getBytes();

    dataSource.putData(key1, value1);
    dataSource.putData(key6, value6);
    dataSource.putData(key2, value2);
    dataSource.putData(key5, value5);
    dataSource.putData(key3, value3);
    dataSource.putData(key4, value4);
  }

  @Test
  public void seekTest() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_seek_key");
    dataSource.initDB();
    dataSource.resetDb();

    putSomeKeyValue(dataSource);
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void getValuesPrev() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getValuesPrev_key");
    dataSource.initDB();
    dataSource.resetDb();

    putSomeKeyValue(dataSource);
    Set<byte[]> seekKeyLimitNext = dataSource.getValuesPrev("0000000300".getBytes(), 2);
    HashSet<String> hashSet = Sets.newHashSet(ByteArray.toStr(value1), ByteArray.toStr(value2));
    seekKeyLimitNext.forEach(valeu -> {
      Assert.assertTrue("getValuesPrev1", hashSet.contains(ByteArray.toStr(valeu)));
    });
    seekKeyLimitNext = dataSource.getValuesPrev("0000000100".getBytes(), 2);
    Assert.assertEquals("getValuesPrev2", 0, seekKeyLimitNext.size());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void getValuesNext() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
            Args.getInstance().getOutputDirectory(), "test_getValuesNext_key");
    dataSource.initDB();
    dataSource.resetDb();

    putSomeKeyValue(dataSource);
    Set<byte[]> seekKeyLimitNext = dataSource.getValuesNext("0000000300".getBytes(), 2);
    HashSet<String> hashSet = Sets.newHashSet(ByteArray.toStr(value3), ByteArray.toStr(value4));
    seekKeyLimitNext.forEach(valeu -> {
      Assert.assertTrue("getValuesNext", hashSet.contains(ByteArray.toStr(valeu)));
    });
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void getPrevious() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getPrevious_key");
    dataSource.initDB();
    dataSource.resetDb();
    putSomeKeyValue(dataSource);
    dataSource.putData(key7, value7);

    Map<byte[], byte[]> seekKeyLimitNext = dataSource.getPrevious(key3, Long.MAX_VALUE, Long.SIZE / Byte.SIZE);
    Assert.assertEquals("getPrevious1",  4, seekKeyLimitNext.size());

    seekKeyLimitNext = dataSource.getPrevious(key3, Long.MAX_VALUE, 10);
    Assert.assertEquals("getPrevious2",  3, seekKeyLimitNext.size());
    dataSource.resetDb();
    dataSource.closeDB();
  }
}