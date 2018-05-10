package org.gsc.service;

import lombok.extern.slf4j.Slf4j;
import org.gsc.config.Args;
import org.gsc.net.gsc.Gsc;
import org.gsc.net.message.gsc.BlockMessage;
import org.gsc.net.message.gsc.GscMessage;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NetService implements Service{

  @Override
  public void init() {

  }

  @Override
  public void init(Args args) {

  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }

  public void broadcast(GscMessage msg) {

  }

  public void handleMessage(Gsc gsc, GscMessage msg) {

  }

  public void handleMessage(Gsc gsc, BlockMessage msg) {
    logger.info("get block message");
  }
}
