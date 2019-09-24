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

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalWitnessTest {

  private LocalWitnesses localWitness = new LocalWitnesses();

  @Before
  public void setLocalWitness() {
    localWitness
        .setPrivateKeys(
            Lists.newArrayList(
                "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62"));
  }

  @Test
  public void whenSetNullPrivateKey() {
    localWitness.setPrivateKeys(null);
  }

  @Test
  public void whenSetEmptyPrivateKey() {
    localWitness.setPrivateKeys(Lists.newArrayList(""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetBadFormatPrivateKey() {
    localWitness.setPrivateKeys(Lists.newArrayList("a111"));
  }

  @Test
  public void whenSetPrefixPrivateKey() {
    localWitness
        .setPrivateKeys(Lists
            .newArrayList("0xf31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62"));
    localWitness
        .setPrivateKeys(Lists
            .newArrayList("0Xf31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62"));
  }

  @Test
  public void getPrivateKey() {
    Assert.assertEquals(Lists
            .newArrayList("f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62"),
        localWitness.getPrivateKeys());
  }
}
