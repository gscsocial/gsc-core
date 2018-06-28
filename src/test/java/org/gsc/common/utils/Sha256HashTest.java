package org.gsc.common.utils;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertEquals;

public class Sha256HashTest {
    @Test
    public void testSha256_EmptyString() {
        String expected1 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String result1 = Hex.toHexString(Sha256Hash.hash(new byte[0]));
        assertEquals(expected1, result1);
    }

    @Test
    public void testSha256_Test() {
        String expected2 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
        String result2 = Hex.toHexString(Sha256Hash.hash("test".getBytes()));
        assertEquals(expected2, result2);
    }

    @Test
    public void testSha256_Multiple() {
        String expected1 = "1b4f0e9851971998e732078544c96b36c3d01cedf7caa332359d6f1d83567014";
        String result1 = Hex.toHexString(Sha256Hash.hash("test1".getBytes()));
        assertEquals(expected1, result1);

        String expected2 = "60303ae22b998861bce3b28f33eec1be758a213c86c93c076dbe9f558c11c752";
        String result2 = Hex.toHexString(Sha256Hash.hash("test2".getBytes()));
        assertEquals(expected2, result2);
    }


}
