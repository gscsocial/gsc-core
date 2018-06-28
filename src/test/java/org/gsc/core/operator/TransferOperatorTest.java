package org.gsc.core.operator;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.FileUtil;
import org.gsc.config.Args;
import org.gsc.config.DefaultConfig;
import org.gsc.core.Constant;
import org.gsc.db.Manager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;

@Slf4j
public class TransferOperatorTest {
    private static Manager dbManager;
    private static final String dbPath = "output_transfer_test";
    private static AnnotationConfigApplicationContext context;
    private static final String OWNER_ADDRESS;
    private static final String TO_ADDRESS;
    private static final long AMOUNT = 100;
    private static final long OWNER_BALANCE = 9999999;
    private static final long TO_BALANCE = 100001;
    private static final String OWNER_ADDRESS_INVALIDATE = "aaaa";
    private static final String TO_ADDRESS_INVALIDATE = "bbb";
    private static final String OWNER_ACCOUNT_INVALIDATE;
    private static final String OWNER_NO_BALANCE;
    private static final String To_ACCOUNT_INVALIDATE;

    static {
        Args.args = new String[]{"--output-directory", dbPath};
        context = new AnnotationConfigApplicationContext(DefaultConfig.class);
        OWNER_ADDRESS = Constant.ADD_PRE_FIX_STRING_MAINNET + "548794500882809695a8a687866e76d4271a1abc";
        TO_ADDRESS = Constant.ADD_PRE_FIX_STRING_MAINNET + "abd4b9367799eaa3197fecb144eb71de1e049abc";
        OWNER_ACCOUNT_INVALIDATE =
                Constant.ADD_PRE_FIX_STRING_MAINNET + "548794500882809695a8a687866e76d4271a3456";
        OWNER_NO_BALANCE = Constant.ADD_PRE_FIX_STRING_MAINNET + "548794500882809695a8a687866e76d4271a3433";
        To_ACCOUNT_INVALIDATE =
                Constant.ADD_PRE_FIX_STRING_MAINNET + "548794500882809695a8a687866e76d4271a3422";
    }

    /**
     * Init data.
     */
    @BeforeClass
    public static void init() {
        dbManager = context.getBean(Manager.class);
        //    Args.setParam(new String[]{"--output-directory", dbPath},
        //        "config-junit.conf");
        //    dbManager = new Manager();
        //    dbManager.init();
    }

    /**
     * Release resources.
     */
    @AfterClass
    public static void destroy() {
        if (FileUtil.deleteDir(new File(dbPath))) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
        context.destroy();
    }

    @Test
    public void test(){
        System.out.println("load spring success");
    }
}
