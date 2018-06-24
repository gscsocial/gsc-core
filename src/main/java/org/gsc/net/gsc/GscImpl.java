package org.gsc.net.gsc;

import io.netty.channel.ChannelHandlerContext;
import org.gsc.core.sync.ChainControllerImpl;
import org.gsc.core.sync.PeerConnection;
import org.gsc.net.message.gsc.AttentionMessage;
import org.gsc.net.message.gsc.BlockMessage;
import org.gsc.net.message.gsc.FetchMessage;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.message.gsc.SyncMessage;
import org.gsc.net.message.gsc.TimeMessage;
import org.gsc.net.message.gsc.TransactionMessage;
import org.gsc.protos.P2p.ReasonCode;
import org.gsc.service.NetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GscImpl extends GscHandler{

  @Autowired
  private ChainControllerImpl controller;

  @Autowired
  private NetService netService;

  @Override
  public void channelRead0(ChannelHandlerContext ctx, GscMessage msg)
      throws InterruptedException {
    super.channelRead0(ctx, msg);

    switch (msg.getType()) {
      case BLOCK:
        netService.handleMessage(this.getPeer(), (BlockMessage) msg);
      case TRANSACTION:
        netService.handleMessage(this, (TransactionMessage) msg);
      case SYNC:
        netService.handleMessage(this, (SyncMessage) msg);
      case TIME:
        netService.handleMessage(this, (TimeMessage) msg);
      case FETCH:
        netService.handleMessage(this, (FetchMessage) msg);
      case ATTENTION:
        netService.handleMessage(this, (AttentionMessage) msg);
    }
  }

  @Override
  public void active() {
  }

  @Override
  public void dropConnect() {
  }

  @Override
  public void sendMessage(GscMessage msg) {
  }

  @Override
  public void disconnect(ReasonCode reason) {

  }

  @Override
  public PeerConnection getPeer() {
    return peer;
  }

}
