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

package org.gsc.runtime.vm.program.invoke;

import com.google.protobuf.ByteString;
import org.gsc.core.wrapper.ContractWrapper;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.Hash;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.Program.IllegalOperationException;
import org.gsc.db.dbsource.Deposit;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.exception.StoreException;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.SmartContract;


/**
 * .
 *
 * @author Roman Mandeleil
 * @since 03.06.2014
 */
public class ProgramInvokeMockImpl implements ProgramInvoke {

    private byte[] msgData;

    private DepositImpl deposit;
    private byte[] ownerAddress = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
    private final byte[] contractAddress = Hex.decode("471fd3ad3e9eeadeec4608b92d16ce6b500704cc");

    private boolean isStaticCall;

    public ProgramInvokeMockImpl(byte[] msgDataRaw) {
        this();
        this.msgData = Arrays.clone(msgDataRaw);
    }

    private long cpuLimit = 50;

    public ProgramInvokeMockImpl() {

        this.deposit = DepositImpl.createRoot(null);
        this.deposit.createAccount(ownerAddress, Protocol.AccountType.Normal);

        this.deposit.createAccount(contractAddress, Protocol.AccountType.Contract);
        this.deposit.createContract(contractAddress,
                new ContractWrapper(SmartContract.newBuilder().setContractAddress(
                        ByteString.copyFrom(contractAddress)).build()));
        this.deposit.saveCode(contractAddress,
                Hex.decode("385E60076000396000605f556014600054601e60"
                        + "205463abcddcba6040545b51602001600a525451"
                        + "6040016014525451606001601e52545160800160"
                        + "28525460a052546016604860003960166000f260"
                        + "00603f556103e75660005460005360200235"));
    }

    public ProgramInvokeMockImpl(boolean defaults) {

    }

    /*           ADDRESS op         */
    public DataWord getContractAddress() {
        return new DataWord(ownerAddress);
    }

    /*           BALANCE op         */
    public DataWord getBalance() {
        byte[] balance = Hex.decode("0DE0B6B3A7640000");
        return new DataWord(balance);
    }

    /*           ORIGIN op         */
    public DataWord getOriginAddress() {

        byte[] cowPrivKey = Hash.sha3("horse".getBytes());
        byte[] addr = ECKey.fromPrivate(cowPrivKey).getAddress();

        return new DataWord(addr);
    }

    /*           CALLER op         */
    public DataWord getCallerAddress() {

        byte[] cowPrivKey = Hash.sha3("monkey".getBytes());
        byte[] addr = ECKey.fromPrivate(cowPrivKey).getAddress();

        return new DataWord(addr);
    }

    /*           Cpu PRICE op       */
    public DataWord getMinCpuPrice() {
        byte[] minCpuPrice = Hex.decode("09184e72a000");
        return new DataWord(minCpuPrice);
    }

    /*          CALLVALUE op    */
    public DataWord getCallValue() {
        return getBalance();
    }

    @Override
    public DataWord getTokenValue() {
        return null;
    }

    @Override
    public DataWord getTokenId() {
        return null;
    }

    /****************.
     /***  msg data **.
     /***************.

     /*     CALLDATALOAD  op   */
    public DataWord getDataValue(DataWord indexData) {

        byte[] data = new byte[32];

        int index = indexData.value().intValue();
        int size = 32;

        if (msgData == null) {
            return new DataWord(data);
        }
        if (index > msgData.length) {
            return new DataWord(data);
        }
        if (index + 32 > msgData.length) {
            size = msgData.length - index;
        }

        System.arraycopy(msgData, index, data, 0, size);

        return new DataWord(data);
    }

    /*  CALLDATASIZE */
    public DataWord getDataSize() {

        if (msgData == null || msgData.length == 0) {
            return new DataWord(new byte[32]);
        }
        int size = msgData.length;
        return new DataWord(size);
    }

    /*  CALLDATACOPY */
    public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {

        int offset = offsetData.value().intValue();
        int length = lengthData.value().intValue();

        byte[] data = new byte[length];

        if (msgData == null) {
            return data;
        }
        if (offset > msgData.length) {
            return data;
        }
        if (offset + length > msgData.length) {
            length = msgData.length - offset;
        }

        System.arraycopy(msgData, offset, data, 0, length);

        return data;
    }

    @Override
    public DataWord getPrevHash() {
        byte[] prevHash = Hex
                .decode("961CB117ABA86D1E596854015A1483323F18883C2D745B0BC03E87F146D2BB1C");
        return new DataWord(prevHash);
    }

    @Override
    public DataWord getCoinbase() {
        byte[] coinBase = Hex.decode("E559DE5527492BCB42EC68D07DF0742A98EC3F1E");
        return new DataWord(coinBase);
    }

    @Override
    public DataWord getTimestamp() {
        long timestamp = 1401421348;
        return new DataWord(timestamp);
    }

    @Override
    public DataWord getNumber() {
        long number = 33;
        return new DataWord(number);
    }

    @Override
    public DataWord getDifficulty() {
        byte[] difficulty = Hex.decode("3ED290");
        return new DataWord(difficulty);
    }

    public void setOwnerAddress(byte[] ownerAddress) {
        this.ownerAddress = Arrays.clone(ownerAddress);
    }

    @Override
    public boolean isStaticCall() {
        return isStaticCall;
    }

    @Override
    public long getVmStartInUs() {
        return 0;
    }

    @Override
    public long getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(long customizedCpuLimit) {
        cpuLimit = customizedCpuLimit;
    }

    @Override
    public void setStaticCall() {
        isStaticCall = true;
    }

    @Override
    public BlockWrapper getBlockByNum(int index) {
        try {
            return deposit.getDbManager().getBlockByNum(index);
        } catch (StoreException e) {
            throw new IllegalOperationException("cannot find block num");
        }
    }

    @Override
    public boolean byTestingSuite() {
        return true;
    }

    @Override
    public Deposit getDeposit() {
        return this.deposit;
    }

    @Override
    public int getCallDeep() {
        return 0;
    }

    @Override
    public long getVmShouldEndInUs() {
        return 0;
        // modity later
    }
}
