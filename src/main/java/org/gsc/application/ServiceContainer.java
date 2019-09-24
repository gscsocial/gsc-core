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

import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import org.gsc.config.args.Args;

@Slf4j(topic = "app")
public class ServiceContainer {

    private ArrayList<Service> services;

    public ServiceContainer() {
        this.services = new ArrayList<>();
    }

    public void add(Service service) {
        this.services.add(service);
    }


    public void init() {
        for (Service service : this.services) {
            logger.debug("Initing " + service.getClass().getSimpleName());
            service.init();
        }
    }

    public void init(Args args) {
        for (Service service : this.services) {
            logger.debug("Initing " + service.getClass().getSimpleName());
            service.init(args);
        }
    }

    public void start() {
        logger.debug("Starting services");
        for (Service service : this.services) {
            logger.debug("Starting " + service.getClass().getSimpleName());
            service.start();
        }
    }

    public void stop() {
        for (Service service : this.services) {
            logger.debug("Stopping " + service.getClass().getSimpleName());
            service.stop();
        }
    }
}
