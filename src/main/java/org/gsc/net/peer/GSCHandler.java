package org.gsc.net.peer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gsc.net.message.GSCMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.common.overlay.server.Channel;
import org.gsc.common.overlay.server.MessageQueue;

@Component
@Scope("prototype")
public class GSCHandler extends SimpleChannelInboundHandler<GSCMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue = null;

  public PeerConnectionDelegate peerDel;

  public void setPeerDel(PeerConnectionDelegate peerDel) {
    this.peerDel = peerDel;
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, GSCMessage msg)
          throws InterruptedException {
    msgQueue.receivedMessage(msg);
    //handle message
    peerDel.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}