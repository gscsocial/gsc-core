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

import com.google.protobuf.ByteString;
import org.gsc.protos.Protocol.TXInput;

public class TxInputUtil {

    /**
     * new transaction input.
     *
     * @param txId      byte[] txId
     * @param vout      int vout
     * @param signature byte[] signature
     * @param pubKey    byte[] pubKey
     * @return {@link TXInput}
     */
    public static TXInput newTxInput(byte[] txId, long vout, byte[]
            signature, byte[] pubKey) {

        TXInput.raw.Builder rawBuilder = TXInput.raw.newBuilder();

        TXInput.raw rawData = rawBuilder
                .setTxID(ByteString.copyFrom(txId))
                .setVout(vout)
                .setPubKey(ByteString.copyFrom(pubKey)).build();

        return TXInput.newBuilder()
                .setSignature(ByteString.copyFrom(signature))
                .setRawData(rawData).build();
    }
}
