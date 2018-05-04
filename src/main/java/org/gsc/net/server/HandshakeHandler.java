/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.gsc.net.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.net.InetSocketAddress;
import java.util.List;
import org.gsc.net.discover.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class HandshakeHandler extends ByteToMessageDecoder {

  private static final Logger logger = LoggerFactory.getLogger("HandshakeHandler");

  private byte[] remoteId;

  private Channel channel;

  private final NodeManager nodeManager;

  private final ChannelManager channelManager;

  @Autowired
  public HandshakeHandler(final NodeManager nodeManager, final ChannelManager channelManager) {
    this.nodeManager = nodeManager;
    this.channelManager = channelManager;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("channel active, {}", ctx.channel().remoteAddress());
    channel.setInetSocketAddress((InetSocketAddress) ctx.channel().remoteAddress());
    if (remoteId.length == 64) {
      channel.initWithNode(remoteId, ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
      //TODO: say hello
      channel.getNodeStatistics().p2pOutHello.add();
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    channelManager.processException(ctx, cause);
  }

  public void setChannel(Channel channel, String remoteId) {
    this.channel = channel;
    this.remoteId = Hex.decode(remoteId);
  }


}
