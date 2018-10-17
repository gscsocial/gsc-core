/*
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gsc-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gsc.core;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.utils.BlockUtil;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.BlockHeader;
import org.gsc.protos.Protocol.BlockHeader.raw;

@Slf4j
public class BlockUtilTest {

  @Before
  public void initConfiguration() {
    Args.setParam(new String[]{}, Constant.TEST_CONF);
  }

  @After
  public void destroy() {
    Args.clearParam();
  }

  @Test
  public void testBlockUtil() {
    //test create GenesisBlockCapsule
    BlockWrapper blockWrapper1 = BlockUtil.newGenesisBlockCapsule();
    Sha256Hash sha256Hash = Sha256Hash.wrap(ByteArray
        .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"));

    Assert.assertEquals(0, blockWrapper1.getTimeStamp());
    Assert.assertEquals(sha256Hash,
        blockWrapper1.getParentHash());
    Assert.assertEquals(0, blockWrapper1.getNum());

    //test isParentOf method: create blockWrapper2 and blockWrapper3
    // blockWrapper3.setParentHash() equals blockWrapper2.getBlockId
    BlockWrapper blockWrapper2 = new BlockWrapper(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());

    BlockWrapper blockWrapper3 = new BlockWrapper(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString(blockWrapper2.getBlockId().toString())))
        )).build());

    Assert.assertEquals(false, BlockUtil.isParentOf(blockWrapper1, blockWrapper2));
    Assert.assertFalse(BlockUtil.isParentOf(blockWrapper1, blockWrapper2));
    Assert.assertEquals(true, BlockUtil.isParentOf(blockWrapper2, blockWrapper3));
    Assert.assertTrue(BlockUtil.isParentOf(blockWrapper2, blockWrapper3));

  }
}
