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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "net")
public class MessageHandler extends SimpleChannelInboundHandler<UdpEvent>
        implements Consumer<UdpEvent> {

    private Channel channel;

    private EventHandler eventHandler;

    public MessageHandler(NioDatagramChannel channel, EventHandler eventHandler) {
        this.channel = channel;
        this.eventHandler = eventHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        eventHandler.channelActivated();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, UdpEvent udpEvent) {
        logger.debug("rcv udp msg type {}, len {} from {} ",
                udpEvent.getMessage().getType(),
                udpEvent.getMessage().getSendData().length,
                udpEvent.getAddress());
        eventHandler.handleEvent(udpEvent);
    }



    @Override
    public void accept(UdpEvent udpEvent) {
        logger.debug("send udp msg type {}, len {} to {} ",
                udpEvent.getMessage().getType(),
                udpEvent.getMessage().getSendData().length,
                udpEvent.getAddress());
        InetSocketAddress address = udpEvent.getAddress();
        sendPacket(udpEvent.getMessage().getSendData(), address);
    }

    void sendPacket(byte[] wire, InetSocketAddress address) {
        DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(wire), address);
        channel.write(packet);
        channel.flush();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.info("exception caught, {} {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
