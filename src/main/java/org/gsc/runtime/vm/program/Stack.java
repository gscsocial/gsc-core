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

import java.util.Objects;

import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.listener.ProgramListener;
import org.gsc.runtime.vm.program.listener.ProgramListenerAware;

public class Stack extends java.util.Stack<DataWord> implements ProgramListenerAware {

    private static final long serialVersionUID = 1;

    private transient ProgramListener programListener;

    @Override
    public void setProgramListener(ProgramListener listener) {
        this.programListener = listener;
    }

    @Override
    public synchronized DataWord pop() {
        if (programListener != null) {
            programListener.onStackPop();
        }
        return super.pop();
    }

    @Override
    public DataWord push(DataWord item) {
        if (programListener != null) {
            programListener.onStackPush(item);
        }
        return super.push(item);
    }

    public void swap(int from, int to) {
        if (isAccessible(from) && isAccessible(to) && (from != to)) {
            if (programListener != null) {
                programListener.onStackSwap(from, to);
            }
            DataWord tmp = get(from);
            set(from, set(to, tmp));
        }
    }

    private boolean isAccessible(int from) {
        return from >= 0 && from < size();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o.getClass() != this.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Stack dataWords = (Stack) o;
        return Objects.equals(programListener, dataWords.programListener);
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), programListener);
    }
}
