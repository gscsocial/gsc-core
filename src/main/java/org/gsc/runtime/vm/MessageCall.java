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

package org.gsc.runtime.vm;

/**
 * A wrapper for a message call from a contract to another account. This can either be a normal
 * CALL, CALLCODE, DELEGATECALL or POST call.
 */
public class MessageCall {

    /**
     * Type of internal call. Either CALL, CALLCODE or POST
     */
    private final OpCode type;

    /**
     * cpu to pay for the call, remaining cpu will be refunded to the caller
     */
    private final DataWord cpu;
    /**
     * address of account which code to call
     */
    private final DataWord codeAddress;
    /**
     * the value that can be transfer along with the code execution
     */
    private final DataWord endowment;
    /**
     * start of memory to be input data to the call
     */
    private final DataWord inDataOffs;
    /**
     * size of memory to be input data to the call
     */
    private final DataWord inDataSize;
    /**
     * start of memory to be output of the call
     */
    private DataWord outDataOffs;
    /**
     * size of memory to be output data to the call
     */
    private DataWord outDataSize;

    private DataWord tokenId;

    private boolean isTokenTransferMsg;

    public MessageCall(OpCode type, DataWord cpu, DataWord codeAddress,
                       DataWord endowment, DataWord inDataOffs, DataWord inDataSize, DataWord tokenId,
                       boolean isTokenTransferMsg) {
        this.type = type;
        this.cpu = cpu;
        this.codeAddress = codeAddress;
        this.endowment = endowment;
        this.inDataOffs = inDataOffs;
        this.inDataSize = inDataSize;
        this.tokenId = tokenId;
        this.isTokenTransferMsg = isTokenTransferMsg;
    }

    public MessageCall(OpCode type, DataWord cpu, DataWord codeAddress,
                       DataWord endowment, DataWord inDataOffs, DataWord inDataSize,
                       DataWord outDataOffs, DataWord outDataSize, DataWord tokenId, boolean isTokenTransferMsg) {
        this(type, cpu, codeAddress, endowment, inDataOffs, inDataSize, tokenId, isTokenTransferMsg);
        this.outDataOffs = outDataOffs;
        this.outDataSize = outDataSize;
    }

    public OpCode getType() {
        return type;
    }

    public DataWord getCpu() {
        return cpu;
    }

    public DataWord getCodeAddress() {
        return codeAddress;
    }

    public DataWord getEndowment() {
        return endowment;
    }

    public DataWord getInDataOffs() {
        return inDataOffs;
    }

    public DataWord getInDataSize() {
        return inDataSize;
    }

    public DataWord getOutDataOffs() {
        return outDataOffs;
    }

    public DataWord getOutDataSize() {
        return outDataSize;
    }

    public DataWord getTokenId() {
        return tokenId;
    }

    public boolean isTokenTransferMsg() {
        return isTokenTransferMsg;
    }
}
