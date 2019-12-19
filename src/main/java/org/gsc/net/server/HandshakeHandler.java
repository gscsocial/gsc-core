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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.net.node.NodeManager;
import org.gsc.net.peer.p2p.DisconnectMessage;
import org.gsc.net.peer.p2p.HelloMessage;
import org.gsc.net.peer.p2p.P2pMessage;
import org.gsc.net.peer.p2p.P2pMessageFactory;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.net.peer.PeerConnection;
import org.gsc.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class HandshakeHandler extends ByteToMessageDecoder {

    private byte[] remoteId;

    protected Channel channel;

    @Autowired
    protected NodeManager nodeManager;

    @Autowired
    protected ChannelManager channelManager;

    @Autowired
    protected Manager manager;

    private P2pMessageFactory messageFactory = new P2pMessageFactory();

    @Autowired
    private SyncPool syncPool;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channel active, {}", ctx.channel().remoteAddress());
        channel.setChannelHandlerContext(ctx);
        if (remoteId.length == 64) {
            channel.initNode(remoteId, ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
            sendHelloMsg(ctx, System.currentTimeMillis());
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
            throws Exception {
        byte[] encoded = new byte[buffer.readableBytes()];
        buffer.readBytes(encoded);
        P2pMessage msg = messageFactory.create(encoded);

        logger.info("Handshake Receive from {}, {}", ctx.channel().remoteAddress(), msg);

        switch (msg.getType()) {
            case P2P_HELLO:
                handleHelloMsg(ctx, (HelloMessage) msg);
                break;
            case P2P_DISCONNECT:
                if (channel.getNodeStatistics() != null) {
                    channel.getNodeStatistics()
                            .nodeDisconnectedRemote(((DisconnectMessage) msg).getReasonCode());
                }
                channel.close();
                break;
            default:
                channel.close();
                break;
        }
    }

    public void setChannel(Channel channel, String remoteId) {
        this.channel = channel;
        this.remoteId = Hex.decode(remoteId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        channel.processException(cause);
    }

    protected void sendHelloMsg(ChannelHandlerContext ctx, long time) {
        HelloMessage message = new HelloMessage(nodeManager.getPublicHomeNode(), time,
                manager.getGenesisBlockId(), manager.getConfirmedBlockId(), manager.getHeadBlockId());
        ctx.writeAndFlush(message.getSendData());
        channel.getNodeStatistics().messageStatistics.addTcpOutMessage(message);
        logger.info("Handshake Send to {}, {} ", ctx.channel().remoteAddress(), message);
    }

    private void handleHelloMsg(ChannelHandlerContext ctx, HelloMessage msg) {
        channel.initNode(msg.getFrom().getId(), msg.getFrom().getPort());

        if (remoteId.length != 64) {
            InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
            if (channelManager.getTrustNodes().getIfPresent(address) == null && !syncPool.isCanConnect()) {
                channel.disconnect(ReasonCode.TOO_MANY_PEERS);
                return;
            }
        }

        if (msg.getVersion() != Args.getInstance().getNodeP2pVersion()) {
            logger.info("Peer {} different p2p version, peer->{}, me->{}",
                    ctx.channel().remoteAddress(), msg.getVersion(), Args.getInstance().getNodeP2pVersion());
            channel.disconnect(ReasonCode.INCOMPATIBLE_VERSION);
            return;
        }

        if (!Arrays
                .equals(manager.getGenesisBlockId().getBytes(), msg.getGenesisBlockId().getBytes())) {
            logger
                    .info("Peer {} different genesis block, peer->{}, me->{}", ctx.channel().remoteAddress(),
                            msg.getGenesisBlockId().getString(), manager.getGenesisBlockId().getString());
            channel.disconnect(ReasonCode.INCOMPATIBLE_CHAIN);
            return;
        }

        if (manager.getConfirmedBlockId().getNum() >= msg.getSolidBlockId().getNum() && !manager
                .containBlockInMainChain(msg.getSolidBlockId())) {
            logger.info("Peer {} different solid block, peer->{}, me->{}", ctx.channel().remoteAddress(),
                    msg.getSolidBlockId().getString(), manager.getConfirmedBlockId().getString());
            channel.disconnect(ReasonCode.FORKED);
            return;
        }

        ((PeerConnection) channel).setHelloMessage(msg);
        channel.getNodeStatistics().messageStatistics.addTcpInMessage(msg);

        channel.publicHandshakeFinished(ctx, msg);
        if (!channelManager.processPeer(channel)) {
            return;
        }

        if (remoteId.length != 64) {
            sendHelloMsg(ctx, msg.getTimestamp());
        }

        syncPool.onConnect(channel);
    }
}
