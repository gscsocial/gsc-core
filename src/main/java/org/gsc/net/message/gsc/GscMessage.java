package org.gsc.net.message.gsc;

import org.gsc.net.message.Message;
import org.gsc.net.message.MessageTypes;

public class GscMessage extends Message {

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return null;
  }
}
