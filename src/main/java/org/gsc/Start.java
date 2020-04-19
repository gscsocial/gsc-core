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

package org.gsc;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

import java.io.File;

import lombok.extern.slf4j.Slf4j;
import org.gsc.services.interfaceOnConfirmed.ConfirmedRpcApiService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.services.RpcApiService;
import org.gsc.services.WitnessService;
import org.gsc.services.http.FullNodeHttpApiService;
import org.gsc.services.interfaceOnConfirmed.http.confirmed.ConfirmedHttpApiService;

@Slf4j(topic = "app")
public class Start {

    /**
     * Start the Start.
     */
    public static void main(String[] args) {
        logger.info("Full node running.");
        Args.setParam(args, Constant.MAIN_NET_CONF);
        Args cfgArgs = Args.getInstance();

        load(cfgArgs.getLogbackPath());

        if (cfgArgs.isHelp()) {
            logger.info("Here is the help message.");
            return;
        }

        if (Args.getInstance().isDebug()) {
            logger.info("in debug mode, it won't check cpu time");
        } else {
            logger.info("not in debug mode, it will check cpu time");
        }

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setAllowCircularReferences(false);
        GSCApplicationContext context =
                new GSCApplicationContext(beanFactory);
        context.register(DefaultConfig.class);

        context.refresh();
        Application application = ApplicationFactory.create(context);
        shutdown(application);

        RpcApiService rpcApiService = context.getBean(RpcApiService.class);
        application.addService(rpcApiService);
        if (cfgArgs.isWitness()) application.addService(new WitnessService(application, context));
        FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
        application.addService(httpApiService);

        if (Args.getInstance().getStorage().getDbVersion() == 2) {
            ConfirmedRpcApiService confirmedRpcApiService = context.getBean(ConfirmedRpcApiService.class);
            application.addService(confirmedRpcApiService);
            ConfirmedHttpApiService confirmedHttpApiService = context.getBean(ConfirmedHttpApiService.class);
            application.addService(confirmedHttpApiService);
        }
        application.initServices(cfgArgs);
        application.startServices();
        application.startup();

        rpcApiService.blockUntilShutdown();
    }

    public static void load(String path) {
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                return;
            }
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            lc.reset();
            configurator.doConfigure(file);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static void shutdown(final Application app) {
        logger.info("********register application shutdown hook********");
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
    }
}
