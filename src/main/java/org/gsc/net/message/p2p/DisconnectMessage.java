package org.gsc.net.message.p2p;

import org.gsc.net.message.MessageTypes;
import org.gsc.protos.P2p;
import org.gsc.protos.P2p.ReasonCode;


public class DisconnectMessage extends P2pMessage {

  private P2p.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte type, byte[] rawData) throws Exception{
    super(type, rawData);
    this.disconnectMessage = P2p.DisconnectMessage.parseFrom(this.data);
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  public DisconnectMessage(ReasonCode reasonCode) {
    this.disconnectMessage = P2p.DisconnectMessage
        .newBuilder()
        .setReason(reasonCode)
        .build();
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
    this.data = this.disconnectMessage.toByteArray();
  }

  public int getReason() {
    return this.disconnectMessage.getReason().getNumber();
  }

  public ReasonCode getReasonCode() {
    return disconnectMessage.getReason();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("reason: ").append(this.disconnectMessage.getReason()).toString();
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