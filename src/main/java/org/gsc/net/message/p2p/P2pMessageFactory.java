package org.gsc.net.message.p2p;

import org.gsc.net.message.Message;
import org.gsc.net.message.MessageFactory;
import org.gsc.net.message.MessageTypes;

public class P2pMessageFactory implements MessageFactory {

  @Override
  public Message create(byte code, byte[] encoded) {

    MessageTypes receivedCommand = MessageTypes.fromByte(code);
    switch (receivedCommand) {
      case P2P_HELLO:
        return new HelloMessage(encoded);
      case P2P_DISCONNECT:
        return new DisconnectMessage(encoded);
      case P2P_PING:
        return StaticMessages.PING_MESSAGE;
      case P2P_PONG:
        return StaticMessages.PONG_MESSAGE;
         default:
        throw new IllegalArgumentException("No such message");
    }
  }
}

