package org.gsc.common.utils;

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
        //logger.info("Byte: hex string 1 to byte = {}", ByteArray.fromHexString("1"));
        byte[] expectedthird = new byte[]{1};
        byte[] actualthird = ByteArray.fromHexString("1");
        assertArrayEquals(expectedthird, actualthird);
    }

    @Test
    public void testToLong() {
        assertEquals("byte to long is wrong", 13L, ByteArray.toLong(new byte[]{13}));

    }

    @Test
    public void testFromLong() {
        byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 127};
        byte[] actual = ByteArray.fromLong(127L);
        assertArrayEquals(expected, actual);

    }

    @Test
    public void test2ToHexString() {
        byte[] bss = new byte[]{8, 9, 12, 13, 14, 15, 16};
        assertEquals("ByteArray.toHexString is not equals Hex.toHexString", ByteArray.toHexString(bss),
                Hex.toHexString(bss));
    }
}
