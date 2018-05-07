package org.gsc.net.message.p2p;

import org.gsc.net.discover.Node;
import org.springframework.stereotype.Component;

@Component
public class StaticMessages {

  public final static PingMessage PING_MESSAGE = new PingMessage();
  public final static PongMessage PONG_MESSAGE = new PongMessage();

  public HelloMessage createHelloMessage(Node node, long timestamp) {
    return new HelloMessage(node, timestamp);
  }
}
