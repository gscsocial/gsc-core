package org.gsc.db.storage.leveldb;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.config.Args;
import org.gsc.config.DefaultConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@Slf4j
public class LevelDbDatasourceImplTest {

    private static final String dbPath = "output-levelDb-test";
    LevelDbDataSourceImpl dataSourceTest;

    @Before
    public void initDb() {
        Args.configFile = "config-localtest.conf";
        ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
        dataSourceTest = context.getBean(LevelDbDataSourceImpl.class);
    }

    /**
     * Release resources.
     */
    @AfterClass
    public static void destroy() {
        //Args.clearParam();
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

    }

    @Test
    public void testupdateByBatchInner() {
        LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
                dbPath, "test_updateByBatch");
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
    }
    @Test
    public void testdeleteData() {
        LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
                dbPath, "test_delete");
        dataSource.initDB();
        String key1 = "431cd8c8d5abe5cb5944b0889b32482d85772fbb98987b10fbb7f17110757350";
        byte[] key = key1.getBytes();
        dataSource.deleteData(key);
        byte[] value = dataSource.getData(key);
        String s = ByteArray.toStr(value);
        assertNull(s);

    }
}
