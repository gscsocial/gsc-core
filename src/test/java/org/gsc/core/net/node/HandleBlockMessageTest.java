package org.gsc.core.net.node;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.net.node.Item;
import org.gsc.net.node.NodeDelegate;
import org.gsc.net.node.NodeDelegateImpl;
import org.gsc.net.node.NodeImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.common.application.Application;
import org.gsc.common.application.ApplicationFactory;
import org.gsc.crypto.ECKey;
import org.gsc.common.overlay.client.PeerClient;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.common.overlay.server.Channel;
import org.gsc.common.overlay.server.ChannelManager;
import org.gsc.common.overlay.server.SyncPool;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.ReflectUtils;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.ByteArrayWrapper;
import org.gsc.db.Manager;
import org.gsc.net.message.BlockMessage;
import org.gsc.net.peer.PeerConnection;
import org.gsc.services.RpcApiService;
import org.gsc.services.WitnessService;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.BlockHeader;
import org.gsc.protos.Protocol.Inventory.InventoryType;


@Slf4j
public class HandleBlockMessageTest {

    private static GSCApplicationContext context;
    private NodeImpl node;
    RpcApiService rpcApiService;
    PeerClient peerClient;
    ChannelManager channelManager;
    SyncPool pool;
    Application appT;
    Manager dbManager;

    private static final String dbPath = "output-HandleBlockMessageTest";
    private static final String dbDirectory = "db_HandleBlockMessage_test";
    private static final String indexDirectory = "index_HandleBlockMessage_test";

    private static Boolean deleteFolder(File index) {
        if (!index.isDirectory() || index.listFiles().length <= 0) {
            return index.delete();
        }
        for (File file : index.listFiles()) {
            if (null != file && !deleteFolder(file)) {
                return false;
            }
        }
        return index.delete();
    }

    @Test
    public void testHandleBlockMessage() throws Exception {
        List<PeerConnection> activePeers = ReflectUtils.getFieldValue(pool, "activePeers");
        PeerConnection peer = activePeers.get(0);

        //receive a sync block
        BlockWrapper headBlockWrapper = dbManager.getHead();
        BlockWrapper syncblockWrapper = generateOneBlockCapsule(headBlockWrapper);
        BlockMessage blockMessage = new BlockMessage(syncblockWrapper);
        peer.getSyncBlockRequested().put(blockMessage.getBlockId(), System.currentTimeMillis());
        node.onMessage(peer, blockMessage);
        Assert.assertEquals(peer.getSyncBlockRequested().isEmpty(), true);

        //receive a advertise block
        BlockWrapper advblockWrapper = generateOneBlockCapsule(headBlockWrapper);
        BlockMessage advblockMessage = new BlockMessage(advblockWrapper);
        peer.getAdvObjWeRequested().put(new Item(advblockMessage.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
        node.onMessage(peer, advblockMessage);
        Assert.assertEquals(peer.getAdvObjWeRequested().size(), 0);

        //receive a sync block but not requested
        BlockWrapper blockWrapper = generateOneBlockCapsule(headBlockWrapper);
        blockMessage = new BlockMessage(blockWrapper);
        BlockWrapper blockWrapperOther = generateOneBlockCapsule(blockWrapper);
        BlockMessage blockMessageOther = new BlockMessage(blockWrapperOther);

        peer.getSyncBlockRequested().put(blockMessage.getBlockId(), System.currentTimeMillis());
        node.onMessage(peer, blockMessageOther);
        Assert.assertEquals(peer.getSyncBlockRequested().isEmpty(), false);
    }

    // generate ong block by parent block
    private BlockWrapper generateOneBlockCapsule(BlockWrapper parentCapsule) {
        ByteString witnessAddress = ByteString.copyFrom(
                ECKey.fromPrivate(
                        ByteArray.fromHexString(
                                Args.getInstance().getLocalWitnesses().getPrivateKey()))
                        .getAddress());
        BlockHeader.raw raw = BlockHeader.raw.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setParentHash(parentCapsule.getBlockId().getByteString())
                .setNumber(parentCapsule.getNum() + 1)
                .setWitnessAddress(witnessAddress)
                .setWitnessId(1).build();
        BlockHeader blockHeader = BlockHeader.newBuilder()
                .setRawData(raw)
                .build();

        Block block = Block.newBuilder().setBlockHeader(blockHeader).build();

        BlockWrapper blockWrapper = new BlockWrapper(block);
        blockWrapper.setMerkleRoot();
        blockWrapper.sign(
                ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));
        blockWrapper.setMerkleRoot();
        blockWrapper.sign(
                ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

        return blockWrapper;
    }

    private static boolean go = false;

    @Before
    public void init() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("Full node running.");
                Args.setParam(
                        new String[]{
                                "--output-directory", dbPath,
                                "--storage-db-directory", dbDirectory,
                                "--storage-index-directory", indexDirectory
                        },
                        Constant.TEST_CONF
                );
                Args cfgArgs = Args.getInstance();
                cfgArgs.setNodeListenPort(17894);
                cfgArgs.setNodeDiscoveryEnable(false);
                cfgArgs.getSeedNode().getIpList().clear();
                cfgArgs.setNeedSyncCheck(false);
                cfgArgs.setNodeExternalIp("127.0.0.1");

                context = new GSCApplicationContext(DefaultConfig.class);

                if (cfgArgs.isHelp()) {
                    logger.info("Here is the help message.");
                    return;
                }
                appT = ApplicationFactory.create(context);
                rpcApiService = context.getBean(RpcApiService.class);
                appT.addService(rpcApiService);
                if (cfgArgs.isWitness()) {
                    appT.addService(new WitnessService(appT, context));
                }
//        appT.initServices(cfgArgs);
//        appT.startServices();
//        appT.startup();
                node = context.getBean(NodeImpl.class);
                peerClient = context.getBean(PeerClient.class);
                channelManager = context.getBean(ChannelManager.class);
                pool = context.getBean(SyncPool.class);
                dbManager = context.getBean(Manager.class);
                NodeDelegate nodeDelegate = new NodeDelegateImpl(dbManager);
                node.setNodeDelegate(nodeDelegate);
                pool.init(node);
                prepare();
                rpcApiService.blockUntilShutdown();
            }
        }).start();
        int tryTimes = 0;
        while (tryTimes < 10 && (node == null || peerClient == null
                || channelManager == null || pool == null || !go)) {
            try {
                logger.info("node:{},peerClient:{},channelManager:{},pool:{},{}", node, peerClient,
                        channelManager, pool, go);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                ++tryTimes;
            }
        }
    }

    private void prepare() {
        try {
            ExecutorService advertiseLoopThread = ReflectUtils.getFieldValue(node, "broadPool");
            advertiseLoopThread.shutdownNow();

            ReflectUtils.setFieldValue(node, "isAdvertiseActive", false);
            ReflectUtils.setFieldValue(node, "isFetchActive", false);

            Node node = new Node(
                    "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17894");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    peerClient.connect(node.getHost(), node.getPort(), node.getHexId());
                }
            }).start();
            Thread.sleep(1000);
            Map<ByteArrayWrapper, Channel> activePeers = ReflectUtils
                    .getFieldValue(channelManager, "activePeers");
            int tryTimes = 0;
            while (MapUtils.isEmpty(activePeers) && ++tryTimes < 10) {
                Thread.sleep(1000);
            }
            go = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void removeDb() {
        Args.clearParam();

        File dbFolder = new File(dbPath);
        if (deleteFolder(dbFolder)) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
        context.destroy();
    }
}
