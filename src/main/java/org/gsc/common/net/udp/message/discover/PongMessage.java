package org.gsc.common.net.udp.message.discover;

import static org.gsc.common.net.udp.message.UdpMessageTypeEnum.DISCOVER_PONG;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.net.udp.message.Message;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.args.Args;
import org.gsc.protos.Discover;
import org.gsc.protos.Discover.Endpoint;

@Slf4j
public class PongMessage extends Message {

  private Discover.PongMessage pongMessage;

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
            .setEcho(Args.getInstance().getNodeP2pVersion())
            .setTimestamp(System.currentTimeMillis())
            .build();
    this.data = this.pongMessage.toByteArray();
  }

  public int getVersion() {
    return this.pongMessage.getEcho();
  }

  @Override
  public Node getFrom() {
    return Message.getNode(pongMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[pongMessage: " + pongMessage;
  }
}
