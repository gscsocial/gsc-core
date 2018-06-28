package org.gsc.net.message.p2p;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Args;
import org.gsc.core.chain.BlockId;
import org.gsc.net.discover.Node;
import org.gsc.net.message.MessageTypes;
import org.gsc.protos.Discover.Endpoint;
import org.gsc.protos.P2p;
import org.springframework.beans.factory.annotation.Autowired;

public class HelloMessage extends P2pMessage {

  private P2p.HelloMessage helloMessage;

  @Autowired
  private Args args;

  public HelloMessage(byte[] rawData) {
    super(rawData);
    this.type = MessageTypes.P2P_HELLO.asByte();
    unPack();
  }

  public HelloMessage(byte type, byte[] rawData) {
    super(type, rawData);
    try {
      this.helloMessage = P2p.HelloMessage.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
    unPack();
  }

  /**
   * Create hello message.
   */
  public HelloMessage(Node from, long timestamp) {

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    P2p.HelloMessage.Builder builder = P2p.HelloMessage.newBuilder();

    builder.setFrom(fromEndpoint);
    builder.setVersion(args.getNodeP2pVersion());
    builder.setTimestamp(timestamp);

    this.helloMessage = builder.build();
    this.type = MessageTypes.P2P_HELLO.asByte();
    this.data = this.helloMessage.toByteArray();
  }

  public void unPack() {
    try {
      this.helloMessage = P2p.HelloMessage.parseFrom(this.data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  @Override
  public byte[] getData() {
    return this.data;
  }

  /**
   * Get the version of p2p protocol.
   */
  public int getVersion() {
    return this.helloMessage.getVersion();
  }

  public long getTimestamp(){
    return this.helloMessage.getTimestamp();
  }

  /**
   * Get listen port.
   */
  public int getListenPort() {
    return this.helloMessage.getFrom().getPort();
  }

  /**
   * Get peer ID.
   */
  public String getPeerId() {
    return ByteArray.toHexString(this.helloMessage.getFrom().getNodeId().toByteArray());
  }

  /**
   * Set version of p2p protocol.
   */
  public void setVersion(byte version) {
    P2p.HelloMessage.Builder builder = this.helloMessage.toBuilder();
    builder.setVersion(version);
    this.helloMessage = builder.build();
  }

  //TODO
  public BlockId getHeadBlockId() {
    return null;
  }

  /**
   * Get string.
   */
  public String toString() {
    return helloMessage.toString();
//    if (!this.unpacked) {
//      this.unPack();
//    }
//    return "[" + this.getCommand().name() + " p2pVersion="
//        + this.getVersion() + " clientId=" + this.getClientId()
//        + " peerPort=" + this.getListenPort() + " peerId="
//        + this.getPeerId() + "]";
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

  public Node getFrom() {
    Endpoint from = this.helloMessage.getFrom();
    return new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
  }
}