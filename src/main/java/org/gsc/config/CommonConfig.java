package org.gsc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "org.gsc")
public class CommonConfig {

    private static final Logger logger = LoggerFactory.getLogger("general");

    private static CommonConfig defaultInstance;
}
