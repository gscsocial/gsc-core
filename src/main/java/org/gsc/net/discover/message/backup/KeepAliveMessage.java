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

package org.gsc.net.discover.message.backup;

import static org.gsc.net.discover.message.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;

import org.gsc.net.discover.message.Message;
import org.gsc.net.node.Node;
import org.gsc.protos.Discover;

public class KeepAliveMessage extends Message {

    private Discover.BackupMessage backupMessage;

    public KeepAliveMessage(byte[] data) throws Exception {
        super(BACKUP_KEEP_ALIVE, data);
        backupMessage = Discover.BackupMessage.parseFrom(data);
    }

    public KeepAliveMessage(boolean flag, int priority) {
        super(BACKUP_KEEP_ALIVE, null);
        backupMessage = Discover.BackupMessage.newBuilder().setFlag(flag).setPriority(priority).build();
        data = backupMessage.toByteArray();
    }

    public boolean getFlag() {
        return backupMessage.getFlag();
    }

    public int getPriority() {
        return backupMessage.getPriority();
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public Node getFrom() {
        return null;
    }
}
