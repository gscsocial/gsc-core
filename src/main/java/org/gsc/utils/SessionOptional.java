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

package org.gsc.utils;

import java.util.Optional;

import org.gsc.db.db2.core.ISession;

public final class SessionOptional {

    private static final SessionOptional INSTANCE = OptionalEnum.INSTANCE.getInstance();

    private Optional<ISession> value;

    private SessionOptional() {
        this.value = Optional.empty();
    }

    public synchronized boolean valid() {
        return value.isPresent();
    }

    public synchronized SessionOptional setValue(ISession value) {
        if (!this.value.isPresent()) {
            this.value = Optional.of(value);
        }
        return this;
    }

    public synchronized void reset() {
        value.ifPresent(ISession::destroy);
        value = Optional.empty();
    }

    public static SessionOptional instance() {
        return INSTANCE;
    }

    private enum OptionalEnum {
        INSTANCE;

        private SessionOptional instance;

        OptionalEnum() {
            instance = new SessionOptional();
        }

        private SessionOptional getInstance() {
            return instance;
        }
    }

}
