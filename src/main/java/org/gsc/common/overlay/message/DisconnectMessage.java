package org.gsc.common.overlay.message;

import org.gsc.net.message.MessageTypes;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.ReasonCode;

public class DisconnectMessage extends P2pMessage {

  private Protocol.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte type, byte[] rawData) throws Exception{
    super(type, rawData);
    this.disconnectMessage = Protocol.DisconnectMessage.parseFrom(this.data);
  }

  public DisconnectMessage(ReasonCode reasonCode) {
    this.disconnectMessage = Protocol.DisconnectMessage
        .newBuilder()
        .setReason(reasonCode)
        .build();
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
    this.data = this.disconnectMessage.toByteArray();
  }

  public ReasonCode getReasonCode() {
    return disconnectMessage.getReason();
  }
  
  public int getReason() {
    return this.disconnectMessage.getReason().getNumber();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("reason: ").append(this.disconnectMessage.getReason()).toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
