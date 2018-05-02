package org.gsc.net.message.discover;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Args;
import org.gsc.net.discover.table.Node;
import org.gsc.protos.Discover;
import org.gsc.protos.Discover.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
public class PingMessage extends Message {

  @Autowired
  private Args config;

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] data) {
    super(Message.PING, data);
    try {
      this.pingMessage = Discover.PingMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public PingMessage(Node from, Node to) {
    super(Message.PING, null);
    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();
    Endpoint toEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(to.getId()))
        .setPort(to.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(to.getHost())))
        .build();
    this.pingMessage = Discover.PingMessage.newBuilder().setVersion(config.getNodeP2pVersion())
        .setFrom(fromEndpoint)
        .setTo(toEndpoint)
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.pingMessage.toByteArray();
  }

  public Node getFrom() {
    Endpoint from = this.pingMessage.getFrom();
    Node node = new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
    return node;
  }

  public Node getTo() {
    Endpoint to = this.pingMessage.getTo();
    Node node = new Node(to.getNodeId().toByteArray(),
        ByteArray.toStr(to.getAddress().toByteArray()), to.getPort());
    return node;
  }

  @Override
  public byte[] getNodeId() {
    return this.pingMessage.getFrom().getNodeId().toByteArray();
  }

  @Override
  public String toString() {
    return "[pingMessage: " + pingMessage;
  }

}
