package org.gsc.net.message.discover;

import static org.gsc.net.message.discover.UdpMessageTypeEnum.DISCOVER_PONG;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Args;
import org.gsc.net.discover.Node;
import org.gsc.protos.Discover;
import org.gsc.protos.Discover.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

  @Autowired
  private Args config = Args.getInstance();

  public PongMessage(byte[] data) throws Exception{
    super(DISCOVER_PONG, data);
    this.pongMessage = Discover.PongMessage.parseFrom(data);
  }

  public PongMessage(Node from) {
    super(DISCOVER_PONG, null);
    Endpoint toEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();
    this.pongMessage = Discover.PongMessage.newBuilder()
        .setFrom(toEndpoint)
        .setEcho(config.getNodeP2pVersion())
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.pongMessage.toByteArray();
  }

  public Node getFrom() {
    Endpoint from = this.pongMessage.getFrom();
    Node node = new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
    return node;
  }

  public int getVersion() {
    return this.pongMessage.getEcho();
  }

  @Override
  public byte[] getNodeId() {
    return this.pongMessage.getFrom().getNodeId().toByteArray();
  }

  @Override
  public String toString() {
    return "[pongMessage: " + pongMessage;
  }
}
