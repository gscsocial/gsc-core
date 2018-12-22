package org.gsc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.gsc.config.args.Args;
import org.gsc.db.RevokingDatabase;
import org.gsc.db.RevokingStore;
import org.gsc.db.api.IndexHelper;
import org.gsc.core.db2.core.SnapshotManager;

@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {

  private static Logger logger = LoggerFactory.getLogger("general");

  @Autowired
  CommonConfig commonConfig;
  
  @Autowired
  ApplicationContext appCtx;

  public DefaultConfig() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception", e));
  }

  @Bean
  public IndexHelper indexHelper() {
    if (!Args.getInstance().isSolidityNode()) {
      return null;
    }
    return new IndexHelper();
  }

  @Bean
  public RevokingDatabase revokingDatabase() {
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (dbVersion == 1) {
      return RevokingStore.getInstance();
    } else if (dbVersion == 2) {
      return new SnapshotManager();
    } else {
      throw new RuntimeException("DB version is error.");
    }
  }

}
