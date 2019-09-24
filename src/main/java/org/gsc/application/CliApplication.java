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

import org.gsc.config.args.Args;
import org.gsc.db.BlockStore;
import org.gsc.db.Manager;

public class CliApplication implements Application {

    @Override
    public void setOptions(Args args) {

    }

    @Override
    public void init(Args args) {

    }

    @Override
    public void initServices(Args args) {

    }

    @Override
    public void startup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void startServices() {

    }

    @Override
    public void shutdownServices() {

    }

    @Override
    public BlockStore getBlockStoreS() {
        return null;
    }

    @Override
    public void addService(Service service) {

    }

    @Override
    public Manager getDbManager() {
        return null;
    }

}
