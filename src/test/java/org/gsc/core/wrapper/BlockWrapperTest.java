package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.common.utils.Sha256Hash;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class BlockWrapperTest {

    private static String dbPath = "output_AccountWrapper_test";

    private static BlockWrapper blockWrapper1 = new BlockWrapper(1,
            Sha256Hash.wrap(ByteString
                    .copyFrom(ByteArray
                            .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
            1234,
            ByteString.copyFrom("1234567".getBytes()));

    @Before
    public void init(){
        //Args.args = new String[]{"-d", dbPath};
    }

    @AfterClass
    public static void removeDb() {
        FileUtil.deleteDir(new File(dbPath));
    }

    @Test
    public void testGetData() {
        blockWrapper1.getData();
        byte[] b = blockWrapper1.getData();
        BlockWrapper blockCapsule1 = null;
        try {
            blockCapsule1 = new BlockWrapper(b);
            Assert.assertEquals(blockWrapper1.getBlockId(), blockCapsule1.getBlockId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetInsHash() {
        Assert.assertEquals(1,
                blockWrapper1.getInstance().getBlockHeader().getRawData().getNumber());
        Assert.assertEquals(blockWrapper1.getParentHash(),
                Sha256Hash.wrap(blockWrapper1.getParentHashStr()));
    }

    @Test
    public void testGetTimeStamp() {
        Assert.assertEquals(1234L, blockWrapper1.getTimeStamp());
    }


}
