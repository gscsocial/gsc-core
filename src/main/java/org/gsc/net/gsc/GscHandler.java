package org.gsc.net.gsc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.server.Channel;
import org.gsc.net.server.MessageQueue;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Scope("prototype")
public abstract class GscHandler extends SimpleChannelInboundHandler<GscMessage> implements Gsc {

  protected Channel channel;

  private MessageQueue msgQueue = null;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, GscMessage msg) throws InterruptedException {
    msgQueue.receivedMessage(msg);
    //handle message
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.error("exception caught, {}", ctx.channel().remoteAddress(), cause);
    ctx.close();
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    logger.info("handler Removed. {}", ctx.channel().remoteAddress());
  }

//  public void activate() {
//    peerDel.onConnectPeer(peer);
//  }

  @Override
  public void sendMessage(GscMessage message) {
    msgQueue.sendMessage(message);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }
}
