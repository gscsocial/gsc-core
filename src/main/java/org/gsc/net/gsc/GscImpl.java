package org.gsc.net.gsc;

import io.netty.channel.ChannelHandlerContext;
import org.gsc.core.sync.ChainControllerImpl;
import org.gsc.core.sync.PeerConnection;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.message.gsc.InventoryMessage;
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
    PeerConnection peer = this.getPeer();


    switch (msg.getType()) {
      case INVENTORY:
        netService.handleMessage(peer, (InventoryMessage) msg);
//      case BLOCK:
//        netService.handleMessage(peer, (TransactionMessage) msg);
//      case TRANSACTION:
//        netService.handleMessage(peer, (TransactionMessage) msg);
//      case SYNC:
//        netService.handleMessage(peer, (SyncMessage) msg);
//      case TIME:
//        netService.handleMessage(peer, (TimeMessage) msg);
//      case FETCH:
//        netService.handleMessage(peer, (FetchMessage) msg);
//      case ATTENTION:
//        netService.handleMessage(peer, (AttentionMessage) msg);
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
