package org.gsc.common.overlay.message;

import com.google.protobuf.ByteString;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.config.args.Args;
import org.gsc.net.message.MessageTypes;
import org.gsc.protos.Discover.Endpoint;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.HelloMessage.Builder;

public class HelloMessage extends P2pMessage {

  Protocol.HelloMessage helloMessage;

  public HelloMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    this.helloMessage = Protocol.HelloMessage.parseFrom(rawData);
  }

  public HelloMessage(Node from, long timestamp, BlockWrapper.BlockId genesisBlockId,
                      BlockWrapper.BlockId solidBlockId, BlockWrapper.BlockId headBlockId){

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    Protocol.HelloMessage.BlockId gBlockId = Protocol.HelloMessage.BlockId.newBuilder()
            .setHash(genesisBlockId.getByteString())
            .setNumber(genesisBlockId.getNum())
            .build();

    Protocol.HelloMessage.BlockId sBlockId = Protocol.HelloMessage.BlockId.newBuilder()
            .setHash(solidBlockId.getByteString())
            .setNumber(solidBlockId.getNum())
            .build();

    Protocol.HelloMessage.BlockId hBlockId = Protocol.HelloMessage.BlockId.newBuilder()
            .setHash(headBlockId.getByteString())
            .setNumber(headBlockId.getNum())
            .build();

    Builder builder = Protocol.HelloMessage.newBuilder();

    builder.setFrom(fromEndpoint);
    builder.setVersion(Args.getInstance().getNodeP2pVersion());
    builder.setTimestamp(timestamp);
    builder.setGenesisBlockId(gBlockId);
    builder.setSolidBlockId(sBlockId);
    builder.setHeadBlockId(hBlockId);

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

  public BlockWrapper.BlockId getGenesisBlockId(){
    return new BlockWrapper.BlockId(this.helloMessage.getGenesisBlockId().getHash(),
            this.helloMessage.getGenesisBlockId().getNumber());
  }

  public BlockWrapper.BlockId getSolidBlockId(){
    return new BlockWrapper.BlockId(this.helloMessage.getSolidBlockId().getHash(),
            this.helloMessage.getSolidBlockId().getNumber());
  }

  public BlockWrapper.BlockId getHeadBlockId(){
    return new BlockWrapper.BlockId(this.helloMessage.getHeadBlockId().getHash(),
            this.helloMessage.getHeadBlockId().getNumber());
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