package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.FileUtil;
import org.gsc.config.Args;
import org.gsc.protos.Protocol;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
public class AccountWrapperTest {

    private static String dbPath = "output_AccountWrapper_test";
    static AccountWrapper accountWrapperTest;
    static AccountWrapper accountWrapper;

    @BeforeClass
    public static void init() {
        Args.args = new String[]{"-d", dbPath};
        ByteString accountName = ByteString.copyFrom(AccountWrapperTest.randomBytes(16));
        ByteString address = ByteString.copyFrom(AccountWrapperTest.randomBytes(32));
        Protocol.AccountType accountType = Protocol.AccountType.forNumber(1);
        accountWrapperTest = new AccountWrapper(address,accountType,System.currentTimeMillis());
        byte[] accountByte = accountWrapperTest.getData();
        accountWrapper = new AccountWrapper(accountByte);
        accountWrapperTest.setBalance(1111L);
    }


    @AfterClass
    public static void removeDb() {
        FileUtil.deleteDir(new File(dbPath));
    }

    @Test
    public void getDataTest() {
        //test accountWrapper onstructed function
        Assert.assertEquals(accountWrapper.getBalance(), 0);
        Assert.assertEquals(1111, accountWrapperTest.getBalance());
    }


    @Test
    public void AssetAmountTest() {
        //test AssetAmount ,addAsset and reduceAssetAmount function

        String nameAdd = "TokenX";
        long amountAdd = 222L;
        boolean addBoolean = accountWrapperTest.addTokenAmount(ByteString.copyFromUtf8(nameAdd), amountAdd);

        Assert.assertTrue(addBoolean);

        long amountReduce = 22L;

        boolean reduceBoolean = accountWrapperTest.reduceTokenAmount(ByteString.copyFromUtf8("TokenX"), amountReduce);
        Assert.assertTrue(reduceBoolean);

    }

    public static byte[] randomBytes(int length) {
        //generate the random number
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

}
