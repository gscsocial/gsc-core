package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.exception.ContractExeException;
import org.gsc.common.exception.ContractValidateException;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.config.Args;
import org.gsc.config.DefaultConfig;
import org.gsc.core.Constant;
import org.gsc.core.chain.TransactionResultWrapper;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;

import static junit.framework.TestCase.fail;

@Slf4j
public class TransferTokenOperatorTest {
    private static AnnotationConfigApplicationContext context;
    private static Manager dbManager;
    private static Any contract;
    private static final String dbPath = "output_transfertoken_test";
    private static final String ASSET_NAME = "gsc";
    private static final String OWNER_ADDRESS;
    private static final String TO_ADDRESS;
    private static final long OWNER_ASSET_BALANCE = 99999;
    private static final long TOTAL_SUPPLY = 10L;
    private static final int TRX_NUM = 10;
    private static final int NUM = 1;
    private static final long START_TIME = 1;
    private static final long END_TIME = 2;
    private static final int VOTE_SCORE = 2;
    private static final String DESCRIPTION = "global social chain";
    private static final String URL = "https://gsc.social";

    static {
        Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
        context = new AnnotationConfigApplicationContext(DefaultConfig.class);
        OWNER_ADDRESS = Constant.ADD_PRE_FIX_STRING_MAINNET + "abd4b9367799eaa3197fecb144eb71de1e049150";
        TO_ADDRESS = Constant.ADD_PRE_FIX_STRING_MAINNET + "548794500882809695a8a687866e76d4271a146a";
    }

    @Before
    public void init(){
        dbManager = context.getBean(Manager.class);
    }

    @Before
    public void createWrapper() {
        AccountWrapper ownerWrapper =
                new AccountWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                        Protocol.AccountType.AssetIssue,System.currentTimeMillis());
        ownerWrapper.getInstance().getAssetMap().put(ASSET_NAME, OWNER_ASSET_BALANCE);

        AccountWrapper toAccountWrapper =
                new AccountWrapper(
                        ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
                        Protocol.AccountType.Normal,System.currentTimeMillis());
        Contract.AssetIssueContract assetIssueContract =
                Contract.AssetIssueContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
                        .setTotalSupply(TOTAL_SUPPLY)
                        .setTrxNum(TRX_NUM)
                        .setNum(NUM)
                        .setStartTime(START_TIME)
                        .setEndTime(END_TIME)
                        .setVoteScore(VOTE_SCORE)
                        .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
                        .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
                        .build();
        AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
        dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
        dbManager.getAccountStore().put(toAccountWrapper.getAddress().toByteArray(), toAccountWrapper);
        dbManager.getAssetIssueStore()
                .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);
    }


    public void createAsset(String assetName) {
        AccountWrapper ownerWrapper = dbManager.getAccountStore()
                .get(ByteArray.fromHexString(OWNER_ADDRESS));
        ownerWrapper.getInstance().getAssetMap().put(assetName, OWNER_ASSET_BALANCE);

        Contract.AssetIssueContract assetIssueContract =
                Contract.AssetIssueContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
                        .setTotalSupply(TOTAL_SUPPLY)
                        .setTrxNum(TRX_NUM)
                        .setNum(NUM)
                        .setStartTime(START_TIME)
                        .setEndTime(END_TIME)
                        .setVoteScore(VOTE_SCORE)
                        .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
                        .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
                        .build();
        AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(assetIssueContract);
        dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);
        dbManager.getAssetIssueStore()
                .put(assetIssueWrapper.getName().toByteArray(), assetIssueWrapper);
    }

    private Any getContract(long sendCoin) {
        return Any.pack(
                Contract.TransferAssetContract.newBuilder()
                        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
                        .setAmount(sendCoin)
                        .build());
    }

    private Any getContract(long sendCoin, ByteString assetName) {
        return Any.pack(
                Contract.TransferAssetContract.newBuilder()
                        .setAssetName(assetName)
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
                        .setAmount(sendCoin)
                        .build());
    }

    private Any getContract(long sendCoin, String assetName) {
        return Any.pack(
                Contract.TransferAssetContract.newBuilder()
                        .setAssetName(ByteString.copyFrom(ByteArray.fromString(assetName)))
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
                        .setAmount(sendCoin)
                        .build());
    }

    private Any getContract(long sendCoin, String owner, String to) {
        return Any.pack(
                Contract.TransferAssetContract.newBuilder()
                        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
                        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(to)))
                        .setAmount(sendCoin)
                        .build());
    }
    @Test
    public void testTransfer() {
        TransferTokenOperator actuator = new TransferTokenOperator(getContract(100L));
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            actuator.validate();
            actuator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCCESS);
            AccountWrapper owner =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
            AccountWrapper toAccount =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
            Assert.assertEquals(owner.getInstance().getAssetMap().get(ASSET_NAME).longValue(),
                    OWNER_ASSET_BALANCE - 100);
            Assert.assertEquals(toAccount.getInstance().getAssetMap().get(ASSET_NAME).longValue(), 100L);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void addOverflowTest() {
        // First, increase the to balance. Else can't complete this test case.
        AccountWrapper toAccount = dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
        toAccount.getInstance().getAssetMap().put(ASSET_NAME, Long.MAX_VALUE);
        dbManager.getAccountStore().put(ByteArray.fromHexString(TO_ADDRESS), toAccount);
        TransferTokenOperator actuator = new TransferTokenOperator(getContract(1));
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            actuator.validate();
            actuator.execute(ret);
            Assert.assertTrue(false);
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertTrue("long overflow".equals(e.getMessage()));
            AccountWrapper owner =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
            toAccount =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
            Assert.assertEquals(owner.getInstance().getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
            Assert.assertEquals(toAccount.getInstance().getAssetMap().get(ASSET_NAME).longValue(), Long.MAX_VALUE);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * transfer asset to yourself,result is error
     */
    @Test
    public void transferToYourself() {
        TransferTokenOperator actuator = new TransferTokenOperator(
                getContract(100L, OWNER_ADDRESS, OWNER_ADDRESS));
        TransactionResultWrapper ret = new TransactionResultWrapper();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("Cannot transfer asset to yourself.");

        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Cannot transfer asset to yourself.", e.getMessage());
            AccountWrapper owner = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(OWNER_ADDRESS));
            AccountWrapper toAccount = dbManager.getAccountStore()
                    .get(ByteArray.fromHexString(TO_ADDRESS));
            Assert.assertEquals(owner.getInstance().getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
            Assert.assertTrue(isNullOrZero(toAccount.getInstance().getAssetMap().get(ASSET_NAME)));
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    private boolean isNullOrZero(Long value) {
        if (null == value || value == 0) {
            return true;
        }
        return false;
    }

    @AfterClass
    public static void destroy() {
        Args.clearParam();
        if (FileUtil.deleteDir(new File(dbPath))) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
        context.destroy();
    }
}
