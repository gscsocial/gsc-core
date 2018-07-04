package org.gsc.common.overlay.message;

import com.google.protobuf.ByteString;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.capsule.BlockCapsule;
import org.gsc.core.config.args.Args;
import org.gsc.core.net.message.MessageTypes;
import org.gsc.protos.Discover.Endpoint;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.HelloMessage.Builder;

public class HelloMessage extends P2pMessage {

  Protocol.HelloMessage helloMessage;

  public HelloMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    this.helloMessage = Protocol.HelloMessage.parseFrom(rawData);
  }

  public HelloMessage(Node from, long timestamp, BlockCapsule.BlockId genesisBlockId,
                      BlockCapsule.BlockId solidBlockId, BlockCapsule.BlockId headBlockId){

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    Builder builder = Protocol.HelloMessage.newBuilder();

    builder.setFrom(fromEndpoint);
    builder.setVersion(Args.getInstance().getNodeP2pVersion());
    builder.setTimestamp(timestamp);
    builder.setGenesisBlockId(genesisBlockId.getByteString());
    builder.setSolidBlockId(solidBlockId.getByteString());
    builder.setHeadBlockId(headBlockId.getByteString());

    this.helloMessage = builder.build();
    this.type = MessageTypes.P2P_HELLO.asByte();
    this.data = this.helloMessage.toByteArray();
  }

  public int getVersion() {
    return this.helloMessage.getVersion();
  }

  public long getTimestamp(){
    return this.helloMessage.getTimestamp();
  }

  public Node getFrom() {
    Endpoint from = this.helloMessage.getFrom();
    return new Node(from.getNodeId().toByteArray(),
            ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
  }

  public BlockCapsule.BlockId getGenesisBlockId(){
    return new BlockCapsule.BlockId(Sha256Hash.wrap(this.helloMessage.getGenesisBlockId()));
  }

  public BlockCapsule.BlockId getSolidBlockId(){
    return new BlockCapsule.BlockId(Sha256Hash.wrap(this.helloMessage.getSolidBlockId()));
  }

  public BlockCapsule.BlockId getHeadBlockId(){
    return new BlockCapsule.BlockId(Sha256Hash.wrap(this.helloMessage.getHeadBlockId()));
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(helloMessage.toString()).toString();
  }

}