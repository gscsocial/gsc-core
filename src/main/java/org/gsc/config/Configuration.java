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

package org.gsc.config;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.typesafe.config.ConfigFactory;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "app")
public class Configuration {

    private static com.typesafe.config.Config config;

    /**
     * Get configuration by a given path.
     *
     * @param confFileName path to configuration file
     * @return loaded configuration
     */
    public static com.typesafe.config.Config getByFileName(final String shellConfFileName,
                                                           final String confFileName) {
        if (isNoneBlank(shellConfFileName)) {
            File shellConfFile = new File(shellConfFileName);
            resolveConfigFile(shellConfFileName, shellConfFile);
            return config;
        }

        if (isBlank(confFileName)) {
            throw new IllegalArgumentException("Configuration path is required!");
        } else {
            File confFile = new File(confFileName);
            resolveConfigFile(confFileName, confFile);
            return config;
        }
    }

    private static void resolveConfigFile(String fileName, File confFile) {
        if (confFile.exists()) {
            config = ConfigFactory.parseFile(confFile);
        } else if (Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)
                != null) {
            config = ConfigFactory.load(fileName);
        } else {
            throw new IllegalArgumentException(
                    "Configuration path is required! No Such file " + fileName);
        }
    }
}

