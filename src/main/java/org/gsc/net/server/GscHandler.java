package org.gsc.net.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gsc.net.message.gsc.GscMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Process the messages between peers with 'eth' capability on the network<br>
 * Contains common logic to all supported versions
 * delegating version specific stuff to its descendants
 *
 */
@Component
@Scope("prototype")
public class GscHandler extends SimpleChannelInboundHandler<GscMessage> {

  private final static Logger logger = LoggerFactory.getLogger("TronHandler");

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

  protected void sendMessage(GscMessage message) {
    msgQueue.sendMessage(message);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }
}
