package org.gsc.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BadNumberBlockException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.BlockHeader;
import org.gsc.protos.Protocol.BlockHeader.raw;

@Slf4j
public class KhaosDatabaseTest {

  private static final String dbPath = "output-khaosDatabase-test";
  private static KhaosDatabase khaosDatabase;
  private static GSCApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    khaosDatabase = context.getBean(KhaosDatabase.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void testStartBlock() {
    BlockWrapper blockWrapper = new BlockWrapper(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());
    khaosDatabase.start(blockWrapper);

    Assert.assertEquals(blockWrapper, khaosDatabase.getBlock(blockWrapper.getBlockId()));
  }

  @Test
  public void testPushGetBlock() {
    BlockWrapper blockWrapper = new BlockWrapper(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());
    BlockWrapper blockWrapper2 = new BlockWrapper(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("9938a342238077182498b464ac029222ae169360e540d1fd6aee7c2ae9575a06")))
        )).build());
    khaosDatabase.start(blockWrapper);
    try {
      khaosDatabase.push(blockWrapper2);
    } catch (UnLinkedBlockException | BadNumberBlockException e) {

    }

    Assert.assertEquals(blockWrapper2, khaosDatabase.getBlock(blockWrapper2.getBlockId()));
    Assert.assertTrue("contain is error", khaosDatabase.containBlock(blockWrapper2.getBlockId()));

    khaosDatabase.removeBlk(blockWrapper2.getBlockId());

    Assert.assertNull("removeBlk is error", khaosDatabase.getBlock(blockWrapper2.getBlockId()));
  }


  @Test
  public void checkWeakReference() throws UnLinkedBlockException, BadNumberBlockException {
    BlockWrapper blockWrapper = new BlockWrapper(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82")))
            .setNumber(0)
        )).build());
    BlockWrapper blockWrapper2 = new BlockWrapper(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            blockWrapper.getBlockId().getBytes())).setNumber(1))).build());
    Assert.assertEquals(blockWrapper.getBlockId(), blockWrapper2.getParentHash());

    khaosDatabase.start(blockWrapper);
    khaosDatabase.push(blockWrapper2);

    khaosDatabase.removeBlk(blockWrapper.getBlockId());
    logger.info("*** " + khaosDatabase.getBlock(blockWrapper.getBlockId()));
    Object object = new Object();
    Reference<Object> objectReference = new WeakReference<>(object);
    blockWrapper = null;
    object = null;
    System.gc();
    logger.info("***** object ref:" + objectReference.get());
    Assert.assertNull(objectReference.get());
    Assert.assertNull(khaosDatabase.getParentBlock(blockWrapper2.getBlockId()));
  }
}