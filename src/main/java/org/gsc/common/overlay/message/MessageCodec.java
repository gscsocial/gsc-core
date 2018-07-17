package org.gsc.common.overlay.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.gsc.common.overlay.server.Channel;
import org.gsc.core.exception.P2pException;
import org.gsc.net.message.MessageTypes;
import org.gsc.net.message.GscMessageFactory;

@Component
@Scope("prototype")
public class MessageCodec extends ByteToMessageDecoder {

  private Channel channel;
  private P2pMessageFactory p2pMessageFactory = new P2pMessageFactory();
  private GscMessageFactory gscMessageFactory = new GscMessageFactory();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
    byte[] encoded = new byte[buffer.readableBytes()];
    buffer.readBytes(encoded);
    try {
      Message msg = createMessage(encoded);
      channel.getNodeStatistics().gscInMessage.add();
      out.add(msg);
    } catch (Exception e) {
      channel.processException(e);
    }
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  private Message createMessage(byte[] encoded) throws Exception{
    byte type = encoded[0];
    if (MessageTypes.inP2pRange(type)) {
      return p2pMessageFactory.create(encoded);
    }
    if (MessageTypes.inGscRange(type)) {
      return gscMessageFactory.create(encoded);
    }
    throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + encoded[0]);
  }

}