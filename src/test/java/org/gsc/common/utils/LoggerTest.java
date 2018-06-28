package org.gsc.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class LoggerTest {

    @Test
    public void testLombokLogger(){
        logger.debug("this is a debug level message, timestamp={}",System.currentTimeMillis());
        logger.info("this is a info level message,timestamp={}",System.currentTimeMillis());
        logger.warn("this is a warn level message,timestamp={}",System.currentTimeMillis());
        logger.error("this is a error level message,timestamp={}",System.currentTimeMillis());
    }
}
