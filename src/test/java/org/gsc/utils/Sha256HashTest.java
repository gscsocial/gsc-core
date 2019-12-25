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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Sha256HashTest {

  @Test
  public void testHash() {
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    Assert.assertEquals(hash0, ByteArray
        .fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084"));
    Assert.assertEquals(hash1, ByteArray
        .fromHexString("10AE21E887E8FE30C591A22A5F8BB20EB32B2A739486DC5F3810E00BBDB58C5C"));

  }

  @Test
  public void testMultiThreadingHash() {
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash = ByteArray
        .fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084");
    AtomicLong countFailed = new AtomicLong(0);
    AtomicLong countAll = new AtomicLong(0);
    IntStream.range(0, 7).parallel().forEach(index -> {
      Thread thread =
          new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
              byte[] hash0 = Sha256Hash.hash(input);
              countAll.incrementAndGet();
              if (!Arrays.equals(hash, hash0)) {
                countFailed.incrementAndGet();
                Assert.assertTrue(false);
              }
            }
          });
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    Assert.assertEquals(70000, countAll.get());
    Assert.assertEquals(0, countFailed.get());
  }
}