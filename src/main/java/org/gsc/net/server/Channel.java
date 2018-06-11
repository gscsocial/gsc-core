
package org.gsc.net.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.scalecube.transport.MessageCodec;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import org.gsc.core.sync.SyncManager;
import org.gsc.core.wrapper.ByteArrayWrapper;
import org.gsc.net.discover.Node;
import org.gsc.net.discover.NodeManager;
import org.gsc.net.discover.NodeStatistics;
import org.gsc.net.gsc.GscHandler;
import org.gsc.net.message.p2p.DisconnectMessage;
import org.gsc.net.message.p2p.HelloMessage;
import org.gsc.net.message.p2p.StaticMessages;
import org.gsc.protos.P2p.ReasonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class Channel {

    private final static Logger logger = LoggerFactory.getLogger("Channel");

    @Autowired
    protected MessageQueue msgQueue;

    @Autowired
    private MessageCodec messageCodec;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private StaticMessages staticMessages;

    @Autowired
    private WireTrafficStats stats;

    @Autowired
    private HandshakeHandler handshakeHandler;

    @Autowired
    private P2pHandler p2pHandler;

    @Autowired
    private GscHandler gscHandler;

    private ChannelManager channelManager;

    private ChannelHandlerContext ctx;

    private InetSocketAddress inetSocketAddress;

    private Node node;

    private long startTime;

    @Autowired
    private SyncManager syncManager;

    private GscState gscState = GscState.INIT;

    protected NodeStatistics nodeStatistics;

    private boolean isActive;

    private volatile boolean isDisconnect;

    private String remoteId;

    private PeerStatistics peerStats = new PeerStatistics();

    public void init(ChannelPipeline pipeline, String remoteId, boolean discoveryMode,
        ChannelManager channelManager, SyncManager syncManager) {

        this.channelManager = channelManager;

        this.remoteId = remoteId;

        isActive = remoteId != null && !remoteId.isEmpty();

        //TODO: use config here
        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60, TimeUnit.SECONDS));
        pipeline.addLast(stats.tcp);
        pipeline.addLast("protoPender", new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast("lengthDecode", new GscProtobufVarint32FrameDecoder(this));

        //handshake first
        pipeline.addLast("handshakeHandler", handshakeHandler);

        this.syncManager = syncManager;

        messageCodec.setChannel(this);
        msgQueue.setChannel(this);
        handshakeHandler.setChannel(this, remoteId);
        p2pHandler.setChannel(this);
        gscHandler.setChannel(this);

        p2pHandler.setMsgQueue(msgQueue);
        gscHandler.setMsgQueue(msgQueue);
        gscHandler.setPeerDel(syncManager);

    }

    public void publicHandshakeFinished(ChannelHandlerContext ctx, HelloMessage msg) {
        ctx.pipeline().remove(handshakeHandler);
        msgQueue.activate(ctx);
        ctx.pipeline().addLast("messageCodec", messageCodec);
        ctx.pipeline().addLast("p2p", p2pHandler);
        ctx.pipeline().addLast("data", gscHandler);
        setStartTime(msg.getTimestamp());
        setGscState(GscState.HANDSHAKE_FINISHED);
        getNodeStatistics().p2pHandShake.add();
        logger.info("Finish handshake with {}.", ctx.channel().remoteAddress());
    }

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    public void initNode(byte[] nodeId, int remotePort) {
        node = new Node(nodeId, inetSocketAddress.getHostString(), remotePort);
        nodeStatistics = nodeManager.getNodeStatistics(node);
    }

    public void disconnect(ReasonCode reason) {
        this.isDisconnect = true;
        channelManager.processDisconnect(this, reason);
        DisconnectMessage msg = new DisconnectMessage(reason);
        logger.info("Send to {}, {}", ctx.channel().remoteAddress(), msg);
        getNodeStatistics().nodeDisconnectedLocal(reason);
        ctx.writeAndFlush(msg.getSendData()).addListener(future -> close());
    }

    public void processException(Throwable throwable) {
        Throwable baseThrowable = throwable;
        while (baseThrowable.getCause() != null) {
            baseThrowable = baseThrowable.getCause();
        }
        String errMsg = throwable.getMessage();
        SocketAddress address = ctx.channel().remoteAddress();
        if (throwable instanceof ReadTimeoutException) {
            logger.error("Read timeout, {}", address);
        } else if (baseThrowable instanceof P2pException) {
            logger.error("type: {}, info: {}, {}", ((P2pException) baseThrowable).getType(),
                baseThrowable.getMessage(), address);
        } else if (errMsg != null && errMsg.contains("Connection reset by peer")) {
            logger.error("{}, {}", errMsg, address);
        } else {
            logger.error("exception caught, {}", address, throwable);
        }
        close();
    }

    public void close() {
        this.isDisconnect = true;
        p2pHandler.close();
        msgQueue.close();
        ctx.close();
    }

    public enum GscState {
        INIT,
        HANDSHAKE_FINISHED,
        START_TO_SYNC,
        SYNCING,
        SYNC_COMPLETED,
        SYNC_FAILED
    }

    public PeerStatistics getPeerStats() {
        return peerStats;
    }

    public Node getNode() {
        return node;
    }

    public byte[] getNodeId() {
        return node == null ? null : node.getId();
    }

    public ByteArrayWrapper getNodeIdWrapper() {
        return node == null ? null : new ByteArrayWrapper(node.getId());
    }

    public String getPeerId() {
        return node == null ? "<null>" : node.getHexId();
    }

    public void setChannelHandlerContext(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.inetSocketAddress = ctx == null ? null : (InetSocketAddress) ctx.channel().remoteAddress();
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return this.ctx;
    }

    public InetAddress getInetAddress() {
        return ctx == null ? null : ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
    }

    public NodeStatistics getNodeStatistics() {
        return nodeStatistics;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setGscState(GscState gscState) {
        this.gscState = gscState;
    }

    public GscState getGscState() {
        return gscState;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isDisconnect() {
        return isDisconnect;
    }

    public boolean isProtocolsInitialized() {
        return gscState.ordinal() > GscState.INIT.ordinal();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Channel channel = (Channel) o;
        if (inetSocketAddress != null ? !inetSocketAddress.equals(channel.inetSocketAddress)
            : channel.inetSocketAddress != null) {
            return false;
        }
        if (node != null ? !node.equals(channel.node) : channel.node != null) {
            return false;
        }
        return this == channel;
    }

    @Override
    public int hashCode() {
        int result = inetSocketAddress != null ? inetSocketAddress.hashCode() : 0;
        result = 31 * result + (node != null ? node.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", inetSocketAddress, getPeerId());
    }
}

