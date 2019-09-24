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

package org.gsc.utils;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class PropUtilTest {

  @Test
  public void testWriteProperty() {
    String filename = "test_prop.properties";
    File file = new File(filename);
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    PropUtil.writeProperty(filename, "key", "value");
    Assert.assertTrue("value".equals(PropUtil.readProperty(filename, "key")));
    PropUtil.writeProperty(filename, "key", "value2");
    Assert.assertTrue("value2".equals(PropUtil.readProperty(filename, "key")));
    file.delete();
  }

  @Test
  public void testReadProperty() {
    String filename = "test_prop.properties";
    File file = new File(filename);
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    PropUtil.writeProperty(filename, "key", "value");
    Assert.assertTrue("value".equals(PropUtil.readProperty(filename, "key")));
    file.delete();
    Assert.assertTrue("".equals(PropUtil.readProperty(filename, "key")));
  }
}