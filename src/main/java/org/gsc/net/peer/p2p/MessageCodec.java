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

package org.gsc.net.peer.p2p;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.server.Channel;
import org.gsc.core.exception.P2pException;
import org.gsc.net.peer.message.MessageTypes;
import org.gsc.net.peer.message.GSCMessageFactory;

@Component
@Scope("prototype")
public class MessageCodec extends ByteToMessageDecoder {

    private Channel channel;
    private P2pMessageFactory p2pMessageFactory = new P2pMessageFactory();
    private GSCMessageFactory GSCMessageFactory = new GSCMessageFactory();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
            throws Exception {
        int length = buffer.readableBytes();
        byte[] encoded = new byte[length];
        buffer.readBytes(encoded);
        try {
            Message msg = createMessage(encoded);
            channel.getNodeStatistics().tcpFlow.add(length);
            out.add(msg);
        } catch (Exception e) {
            channel.processException(e);
        }
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    private Message createMessage(byte[] encoded) throws Exception {
        byte type = encoded[0];
        if (MessageTypes.inP2pRange(type)) {
            return p2pMessageFactory.create(encoded);
        }
        if (MessageTypes.inTRXRange(type)) {
            return GSCMessageFactory.create(encoded);
        }
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + encoded[0]);
    }

}