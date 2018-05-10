package org.gsc.net.gsc;

import io.netty.channel.ChannelHandlerContext;
import org.gsc.core.sync.ChainControllerImpl;
import org.gsc.net.message.gsc.BlockMessage;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.message.p2p.ReasonCode;
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
        netService.handleMessage(this, (BlockMessage) msg);
    }
  }

  @Override
  public void active() {

  }

  @Override
  public void dropConnect() {

  }

  @Override
  public void disconnect(ReasonCode reason) {

  }

}
