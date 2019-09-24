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


package org.gsc.core.wrapper.utils;

import java.util.ArrayList;

import org.gsc.utils.ByteUtil;

/**
 */
public class RLPList extends ArrayList<RLPElement> implements RLPElement {

    byte[] rlpData;

    public void setRLPData(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    public byte[] getRLPData() {
        return rlpData;
    }

    public static void recursivePrint(RLPElement element) {

        if (element == null) {
            throw new RuntimeException("RLPElement object can't be null");
        }
        if (element instanceof RLPList) {

            RLPList rlpList = (RLPList) element;
            System.out.print("[");
            for (RLPElement singleElement : rlpList) {
                recursivePrint(singleElement);
            }
            System.out.print("]");
        } else {
            String hex = ByteUtil.toHexString(element.getRLPData());
            System.out.print(hex + ", ");
        }
    }
}
