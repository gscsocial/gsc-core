package org.gsc.net.gsc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.sync.PeerConnection;
import org.gsc.core.sync.SyncManager;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.server.Channel;
import org.gsc.net.server.MessageQueue;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Scope("prototype")
public abstract class GscHandler extends SimpleChannelInboundHandler<GscMessage> implements Gsc {

  protected PeerConnection peer;

  private MessageQueue msgQueue = null;

  public SyncManager syncManager;

  public void setSyncManager(SyncManager peerDel) {
    this.syncManager = syncManager;
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, GscMessage msg)
      throws InterruptedException {
    msgQueue.receivedMessage(msg);
    //handle message
    syncManager.onMessage(peer, msg);
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