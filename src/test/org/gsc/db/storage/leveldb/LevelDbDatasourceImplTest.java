package org.gsc.db.storage.leveldb;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.FileUtil;
import org.junit.AfterClass;
import org.junit.Before;

import java.io.File;


@Slf4j
public class LevelDbDatasourceImplTest {

    private static final String dbPath = "output-levelDb-test";
    LevelDbDataSourceImpl dataSourceTest;

    @Before
    public void initDb() {
        //Args.setParam(new String[]{"--output-directory", dbPath}, "mainnet");
        //dataSourceTest = new LevelDbDataSourceImpl(dbPath + File.separator, "test_levelDb");
        //dataSourceTest = new LevelDbDataSourceImpl();
        //dataSourceTest.setDBName("test_levelDb");

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


}
