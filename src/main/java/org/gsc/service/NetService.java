package org.gsc.service;

import lombok.extern.slf4j.Slf4j;
import org.gsc.config.Args;
import org.gsc.net.gsc.Gsc;
import org.gsc.net.message.gsc.AttentionMessage;
import org.gsc.net.message.gsc.BlockMessage;
import org.gsc.net.message.gsc.FetchMessage;
import org.gsc.net.message.gsc.GscMessage;
import org.gsc.net.message.gsc.SyncMessage;
import org.gsc.net.message.gsc.TimeMessage;
import org.gsc.net.message.gsc.TransactionMessage;
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

  public void handleMessage(Gsc gsc, TransactionMessage msg) {
    logger.info("get tx message");
  }

  public void handleMessage(Gsc gsc, FetchMessage msg) {
    logger.info("get fetch message");
  }

  public void handleMessage(Gsc gsc, SyncMessage msg) {
    logger.info("get sync message");
  }

  public void handleMessage(Gsc gsc, TimeMessage msg) {
    logger.info("get time message");
  }

  public void handleMessage(Gsc gsc, AttentionMessage msg) {
    logger.info("get attention message");
  }

}
