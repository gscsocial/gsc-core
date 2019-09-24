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

package org.gsc.net.peer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.config.Parameter.NetConstants;
import org.gsc.net.GSCNetDelegate;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class PeerStatusCheck {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    private ScheduledExecutorService peerStatusCheckExecutor = Executors
            .newSingleThreadScheduledExecutor();

    private int blockUpdateTimeout = 30_000;

    public void init() {
        peerStatusCheckExecutor.scheduleWithFixedDelay(() -> {
            try {
                statusCheck();
            } catch (Throwable t) {
                logger.error("Unhandled exception", t);
            }
        }, 5, 2, TimeUnit.SECONDS);
    }

    public void close() {
        peerStatusCheckExecutor.shutdown();
    }

    public void statusCheck() {

        long now = System.currentTimeMillis();

        gscNetDelegate.getActivePeer().forEach(peer -> {

            boolean isDisconnected = false;

            if (peer.isNeedSyncFromPeer()
                    && peer.getBlockBothHaveUpdateTime() < now - blockUpdateTimeout) {
                logger.warn("Peer {} not sync for a long time.", peer.getInetAddress());
                isDisconnected = true;
            }

            if (!isDisconnected) {
                isDisconnected = peer.getAdvInvRequest().values().stream()
                        .anyMatch(time -> time < now - NetConstants.ADV_TIME_OUT);
            }

            if (!isDisconnected) {
                isDisconnected = peer.getSyncBlockRequested().values().stream()
                        .anyMatch(time -> time < now - NetConstants.SYNC_TIME_OUT);
            }

            if (isDisconnected) {
                peer.disconnect(ReasonCode.TIME_OUT);
            }
        });
    }

}
