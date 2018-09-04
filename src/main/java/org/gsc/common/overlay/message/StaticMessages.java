package org.gsc.common.overlay.message;

import org.springframework.stereotype.Component;

@Component
public class StaticMessages {
  public final static PingMessage PING_MESSAGE = new PingMessage();
  public final static PongMessage PONG_MESSAGE = new PongMessage();
}
