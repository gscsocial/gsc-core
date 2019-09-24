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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.config.args.Args;
import org.gsc.db.BlockStore;
import org.gsc.db.Manager;
import org.gsc.net.GSCNetService;

@Slf4j(topic = "app")
@Component
public class ApplicationImpl implements Application {

    private BlockStore blockStoreDb;
    private ServiceContainer services;

    @Autowired
    private GSCNetService gscNetService;

    @Autowired
    private Manager dbManager;

    private boolean isProducer;

    @Override
    public void setOptions(Args args) {
        // not used
    }

    @Override
    @Autowired
    public void init(Args args) {
        blockStoreDb = dbManager.getBlockStore();
        services = new ServiceContainer();
    }

    @Override
    public void addService(Service service) {
        services.add(service);
    }

    @Override
    public void initServices(Args args) {
        services.init(args);
    }

    /**
     * start up the app.
     */
    public void startup() {
        gscNetService.start();
    }

    @Override
    public void shutdown() {
        logger.info("******** begin to shutdown ********");
        gscNetService.close();
        synchronized (dbManager.getRevokingStore()) {
            closeRevokingStore();
            closeAllStore();
        }
        dbManager.stopRepushThread();
        dbManager.stopRepushTriggerThread();
        EventPluginLoader.getInstance().stopPlugin();
        logger.info("******** end to shutdown ********");
    }

    @Override
    public void startServices() {
        services.start();
    }

    @Override
    public void shutdownServices() {
        services.stop();
    }

    @Override
    public BlockStore getBlockStoreS() {
        return blockStoreDb;
    }

    @Override
    public Manager getDbManager() {
        return dbManager;
    }

    public boolean isProducer() {
        return isProducer;
    }

    public void setIsProducer(boolean producer) {
        isProducer = producer;
    }

    private void closeRevokingStore() {
        logger.info("******** begin to closeRevokingStore ********");
        dbManager.getRevokingStore().shutdown();
    }

    private void closeAllStore() {
//    if (dbManager.getRevokingStore().getClass() == SnapshotManager.class) {
//      ((SnapshotManager) dbManager.getRevokingStore()).getDbs().forEach(IRevokingDB::close);
//    } else {
//      dbManager.closeAllStore();
//    }
        dbManager.closeAllStore();
    }

}
