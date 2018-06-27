package org.gsc.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class ArgsTest {


    @Test
    public void test(){
        Args.configFile = "config.conf";
        ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
        Args config = context.getBean(Args.class);
        Assert.assertEquals("database", config.getStorage().getDbDirectory());
    }

}
