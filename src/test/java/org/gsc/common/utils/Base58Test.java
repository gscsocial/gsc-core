package org.gsc.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class Base58Test {

    @Test
    public void test(){
        String secretMsg = "2ab3df9a2f5e8f998347dsa2f89836284948372";
        String encodeMsg = Base58.encode(ByteArray.fromString(secretMsg));
        System.out.println(encodeMsg);
        Assert.assertEquals(secretMsg,new String(Base58.decode(encodeMsg)));
    }
}
