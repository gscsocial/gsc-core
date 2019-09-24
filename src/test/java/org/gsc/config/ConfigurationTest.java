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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;

@Slf4j
public class ConfigurationTest {

  @Before
  public void resetSingleton()
      throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    Field instance = Configuration.class.getDeclaredField("config");
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  public void testGetEcKey() {
    ECKey key = ECKey.fromPrivate(
        Hex.decode("1cd5a70741c6e583d2dd3c5f17231e608eb1e52437210d948c5085e141c2d830"));

//    log.debug("address = {}", ByteArray.toHexString(key.getOwnerAddress()));

    assertEquals(Wallet.getAddressPreFixString() + "125b6c87b3d67114b3873977888c34582f27bbb0",
        ByteArray.toHexString(key.getAddress()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenNullPathGetShouldThrowIllegalArgumentException() {
    Configuration.getByFileName(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenEmptyPathGetShouldThrowIllegalArgumentException() {
    Configuration.getByFileName(StringUtils.EMPTY, StringUtils.EMPTY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getShouldNotFindConfiguration() {
    Config config = Configuration.getByFileName("notExistingPath", "notExistingPath");
    assertFalse(config.hasPath("storage"));
    assertFalse(config.hasPath("overlay"));
    assertFalse(config.hasPath("node.discovery.boot"));
    assertFalse(config.hasPath("genesis.block"));
  }

  @Test
  public void getShouldReturnConfiguration() {
    Config config = Configuration.getByFileName(Constant.TEST_NET_CONF, Constant.TEST_NET_CONF);
    assertTrue(config.hasPath("storage"));
    assertTrue(config.hasPath("node.discovery.boot"));
    assertTrue(config.hasPath("genesis.block"));
  }
}
