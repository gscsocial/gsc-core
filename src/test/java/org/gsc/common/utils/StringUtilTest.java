package org.gsc.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class StringUtilTest {

    @Test
    public void testIsHexString(){
        Assert.assertFalse(StringUtil.isHexString("cba123yi",8));
        Assert.assertTrue(StringUtil.isHexString("cba123de",8));
        Assert.assertFalse(StringUtil.isHexString("cba123567def",10));
    }

}
