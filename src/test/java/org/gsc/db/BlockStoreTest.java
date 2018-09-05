package org.gsc.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;

@Slf4j
public class BlockStoreTest {

  private static final String dbPath = "output-blockStore-test";
  BlockStore blockStore;
  private static GSCApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @Before
  public void init() {
    blockStore = context.getBean(BlockStore.class);
  }

  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();

  }

  @Test
  public void testCreateBlockStore() {
  }
}
