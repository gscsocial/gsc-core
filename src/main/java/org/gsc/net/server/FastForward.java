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

package org.gsc.net.server;

import com.google.protobuf.ByteString;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.gsc.net.backup.BackupManager;
import org.gsc.net.node.Node;
import org.gsc.config.args.Args;
import org.gsc.db.WitnessScheduleStore;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class FastForward {

    @Autowired
    private ApplicationContext ctx;

    private BackupManager backupManager;

    private ChannelManager channelManager;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private Args args = Args.getInstance();
    private List<Node> fastForwardNodes = args.getFastForwardNodes();
    private ByteString witnessAddress = ByteString
            .copyFrom(args.getLocalWitnesses().getWitnessAccountAddress());
    private int keySize = args.getLocalWitnesses().getPrivateKeys().size();

    public void init() {

        logger.info("Fast forward config, isWitness: {}, keySize: {}, fastForwardNodes: {}",
                args.isWitness(), keySize, fastForwardNodes.size());

        if (!args.isWitness() || keySize == 0 || fastForwardNodes.size() == 0) {
            return;
        }

        channelManager = ctx.getBean(ChannelManager.class);
        backupManager = ctx.getBean(BackupManager.class);
        WitnessScheduleStore witnessScheduleStore = ctx.getBean(WitnessScheduleStore.class);

        executorService.scheduleWithFixedDelay(() -> {
            try {
                if (witnessScheduleStore.getActiveWitnesses().contains(witnessAddress) &&
                        backupManager.getStatus().equals(BackupManager.BackupStatusEnum.MASTER)) {
                    connect();
                } else {
                    disconnect();
                }
            } catch (Throwable t) {
                logger.info("Execute failed.", t);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void connect() {
        fastForwardNodes.forEach(node -> {
            InetAddress address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
            channelManager.getActiveNodes().put(address, node);
        });
    }

    private void disconnect() {
        fastForwardNodes.forEach(node -> {
            InetAddress address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
            channelManager.getActiveNodes().remove(address);
            channelManager.getActivePeers().forEach(channel -> {
                if (channel.getInetAddress().equals(address)) {
                    channel.disconnect(ReasonCode.RESET);
                }
            });
        });
    }
}
