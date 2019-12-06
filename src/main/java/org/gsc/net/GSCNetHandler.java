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

package org.gsc.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.server.Channel;
import org.gsc.net.server.MessageQueue;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.PeerConnection;

@Component
@Scope("prototype")
public class GSCNetHandler extends SimpleChannelInboundHandler<GSCMessage> {

    protected PeerConnection peer;

    private MessageQueue msgQueue;

    @Autowired
    private GSCNetService gscNetService;

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, GSCMessage msg) throws Exception {
        msgQueue.receivedMessage(msg);
        gscNetService.onMessage(peer, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        peer.processException(cause);
    }

    public void setChannel(Channel channel) {
        this.peer = (PeerConnection) channel;
    }

    public void setMsgQueue(MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

}