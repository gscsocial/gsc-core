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

@Slf4j
public class TypeConversionTest {

  @Test
  public void testLongToBytes() {
    byte[] result = TypeConversion.longToBytes(123L);
    //logger.info("long 123 to bytes is: {}", result);
    byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 123};
    assertArrayEquals(expected, result);

  }

  @Test
  public void testBytesToLong() {
    long result = TypeConversion.bytesToLong(new byte[]{0, 0, 0, 0, 0, 0, 0, 124});
    //logger.info("bytes 124 to long is: {}", result);
    assertEquals(124L, result);

  }

  @Test
  public void testBytesToHexString() {
    String result = TypeConversion.bytesToHexString(new byte[]{0, 0, 0, 0, 0, 0, 0, 125});
    //logger.info("bytes 125 to hex string is: {}", result);
    assertEquals("000000000000007d", result);
  }

  @Test
  public void testHexStringToBytes() {
    byte[] result = TypeConversion.hexStringToBytes("7f");
    //logger.info("hex string 7f to bytes is: {}", result);
    byte[] expected = new byte[]{127};
    assertArrayEquals(expected, result);

  }
}
