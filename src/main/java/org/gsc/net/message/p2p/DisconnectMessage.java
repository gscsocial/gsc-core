package org.gsc.net.message.p2p;

import com.google.protobuf.InvalidProtocolBufferException;
import org.gsc.net.message.MessageTypes;
import org.gsc.protos.P2p;

public class DisconnectMessage extends P2pMessage {

  private P2p.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte[] rawData) {
    super(rawData);
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
  }

  public DisconnectMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  /**
   * The reason of disconnect.
   */
  public DisconnectMessage(ReasonCode reasonCode) {
    this.disconnectMessage = P2p.DisconnectMessage
        .newBuilder()
        .setReason(reasonCode.getReason())
        .build();
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
    this.data = this.disconnectMessage.toByteArray();
  }

  private void unPack() {
    try {
      this.disconnectMessage = P2p.DisconnectMessage.parseFrom(this.data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
    unpacked = true;
  }

  private void pack() {
    this.data = this.disconnectMessage.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  /**
   * Get reason of disconnect.
   */
  public int getReason() {
    if (!this.unpacked) {
      this.unPack();
    }

    return this.disconnectMessage.getReason();
  }

  /**
   * Print reason of disconnect.
   */
  public String toString() {
    if (!this.unpacked) {
      this.unPack();
    }
    return "[" + this.getType().name() + " reason=" + this.getReason() + "]";
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}
