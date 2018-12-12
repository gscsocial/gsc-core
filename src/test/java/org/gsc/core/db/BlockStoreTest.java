package org.gsc.core.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.db.BlockStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;

@Slf4j
public class BlockStoreTest {

  private static final String dbPath = "output-blockStore-test";
  BlockStore blockStore;
  private static AnnotationConfigApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
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
