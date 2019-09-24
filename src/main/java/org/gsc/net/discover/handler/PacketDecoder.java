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


package org.gsc.net.discover.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.gsc.net.discover.message.Message;

@Slf4j(topic = "net")
public class PacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final int MAXSIZE = 2048;

    @Override
    public void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out)
            throws Exception {
        ByteBuf buf = packet.content();
        int length = buf.readableBytes();
        if (length <= 1 || length >= MAXSIZE) {
            logger
                    .error("UDP rcv bad packet, from {} length = {}", ctx.channel().remoteAddress(), length);
            return;
        }
        byte[] encoded = new byte[length];
        buf.readBytes(encoded);
        try {
            UdpEvent event = new UdpEvent(Message.parse(encoded), packet.sender());
            out.add(event);
        } catch (Exception e) {
            logger.error("Parse msg failed, type {}, len {}, address {}", encoded[0], encoded.length,
                    packet.sender());
        }
    }
}
