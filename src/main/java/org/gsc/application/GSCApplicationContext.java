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
package org.gsc.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.net.discover.DiscoverServer;
import org.gsc.net.node.NodeManager;
import org.gsc.net.server.ChannelManager;
import org.gsc.db.Manager;

public class GSCApplicationContext extends AnnotationConfigApplicationContext {

    public GSCApplicationContext() {
    }

    public GSCApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    public GSCApplicationContext(Class<?>... annotatedClasses) {
        super(annotatedClasses);
    }

    public GSCApplicationContext(String... basePackages) {
        super(basePackages);
    }

    @Override
    public void destroy() {

        Application appT = ApplicationFactory.create(this);
        appT.shutdownServices();
        appT.shutdown();

        DiscoverServer discoverServer = getBean(DiscoverServer.class);
        discoverServer.close();
        ChannelManager channelManager = getBean(ChannelManager.class);
        channelManager.close();
        NodeManager nodeManager = getBean(NodeManager.class);
        nodeManager.close();

        Manager dbManager = getBean(Manager.class);
        dbManager.stopRepushThread();
        dbManager.stopRepushTriggerThread();
        super.destroy();
    }
}
