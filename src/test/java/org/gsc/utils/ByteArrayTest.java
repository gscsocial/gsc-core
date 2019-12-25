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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

@Slf4j
public class ByteArrayTest {

  @Test
  public void testToHexString() {
    assertEquals("byte to hex string is wrong", "10", ByteArray.toHexString(new byte[]{16}));
  }

  @Test
  public void long2Bytes() {
    long a = 0x123456;
    byte[] bb = ByteArray.fromLong(a);
    System.out.println(bb[6]);
    System.out.println(bb[7]);
  }

  @Test
  public void testHexStringToByte() {
    byte[] expectedfirst = new byte[]{17};
    byte[] actualfirst = ByteArray.fromHexString("0x11");
    assertArrayEquals(expectedfirst, actualfirst);
    byte[] expectedsecond = new byte[]{16};
    byte[] actualsecond = ByteArray.fromHexString("10");
    assertArrayEquals(expectedsecond, actualsecond);
    byte[] expectedthird = new byte[]{1};
    byte[] actualthird = ByteArray.fromHexString("1");
    assertArrayEquals(expectedthird, actualthird);
  }

  @Test
  public void testFromLong() {
    byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 127};
    byte[] actual = ByteArray.fromLong(127L);
    assertArrayEquals(expected, actual);

  }

  @Test
  public void testToLong() {
    assertEquals("byte to long is wrong", 13L, ByteArray.toLong(new byte[]{13}));

  }

  @Test
  public void test2ToHexString() {
    byte[] bss = new byte[]{8, 9, 12, 13, 14, 15, 16};
    assertEquals("ByteArray.toHexString is not equals Hex.toHexString", ByteArray.toHexString(bss),
        Hex.toHexString(bss));
  }
}
