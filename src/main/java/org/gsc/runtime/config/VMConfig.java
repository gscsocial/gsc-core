/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */


package org.gsc.runtime.config;

import lombok.Setter;
import org.gsc.config.Parameter;
import org.gsc.utils.ForkController;
import org.gsc.config.Parameter.ForkBlockVersionConsts;
import org.gsc.config.args.Args;

/**
 * For developer only
 */
public class VMConfig {

    public static final int MAX_CODE_LENGTH = 1024 * 1024;

    public static final int MAX_FEE_LIMIT = 1_000_000_000; //1000 gsc

    private boolean vmTraceCompressed = false;
    private boolean vmTrace = Args.getInstance().isVmTrace();

    //Odyssey3.2 hard fork -- ForkBlockVersionConsts.CPU_LIMIT
//    @Setter
//    private static boolean CPU_LIMIT_HARD_FORK = false;

//  @Getter
//  @Setter
//  private static boolean VERSION_3_5_HARD_FORK = false;

    @Setter
    private static boolean ALLOW_GVM_TRANSFER_GRC10 = false;

    @Setter
    private static boolean ALLOW_GVM_CONSTANTINOPLE = false;

    @Setter
    private static boolean ALLOW_MULTI_SIGN = false;

    private VMConfig() {
    }

    private static class SystemPropertiesInstance {

        private static final VMConfig INSTANCE = new VMConfig();
    }

    public static VMConfig getInstance() {
        return SystemPropertiesInstance.INSTANCE;
    }

    public boolean vmTrace() {
        return vmTrace;
    }

    public boolean vmTraceCompressed() {
        return vmTraceCompressed;
    }

//    public static void initVmHardFork() {
////        CPU_LIMIT_HARD_FORK = ForkController.instance().pass(ForkBlockVersionConsts.CPU_LIMIT);
//        VERSION_2_0_HARD_FORK = ForkController.instance().pass(Parameter.ForkBlockVersionEnum.VERSION_2_0);
//    }

    public static void initAllowMultiSign(long allow) {
        ALLOW_MULTI_SIGN = allow == 1;
    }

    public static void initAllowGvmTransferGrc10(long allow) {
        ALLOW_GVM_TRANSFER_GRC10 = allow == 1;
    }

    public static void initAllowGvmConstantinople(long allow) {
        ALLOW_GVM_CONSTANTINOPLE = allow == 1;
    }

//    public static boolean getCpuLimitHardFork() {
//        return CPU_LIMIT_HARD_FORK;
//    }

    public static boolean allowGvmTransferGrc10() {
        return ALLOW_GVM_TRANSFER_GRC10;
    }

    public static boolean allowGvmConstantinople() {
        return ALLOW_GVM_CONSTANTINOPLE;
    }

    public static boolean allowMultiSign() {
        return ALLOW_MULTI_SIGN;
    }

}