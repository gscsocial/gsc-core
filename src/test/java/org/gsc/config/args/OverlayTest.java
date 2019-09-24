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

package org.gsc.config.args;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OverlayTest {

  private Overlay overlay = new Overlay();

  @Before
  public void setOverlay() {
    overlay.setPort(8080);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetOutOfBoundsPort() {
    overlay.setPort(-1);
  }

  @Test
  public void getOverlay() {
    Assert.assertEquals(8080, overlay.getPort());
  }
}
