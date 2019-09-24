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

package org.gsc.runtime.vm.program;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.OpCode;


@Slf4j(topic = "VM")
/**
 * Created by Anton Nashatyrev on 06.02.2017.
 */
public class ProgramPrecompile {

    private Set<Integer> jumpdest = new HashSet<>();

    public static ProgramPrecompile compile(byte[] ops) {
        ProgramPrecompile ret = new ProgramPrecompile();
        for (int i = 0; i < ops.length; ++i) {

            OpCode op = OpCode.code(ops[i]);
            if (op == null) {
                continue;
            }

            if (op.equals(OpCode.JUMPDEST)) {
                logger.debug("JUMPDEST:" + i);
                ret.jumpdest.add(i);
            }

            if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
                i += op.asInt() - OpCode.PUSH1.asInt() + 1;
            }
        }
        return ret;
    }

    public static byte[] getCode(byte[] ops) {
        for (int i = 0; i < ops.length; ++i) {

            OpCode op = OpCode.code(ops[i]);
            if (op == null) {
                continue;
            }

            if (op.equals(OpCode.RETURN)) {
                logger.debug("return");
            }

            if (op.equals(OpCode.RETURN) && i + 1 < ops.length && OpCode.code(ops[i + 1]) != null
                    && OpCode.code(ops[i + 1]).equals(OpCode.STOP)) {
                byte[] ret;
                i++;
                ret = new byte[ops.length - i - 1];

                System.arraycopy(ops, i + 1, ret, 0, ops.length - i - 1);
                return ret;
            }

            if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
                i += op.asInt() - OpCode.PUSH1.asInt() + 1;
            }
        }
        if (VMConfig.allowGvmConstantinople()) {
            return new byte[0];
        } else {
            return new DataWord(0).getData();
        }
    }

    public boolean hasJumpDest(int pc) {
        return jumpdest.contains(pc);
    }
}
