package org.gsc.core.net.node;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.net.node.Item;
import org.gsc.net.node.NodeDelegateImpl;
import org.gsc.net.node.NodeImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.common.application.Application;
import org.gsc.common.application.ApplicationFactory;
import org.gsc.crypto.ECKey;
import org.gsc.common.overlay.server.SyncPool;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.wrapper.utils.BlockUtil;
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter.NetConstants;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.net.message.BlockMessage;
import org.gsc.net.peer.PeerConnection;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.BlockHeader;
import org.gsc.protos.Protocol.Inventory.InventoryType;


@Slf4j
public class NodeImplTest {

  private static GSCApplicationContext context;

  private static Application appT;
  private static String dbPath = "output_nodeimpl_test";
  private static NodeImpl nodeImpl;
  private static Manager dbManager;
  private static NodeDelegateImpl nodeDelegate;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    Args.getInstance().setSolidityNode(true);
    appT = ApplicationFactory.create(context);
  }

  /**
   * init db.
   */
  @BeforeClass
  public static void init() {
    nodeImpl = context.getBean(NodeImpl.class);
    dbManager = context.getBean(Manager.class);
    nodeDelegate = new NodeDelegateImpl(dbManager);
    nodeImpl.setNodeDelegate(nodeDelegate);
  }

  /**
   * remo db when after test.
   */
  @AfterClass
  public static void removeDb() {
    Args.clearParam();

    File dbFolder = new File(dbPath);
    if (deleteFolder(dbFolder)) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
    appT.shutdownServices();
    appT.shutdown();
  }

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
  public void testSyncBlockMessage() throws Exception {
    PeerConnection peer = new PeerConnection();
    BlockWrapper genesisBlockWrapper = BlockUtil.newGenesisBlockCapsule();

    ByteString witnessAddress = ByteString.copyFrom(
        ECKey.fromPrivate(
            ByteArray.fromHexString(
                Args.getInstance().getLocalWitnesses().getPrivateKey()))
            .getAddress());
    BlockHeader.raw raw = BlockHeader.raw.newBuilder()
        .setTimestamp(System.currentTimeMillis())
        .setParentHash(genesisBlockWrapper.getParentHash().getByteString())
        .setNumber(genesisBlockWrapper.getNum() + 1)
        .setWitnessAddress(witnessAddress)
        .setWitnessId(1).build();
    BlockHeader blockHeader = BlockHeader.newBuilder()
        .setRawData(raw)
        .build();

    Block block = Block.newBuilder().setBlockHeader(blockHeader).build();

    BlockWrapper blockWrapper = new BlockWrapper(block);
    blockWrapper.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));
    blockWrapper.setMerkleRoot();
    BlockMessage blockMessage = new BlockMessage(blockWrapper);
    peer.getSyncBlockRequested().put(blockMessage.getBlockId(), System.currentTimeMillis());
    nodeImpl.onMessage(peer, blockMessage);
    Assert.assertEquals(peer.getSyncBlockRequested().size(), 0);
  }

  @Test
  public void testAdvBlockMessage() throws Exception {
    PeerConnection peer = new PeerConnection();
    BlockWrapper genesisBlockWrapper = BlockUtil.newGenesisBlockCapsule();

    ByteString witnessAddress = ByteString.copyFrom(
        ECKey.fromPrivate(
            ByteArray.fromHexString(
                Args.getInstance().getLocalWitnesses().getPrivateKey()))
            .getAddress());
    BlockHeader.raw raw = BlockHeader.raw.newBuilder()
        .setTimestamp(System.currentTimeMillis())
        .setParentHash(genesisBlockWrapper.getBlockId().getByteString())
        .setNumber(genesisBlockWrapper.getNum() + 1)
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
    BlockMessage blockMessage = new BlockMessage(blockWrapper);
    peer.getAdvObjWeRequested().put(new Item(blockMessage.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
    nodeImpl.onMessage(peer, blockMessage);
    Assert.assertEquals(peer.getAdvObjWeRequested().size(), 0);
  }

  //  @Test
  public void testDisconnectInactive() {
    // generate test data
    ConcurrentHashMap<Item, Long> advObjWeRequested1 = new ConcurrentHashMap<>();
    ConcurrentHashMap<Item, Long> advObjWeRequested2 = new ConcurrentHashMap<>();
    ConcurrentHashMap<Item, Long> advObjWeRequested3 = new ConcurrentHashMap<>();
    ConcurrentHashMap<BlockId, Long> syncBlockRequested1 = new ConcurrentHashMap<>();
    ConcurrentHashMap<BlockId, Long> syncBlockRequested2 = new ConcurrentHashMap<>();
    ConcurrentHashMap<BlockId, Long> syncBlockRequested3 = new ConcurrentHashMap<>();

    advObjWeRequested1.put(new Item(new Sha256Hash(1, Sha256Hash.ZERO_HASH), InventoryType.TRX),
        System.currentTimeMillis() - NetConstants.ADV_TIME_OUT);
    syncBlockRequested1.put(new BlockId(),
        System.currentTimeMillis());
    advObjWeRequested2.put(new Item(new Sha256Hash(1, Sha256Hash.ZERO_HASH), InventoryType.TRX),
        System.currentTimeMillis());
    syncBlockRequested2.put(new BlockId(),
        System.currentTimeMillis() - NetConstants.SYNC_TIME_OUT);
    advObjWeRequested3.put(new Item(new Sha256Hash(1, Sha256Hash.ZERO_HASH), InventoryType.TRX),
        System.currentTimeMillis());
    syncBlockRequested3.put(new BlockId(),
        System.currentTimeMillis());

    PeerConnection peer1 = new PeerConnection();
    PeerConnection peer2 = new PeerConnection();
    PeerConnection peer3 = new PeerConnection();

    peer1.setAdvObjWeRequested(advObjWeRequested1);
    peer1.setSyncBlockRequested(syncBlockRequested1);
    peer2.setAdvObjWeRequested(advObjWeRequested2);
    peer2.setSyncBlockRequested(syncBlockRequested2);
    peer3.setAdvObjWeRequested(advObjWeRequested3);
    peer3.setSyncBlockRequested(syncBlockRequested3);

    // fetch failed
    SyncPool pool = new SyncPool();
    pool.addActivePeers(peer1);
    nodeImpl.setPool(pool);
    try {
      nodeImpl.disconnectInactive();
      fail("disconnectInactive failed");
    } catch (RuntimeException e) {
      assertTrue("disconnect successfully, reason is fetch failed", true);
    }

    // sync failed
    pool = new SyncPool();
    pool.addActivePeers(peer2);
    nodeImpl.setPool(pool);
    try {
      nodeImpl.disconnectInactive();
      fail("disconnectInactive failed");
    } catch (RuntimeException e) {
      assertTrue("disconnect successfully, reason is sync failed", true);
    }

    // should not disconnect
    pool = new SyncPool();
    pool.addActivePeers(peer3);
    nodeImpl.setPool(pool);
    try {
      nodeImpl.disconnectInactive();
      assertTrue("not disconnect", true);
    } catch (RuntimeException e) {
      fail("should not disconnect!");
    }
  }
}
