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

package org.gsc.wallet.common.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;


public class Configuration {

  private static Config config;

  private static final Logger logger = LoggerFactory.getLogger("Configuration");

  /**
   * constructor.
   */

  public static Config getByPath(final String configurationPath) {
    if (isBlank(configurationPath)) {
      throw new IllegalArgumentException("Configuration path is required!");
    }

    if (config == null) {
      File configFile = new File(System.getProperty("user.dir") + '/' + configurationPath);
      if (configFile.exists()) {
        try {
          config = ConfigFactory.parseReader(new InputStreamReader(new
              FileInputStream(configurationPath)));
          logger.info("use user defined config file in current dir");
        } catch (FileNotFoundException e) {
          logger.error("load user defined config file exception: " + e.getMessage());
        }
      } else {
        config = ConfigFactory.load(configurationPath);
        logger.info("user defined config file doesn't exists, use default config file in jar");
      }
    }
    return config;
  }
}
