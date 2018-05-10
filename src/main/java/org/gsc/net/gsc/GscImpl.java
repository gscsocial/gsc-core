package org.gsc.net.gsc;

import org.gsc.core.sync.ChainControllerImpl;
import org.gsc.net.message.p2p.ReasonCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GscImpl extends GscHandler{

  @Autowired
  private ChainControllerImpl controller;

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
