package org.gsc.db.storage.leveldb;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.FileUtil;
import org.gsc.config.Args;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


@Slf4j
public class LevelDbDatasourceImplTest {

    private static final String dbPath = "output-levelDb-test";
    LevelDbDataSourceImpl dataSourceTest;

    @Before
    public void initDb() {
        Args.configFile = "config-localtest.conf";
        dataSourceTest = new LevelDbDataSourceImpl(dbPath + File.separator, "test_levelDb");
        dataSourceTest.setDBName("test_levelDb");

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
    public void test(){

    }




}
