package org.gsc.net.message.p2p;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.exception.P2pException;
import org.gsc.net.message.MessageFactory;
import org.gsc.net.message.MessageTypes;


public class P2pMessageFactory implements MessageFactory {

  @Override
  public P2pMessage create(byte[] data) throws Exception{
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (Exception e) {
      if (e instanceof P2pException){
        throw e;
      }else {
        throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED, "type=" + data[0] + ", len=" + data.length);
      }
    }
  }

  private P2pMessage create(byte type, byte[] rawData) throws  Exception{
    MessageTypes messageType = MessageTypes.fromByte(type);
    if (messageType == null){
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + type + ", len=" + rawData.length);
    }
    switch (messageType) {
      case P2P_HELLO:
        return new HelloMessage(type, rawData);
      case P2P_DISCONNECT:
        return new DisconnectMessage(type, rawData);
      case P2P_PING:
        return new PingMessage(type, rawData);
      case P2P_PONG:
        return new PongMessage(type, rawData);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, messageType.toString()  + ", len=" + rawData.length);
    }
  }
}
