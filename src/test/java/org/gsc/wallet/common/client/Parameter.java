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

package org.gsc.wallet.common.client;

public interface Parameter {

    interface CommonConstant {

        int ADDRESS_SIZE = 23;
        int BASE58CHECK_ADDRESS_SIZE = 36;
        byte[] ADD_PRE_FIX_BYTE = new byte[]{(byte) 0x01, (byte) 0xf8, (byte) 0x0c};   //26 + address
        String ADD_PRE_FIX_STRING = "01f80c";
    }
}