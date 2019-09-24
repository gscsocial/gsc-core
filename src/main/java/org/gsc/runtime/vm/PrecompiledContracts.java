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

import static org.gsc.utils.ByteUtil.parseWord;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.gsc.core.operator.*;
import org.gsc.core.wrapper.TransactionWrapper;
import org.spongycastle.util.encoders.Hex;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.zksnark.BN128;
import org.gsc.crypto.zksnark.BN128Fp;
import org.gsc.crypto.zksnark.BN128G1;
import org.gsc.crypto.zksnark.BN128G2;
import org.gsc.crypto.zksnark.Fp;
import org.gsc.crypto.zksnark.PairingCheck;
import org.gsc.runtime.vm.program.Program;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.db.dbsource.Deposit;
import org.gsc.utils.BIUtil;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.Wallet;
import org.gsc.core.operator.ProposalApproveOperator;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.ProposalApproveContract;
import org.gsc.protos.Contract.ProposalCreateContract;
import org.gsc.protos.Contract.ProposalDeleteContract;
import org.gsc.protos.Contract.VoteWitnessContract;
import org.gsc.protos.Contract.WithdrawBalanceContract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.runtime.utils.MUtil;
import org.gsc.utils.ByteUtil;

/**
 * @author Roman Mandeleil
 * @since 09.01.2015
 */

@Slf4j(topic = "VM")
public class PrecompiledContracts {

    private static final ECRecover ecRecover = new ECRecover();
    private static final Sha256 sha256 = new Sha256();
    private static final Ripempd160 ripempd160 = new Ripempd160();
    private static final Identity identity = new Identity();
    private static final ModExp modExp = new ModExp();
    private static final BN128Addition altBN128Add = new BN128Addition();
    private static final BN128Multiplication altBN128Mul = new BN128Multiplication();
    private static final BN128Pairing altBN128Pairing = new BN128Pairing();
//  private static final VoteWitnessNative voteContract = new VoteWitnessNative();
//  private static final FreezeBalanceNative freezeBalance = new FreezeBalanceNative();
//  private static final UnfreezeBalanceNative unFreezeBalance = new UnfreezeBalanceNative();
//  private static final WithdrawBalanceNative withdrawBalance = new WithdrawBalanceNative();
//  private static final ProposalApproveNative proposalApprove = new ProposalApproveNative();
//  private static final ProposalCreateNative proposalCreate = new ProposalCreateNative();
//  private static final ProposalDeleteNative proposalDelete = new ProposalDeleteNative();
//  private static final ConvertFromGSCBytesAddressNative convertFromGSCBytesAddress = new ConvertFromGSCBytesAddressNative();
//  private static final ConvertFromGSCBase58AddressNative convertFromGSCBase58Address = new ConvertFromGSCBase58AddressNative();
//  private static final TransferAssetNative transferAsset = new TransferAssetNative();
//  private static final GetTransferAssetNative getTransferAssetAmount =  new GetTransferAssetNative();

    private static final ECKey addressCheckECKey = new ECKey();
    private static final String addressCheckECKeyAddress = Wallet
            .encode58Check(addressCheckECKey.getAddress());


    private static final DataWord ecRecoverAddr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000001");
    private static final DataWord sha256Addr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000002");
    private static final DataWord ripempd160Addr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000003");
    private static final DataWord identityAddr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000004");
    private static final DataWord modExpAddr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000005");
    private static final DataWord altBN128AddAddr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000006");
    private static final DataWord altBN128MulAddr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000007");
    private static final DataWord altBN128PairingAddr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000008");
//  private static final DataWord voteContractAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010001");
    //  private static final DataWord freezeBalanceAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010002");
//  private static final DataWord unFreezeBalanceAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010003");
//  private static final DataWord withdrawBalanceAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010004");
//  private static final DataWord proposalApproveAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010005");
//  private static final DataWord proposalCreateAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010006");
//  private static final DataWord proposalDeleteAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010007");
//  private static final DataWord convertFromGSCBytesAddressAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010008");
//  private static final DataWord convertFromGSCBase58AddressAddr = new DataWord(
//      "0000000000000000000000000000000000000000000000000000000000010009");
//  private static final DataWord transferAssetAddr = new DataWord(
//      "000000000000000000000000000000000000000000000000000000000001000a");
//  private static final DataWord getTransferAssetAmountAddr = new DataWord(
//      "000000000000000000000000000000000000000000000000000000000001000b");

    public static PrecompiledContract getContractForAddress(DataWord address) {

        if (address == null) {
            return identity;
        }
        if (address.equals(ecRecoverAddr)) {
            return ecRecover;
        }
        if (address.equals(sha256Addr)) {
            return sha256;
        }
        if (address.equals(ripempd160Addr)) {
            return ripempd160;
        }
        if (address.equals(identityAddr)) {
            return identity;
        }
//    if (address.equals(voteContractAddr)) {
//      return voteContract;
//    }
//    if (address.equals(freezeBalanceAddr)) {
//      return freezeBalance;
//    }
//    if (address.equals(unFreezeBalanceAddr)) {
//      return unFreezeBalance;
//    }
//    if (address.equals(withdrawBalanceAddr)) {
//      return withdrawBalance;
//    }
//    if (address.equals(proposalApproveAddr)) {
//      return proposalApprove;
//    }
//    if (address.equals(proposalCreateAddr)) {
//      return proposalCreate;
//    }
//    if (address.equals(proposalDeleteAddr)) {
//      return proposalDelete;
//    }
//    if (address.equals(convertFromGSCBytesAddressAddr)) {
//      return convertFromGSCBytesAddress;
//    }
//    if (address.equals(convertFromGSCBase58AddressAddr)) {
//      return convertFromGSCBase58Address;
//    }
//    if (address.equals(transferAssetAddr)) {
//      return transferAsset;
//    }
//    if (address.equals(getTransferAssetAmountAddr)) {
//      return getTransferAssetAmount;
//    }

        // Byzantium precompiles
        if (address.equals(modExpAddr)) {
            return modExp;
        }
        if (address.equals(altBN128AddAddr)) {
            return altBN128Add;
        }
        if (address.equals(altBN128MulAddr)) {
            return altBN128Mul;
        }
        if (address.equals(altBN128PairingAddr)) {
            return altBN128Pairing;
        }
        return null;
    }

    private static byte[] encodeRes(byte[] w1, byte[] w2) {

        byte[] res = new byte[64];

        w1 = ByteUtil.stripLeadingZeroes(w1);
        w2 = ByteUtil.stripLeadingZeroes(w2);

        System.arraycopy(w1, 0, res, 32 - w1.length, w1.length);
        System.arraycopy(w2, 0, res, 64 - w2.length, w2.length);

        return res;
    }

    public static abstract class PrecompiledContract {

        public abstract long getCpuForData(byte[] data);

        public abstract Pair<Boolean, byte[]> execute(byte[] data);

        private byte[] callerAddress;

        public void setCallerAddress(byte[] callerAddress) {
            this.callerAddress = callerAddress.clone();
        }

        public void setDeposit(Deposit deposit) {
            this.deposit = deposit;
        }

        public void setResult(ProgramResult result) {
            this.result = result;
        }

        private Deposit deposit;

        private ProgramResult result;

        public byte[] getCallerAddress() {
            return callerAddress.clone();
        }

        public Deposit getDeposit() {
            return deposit;
        }

        public ProgramResult getResult() {
            return result;
        }

        @Setter
        @Getter
        private boolean isStaticCall;
    }

    public static class Identity extends PrecompiledContract {

        public Identity() {
        }

        @Override
        public long getCpuForData(byte[] data) {

            // cpu charge for the execution:
            // minimum 1 and additional 1 for each 32 bytes word (round  up)
            if (data == null) {
                return 15;
            }
            return 15L + (data.length + 31) / 32 * 3;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {
            return Pair.of(true, data);
        }
    }

    public static class Sha256 extends PrecompiledContract {


        @Override
        public long getCpuForData(byte[] data) {

            // cpu charge for the execution:
            // minimum 50 and additional 50 for each 32 bytes word (round  up)
            if (data == null) {
                return 60;
            }
            return 60L + (data.length + 31) / 32 * 12;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (data == null) {
                return Pair.of(true, Sha256Hash.hash(ByteUtil.EMPTY_BYTE_ARRAY));
            }
            return Pair.of(true, Sha256Hash.hash(data));
        }
    }


    public static class Ripempd160 extends PrecompiledContract {


        @Override
        public long getCpuForData(byte[] data) {

            // TODO #POC9 Replace magic numbers with constants
            // cpu charge for the execution:
            // minimum 50 and additional 50 for each 32 bytes word (round  up)
            if (data == null) {
                return 600;
            }
            return 600L + (data.length + 31) / 32 * 120;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {
            byte[] target = new byte[20];
            if (data == null) {
                data = ByteUtil.EMPTY_BYTE_ARRAY;
            }
            byte[] orig = Sha256Hash.hash(data);
            System.arraycopy(orig, 0, target, 0, 20);
            return Pair.of(true, Sha256Hash.hash(target));
        }
    }


    public static class ECRecover extends PrecompiledContract {

        @Override
        public long getCpuForData(byte[] data) {
            return 3000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            byte[] h = new byte[32];
            byte[] v = new byte[32];
            byte[] r = new byte[32];
            byte[] s = new byte[32];

            DataWord out = null;

            try {
                System.arraycopy(data, 0, h, 0, 32);
                System.arraycopy(data, 32, v, 0, 32);
                System.arraycopy(data, 64, r, 0, 32);

                int sLength = data.length < 128 ? data.length - 96 : 32;
                System.arraycopy(data, 96, s, 0, sLength);

                ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v[31]);
                if (validateV(v) && signature.validateComponents()) {
                    out = new DataWord(ECKey.signatureToAddress(h, signature));
                }
            } catch (Throwable any) {
            }

            if (out == null) {
                return Pair.of(true, ByteUtil.EMPTY_BYTE_ARRAY);
            } else {
                return Pair.of(true, out.getData());
            }
        }

        private static boolean validateV(byte[] v) {
            for (int i = 0; i < v.length - 1; i++) {
                if (v[i] != 0) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Computes modular exponentiation on big numbers
     * <p>
     * format of data[] array: [length_of_BASE] [length_of_EXPONENT] [length_of_MODULUS] [BASE]
     * [EXPONENT] [MODULUS] where every length is a 32-byte left-padded integer representing the
     * number of bytes. Call data is assumed to be infinitely right-padded with zero bytes.
     * <p>
     * Returns an output as a byte array with the same length as the modulus
     */
    public static class ModExp extends PrecompiledContract {

        private static final BigInteger GQUAD_DIVISOR = BigInteger.valueOf(20);

        private static final int ARGS_OFFSET = 32 * 3; // addresses length part

        @Override
        public long getCpuForData(byte[] data) {

            if (data == null) {
                data = ByteUtil.EMPTY_BYTE_ARRAY;
            }

            int baseLen = parseLen(data, 0);
            int expLen = parseLen(data, 1);
            int modLen = parseLen(data, 2);

            byte[] expHighBytes = ByteUtil.parseBytes(data, BIUtil.addSafely(ARGS_OFFSET, baseLen), Math.min(expLen, 32));

            long multComplexity = getMultComplexity(Math.max(baseLen, modLen));
            long adjExpLen = getAdjustedExponentLength(expHighBytes, expLen);

            // use big numbers to stay safe in case of overflow
            BigInteger cpu = BigInteger.valueOf(multComplexity)
                    .multiply(BigInteger.valueOf(Math.max(adjExpLen, 1)))
                    .divide(GQUAD_DIVISOR);

            return BIUtil.isLessThan(cpu, BigInteger.valueOf(Long.MAX_VALUE)) ? cpu.longValueExact()
                    : Long.MAX_VALUE;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (data == null) {
                return Pair.of(true, ByteUtil.EMPTY_BYTE_ARRAY);
            }

            int baseLen = parseLen(data, 0);
            int expLen = parseLen(data, 1);
            int modLen = parseLen(data, 2);

            BigInteger base = parseArg(data, ARGS_OFFSET, baseLen);
            BigInteger exp = parseArg(data, BIUtil.addSafely(ARGS_OFFSET, baseLen), expLen);
            BigInteger mod = parseArg(data, BIUtil.addSafely(BIUtil.addSafely(ARGS_OFFSET, baseLen), expLen), modLen);

            // check if modulus is zero
            if (BIUtil.isZero(mod)) {
                return Pair.of(true, ByteUtil.EMPTY_BYTE_ARRAY);
            }

            byte[] res = ByteUtil.stripLeadingZeroes(base.modPow(exp, mod).toByteArray());

            // adjust result to the same length as the modulus has
            if (res.length < modLen) {

                byte[] adjRes = new byte[modLen];
                System.arraycopy(res, 0, adjRes, modLen - res.length, res.length);

                return Pair.of(true, adjRes);

            } else {
                return Pair.of(true, res);
            }
        }

        private long getMultComplexity(long x) {

            long x2 = x * x;

            if (x <= 64) {
                return x2;
            }
            if (x <= 1024) {
                return x2 / 4 + 96 * x - 3072;
            }

            return x2 / 16 + 480 * x - 199680;
        }

        private long getAdjustedExponentLength(byte[] expHighBytes, long expLen) {

            int leadingZeros = ByteUtil.numberOfLeadingZeros(expHighBytes);
            int highestBit = 8 * expHighBytes.length - leadingZeros;

            // set index basement to zero
            if (highestBit > 0) {
                highestBit--;
            }

            if (expLen <= 32) {
                return highestBit;
            } else {
                return 8 * (expLen - 32) + highestBit;
            }
        }

        private int parseLen(byte[] data, int idx) {
            byte[] bytes = ByteUtil.parseBytes(data, 32 * idx, 32);
            return new DataWord(bytes).intValueSafe();
        }

        private BigInteger parseArg(byte[] data, int offset, int len) {
            byte[] bytes = ByteUtil.parseBytes(data, offset, len);
            return ByteUtil.bytesToBigInteger(bytes);
        }
    }

    /**
     * Computes point addition on Barreto–Naehrig curve. See {@link BN128Fp} for details<br/> <br/>
     * <p>
     * input data[]:<br/> two points encoded as (x, y), where x and y are 32-byte left-padded
     * integers,<br/> if input is shorter than expected, it's assumed to be right-padded with zero
     * bytes<br/> <br/>
     * <p>
     * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
     * integers<br/>
     */
    public static class BN128Addition extends PrecompiledContract {

        @Override
        public long getCpuForData(byte[] data) {
            return 500;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (data == null) {
                data = ByteUtil.EMPTY_BYTE_ARRAY;
            }

            byte[] x1 = ByteUtil.parseWord(data, 0);
            byte[] y1 = ByteUtil.parseWord(data, 1);

            byte[] x2 = ByteUtil.parseWord(data, 2);
            byte[] y2 = ByteUtil.parseWord(data, 3);

            BN128<Fp> p1 = BN128Fp.create(x1, y1);
            if (p1 == null) {
                return Pair.of(false, ByteUtil.EMPTY_BYTE_ARRAY);
            }

            BN128<Fp> p2 = BN128Fp.create(x2, y2);
            if (p2 == null) {
                return Pair.of(false, ByteUtil.EMPTY_BYTE_ARRAY);
            }

            BN128<Fp> res = p1.add(p2).toEthNotation();

            return Pair.of(true, encodeRes(res.x().bytes(), res.y().bytes()));
        }
    }

    /**
     * Computes multiplication of scalar value on a point belonging to Barreto–Naehrig curve. See
     * {@link BN128Fp} for details<br/> <br/>
     * <p>
     * input data[]:<br/> point encoded as (x, y) is followed by scalar s, where x, y and s are
     * 32-byte left-padded integers,<br/> if input is shorter than expected, it's assumed to be
     * right-padded with zero bytes<br/> <br/>
     * <p>
     * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
     * integers<br/>
     */
    public static class BN128Multiplication extends PrecompiledContract {

        @Override
        public long getCpuForData(byte[] data) {
            return 40000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (data == null) {
                data = ByteUtil.EMPTY_BYTE_ARRAY;
            }

            byte[] x = ByteUtil.parseWord(data, 0);
            byte[] y = ByteUtil.parseWord(data, 1);

            byte[] s = ByteUtil.parseWord(data, 2);

            BN128<Fp> p = BN128Fp.create(x, y);
            if (p == null) {
                return Pair.of(false, ByteUtil.EMPTY_BYTE_ARRAY);
            }

            BN128<Fp> res = p.mul(BIUtil.toBI(s)).toEthNotation();

            return Pair.of(true, encodeRes(res.x().bytes(), res.y().bytes()));
        }
    }

    /**
     * Computes pairing check. <br/> See {@link PairingCheck} for details.<br/> <br/>
     * <p>
     * Input data[]: <br/> an array of points (a1, b1, ... , ak, bk), <br/> where "ai" is a point of
     * {@link BN128Fp} curve and encoded as two 32-byte left-padded integers (x; y) <br/> "bi" is a
     * point of {@link BN128G2} curve and encoded as four 32-byte left-padded integers {@code (ai + b;
     * ci + d)}, each coordinate of the point is a big-endian {@link } number, so {@code b} precedes
     * {@code a} in the encoding: {@code (b, a; d, c)} <br/> thus each pair (ai, bi) has 192 bytes
     * length, if 192 is not a multiple of {@code data.length} then execution fails <br/> the number
     * of pairs is derived from input length by dividing it by 192 (the length of a pair) <br/> <br/>
     * <p>
     * output: <br/> pairing product which is either 0 or 1, encoded as 32-byte left-padded integer
     * <br/>
     */
    public static class BN128Pairing extends PrecompiledContract {

        private static final int PAIR_SIZE = 192;

        @Override
        public long getCpuForData(byte[] data) {

            if (data == null) {
                return 100000;
            }

            return 80000L * (data.length / PAIR_SIZE) + 100000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (data == null) {
                data = ByteUtil.EMPTY_BYTE_ARRAY;
            }

            // fail if input len is not a multiple of PAIR_SIZE
            if (data.length % PAIR_SIZE > 0) {
                return Pair.of(false, ByteUtil.EMPTY_BYTE_ARRAY);
            }

            PairingCheck check = PairingCheck.create();

            // iterating over all pairs
            for (int offset = 0; offset < data.length; offset += PAIR_SIZE) {

                Pair<BN128G1, BN128G2> pair = decodePair(data, offset);

                // fail if decoding has failed
                if (pair == null) {
                    return Pair.of(false, ByteUtil.EMPTY_BYTE_ARRAY);
                }

                check.addPair(pair.getLeft(), pair.getRight());
            }

            check.run();
            int result = check.result();

            return Pair.of(true, new DataWord(result).getData());
        }

        private Pair<BN128G1, BN128G2> decodePair(byte[] in, int offset) {

            byte[] x = ByteUtil.parseWord(in, offset, 0);
            byte[] y = ByteUtil.parseWord(in, offset, 1);

            BN128G1 p1 = BN128G1.create(x, y);

            // fail if point is invalid
            if (p1 == null) {
                return null;
            }

            // (b, a)
            byte[] b = ByteUtil.parseWord(in, offset, 2);
            byte[] a = ByteUtil.parseWord(in, offset, 3);

            // (d, c)
            byte[] d = ByteUtil.parseWord(in, offset, 4);
            byte[] c = ByteUtil.parseWord(in, offset, 5);

            BN128G2 p2 = BN128G2.create(a, b, c, d);

            // fail if point is invalid
            if (p2 == null) {
                return null;
            }

            return Pair.of(p1, p2);
        }
    }

    /**
     * Native function for voting witness. <br/> <br/>
     * <p>
     * Input data[]: <br/> witness address, voteCount
     * <p>
     * output: <br/> voteCount <br/>
     */
    public static class VoteWitnessNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (isStaticCall()) {
                return Pair.of(true, new DataWord(0).getData());
            }
            if (data == null || data.length != 2 * DataWord.WORD_SIZE) {
                return Pair.of(false, new DataWord(0).getData());
            }

            byte[] witnessAddress = new byte[32];
            System.arraycopy(data, 0, witnessAddress, 0, 32);
            byte[] value = new byte[8];
            System.arraycopy(data, 32 + 16 + 8, value, 0, 8);

            Contract.VoteWitnessContract.Builder builder = Contract.VoteWitnessContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(getCallerAddress()));
            long count = Longs.fromByteArray(value);
            Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote
                    .newBuilder();
            byte[] witnessAddress20 = new byte[20];
            System.arraycopy(witnessAddress, 12, witnessAddress20, 0, 20);
            voteBuilder.setVoteAddress(ByteString.copyFrom(MUtil.convertToGSCAddress(witnessAddress20)));
            voteBuilder.setVoteCount(count);
            builder.addVotes(voteBuilder.build());
            VoteWitnessContract contract = builder.build();

            final List<Operator> operatorList = OperatorFactory
                    .createOperator(new TransactionWrapper(contract), getDeposit().getDbManager());
            VoteWitnessOperator voteWitnessOperator;
            try {
                if (Objects.isNull(operatorList) || operatorList.isEmpty()) {
                    throw new ContractExeException(
                            "can't initiate VoteWitnessOperator for precompiled vm method");
                } else {
                    Optional voteOptional = operatorList.stream().findFirst();
                    if (voteOptional.isPresent()) {
                        voteWitnessOperator = (VoteWitnessOperator) voteOptional.get();
                    } else {
                        throw new ContractExeException(
                                "can't initiate VoteWitnessOperator for precompiled vm method");
                    }
                }
                voteWitnessOperator.setDeposit(getDeposit());
                voteWitnessOperator.validate();
                voteWitnessOperator.execute(getResult().getRet());
            } catch (ContractExeException e) {
                logger.debug("ContractExeException when calling voteWitness in vm");
                logger.debug("ContractExeException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractExecuteException(e));
                return Pair.of(false, new DataWord(0).getData());
            } catch (ContractValidateException e) {
                logger.debug("ContractValidateException when calling voteWitness in vm");
                logger.debug("ContractValidateException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractValidateException(e));
                return Pair.of(false, new DataWord(0).getData());
            }
            return Pair.of(true, new DataWord(1).getData());
        }
    }

    /**
     * Native function to freeze caller account balance. <br/> <br/>
     * <p>
     * Input data[]: <br/> freeze balance amount, freeze duration
     * <p>
     * output: <br/> isSuccess <br/>
     */
    public static class FreezeBalanceNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            // if (data == null) {
            //   data = EMPTY_BYTE_ARRAY;
            // }

//      byte[] frozenBalance = new byte[32];
//      System.arraycopy(data, 0, frozenBalance, 0, 32);
//      byte[] frozenDuration = new byte[32];
//      System.arraycopy(data, 32, frozenDuration, 0, 32);
//
//      if (getDeposit().getDbManager().getAccountStore().get(getCallerAddress()).getType()
//          == AccountType.Contract) {
//        logger.debug("caller can't be a contract");
//        // TODO: or exception here.
//        return Pair.of(false, null);
//      }
//
//      Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
//      ByteString byteAddress = ByteString.copyFrom(getCallerAddress());
//      builder.setOwnerAddress(byteAddress).setFrozenBalance(ByteArray.toLong(frozenBalance))
//          .setFrozenDuration(ByteArray.toLong(frozenDuration));
//      FreezeBalanceContract contract = builder.build();
//
//      TransactionWrapper trx = new TransactionWrapper(contract, ContractType.FreezeBalanceContract);
//
//      final List<Operator> operatorList = OperatorFactory
//          .createOperator(trx, getDeposit().getDbManager());
//      try {
//        operatorList.get(0).validate();
//        operatorList.get(0).execute(getResult().getRet());
//        getDeposit()
//            .syncCacheFromAccountStore(ByteString.copyFrom(getCallerAddress()).toByteArray());
//      } catch (ContractExeException e) {
//        logger.debug("ContractExeException when calling freezeBalance in vm");
//        logger.debug("ContractExeException: {}", e.getMessage());
//        return null;
//      } catch (ContractValidateException e) {
//        logger.debug("ContractValidateException when calling freezeBalance in vm");
//        logger.debug("ContractValidateException: {}", e.getMessage());
//        return null;
//      }
            return Pair.of(true, new DataWord(1).getData());
        }
    }

    /**
     * Native function to unfreeze caller account balance. <br/> <br/>
     * <p>
     * Input data[]: <br/> null
     * <p>
     * output: <br/> isSuccess <br/>
     */
    public static class UnfreezeBalanceNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            // if (data == null) {
            //   data = EMPTY_BYTE_ARRAY;
            // }

//      if (getDeposit().getDbManager().getAccountStore().get(getCallerAddress()).getType()
//          == AccountType.Contract) {
//        logger.debug("caller can't be a contract");
//        // TODO: or exception here.
//        return Pair.of(false, null);
//      }
//
//      Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
//          .newBuilder();
//      ByteString byteAddress = ByteString.copyFrom(getCallerAddress());
//      builder.setOwnerAddress(byteAddress);
//      UnfreezeBalanceContract contract = builder.build();
//
//      TransactionWrapper trx = new TransactionWrapper(contract,
//          ContractType.UnfreezeBalanceContract);
//
//      final List<Operator> operatorList = OperatorFactory
//          .createOperator(trx, getDeposit().getDbManager());
//      try {
//        operatorList.get(0).validate();
//        operatorList.get(0).execute(getResult().getRet());
//        getDeposit()
//            .syncCacheFromAccountStore(ByteString.copyFrom(getCallerAddress()).toByteArray());
//        getDeposit().syncCacheFromVotesStore(ByteString.copyFrom(getCallerAddress()).toByteArray());
//      } catch (ContractExeException e) {
//        logger.debug("ContractExeException when calling unfreezeBalance in vm");
//        logger.debug("ContractExeException: {}", e.getMessage());
//        return null;
//      } catch (ContractValidateException e) {
//        logger.debug("ContractValidateException when calling unfreezeBalance in vm");
//        logger.debug("ContractValidateException: {}", e.getMessage());
//        return null;
//      }
            return Pair.of(true, new DataWord(1).getData());
        }
    }

    /**
     * Native function for witnesses to withdraw their reward . <br/> <br/>
     * <p>
     * Input data[]: <br/> null
     * <p>
     * output: <br/> isSuccess <br/>
     */
    public static class WithdrawBalanceNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (isStaticCall()) {
                return Pair.of(true, new DataWord(0).getData());
            }

            Contract.WithdrawBalanceContract.Builder builder = Contract.WithdrawBalanceContract
                    .newBuilder();
            ByteString byteAddress = ByteString.copyFrom(getCallerAddress());
            builder.setOwnerAddress(byteAddress);
            WithdrawBalanceContract contract = builder.build();

            TransactionWrapper trx = new TransactionWrapper(contract,
                    ContractType.WithdrawBalanceContract);

            final List<Operator> operatorList = OperatorFactory
                    .createOperator(trx, getDeposit().getDbManager());
            WithdrawBalanceOperator withdrawBalanceOperator;
            try {
                if (Objects.isNull(operatorList) || operatorList.isEmpty()) {
                    throw new ContractExeException(
                            "can't initiate WithdrawBalanceOperator for precompiled vm method");
                } else {
                    Optional withdrawOptional = operatorList.stream().findFirst();
                    if (withdrawOptional.isPresent()) {
                        withdrawBalanceOperator = (WithdrawBalanceOperator) withdrawOptional.get();
                    } else {
                        throw new ContractExeException(
                                "can't initiate WithdrawBalanceOperator for precompiled vm method");
                    }
                }

                withdrawBalanceOperator.setDeposit(getDeposit());
                withdrawBalanceOperator.validate();
                withdrawBalanceOperator.execute(getResult().getRet());
            } catch (ContractExeException e) {
                logger.debug("ContractExeException when calling withdrawBalanceNative in vm");
                logger.debug("ContractExeException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractExecuteException(e));
                return Pair.of(false, new DataWord(0).getData());
            } catch (ContractValidateException e) {
                logger.debug("ContractValidateException when calling withdrawBalanceNative in vm");
                logger.debug("ContractValidateException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractValidateException(e));
                return Pair.of(false, new DataWord(0).getData());
            }
            return Pair.of(true, new DataWord(1).getData());
        }
    }

    /**
     * Native function for witnesses to approve a proposal . <br/> <br/>
     * <p>
     * Input data[]: <br/> proposalId, isApprove
     * <p>
     * output: <br/> isSuccess <br/>
     */
    public static class ProposalApproveNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (isStaticCall()) {
                return Pair.of(true, new DataWord(0).getData());
            }

            if (data == null || data.length != 2 * DataWord.WORD_SIZE) {
                return Pair.of(false, new DataWord(0).getData());
            }

            byte[] proposalId = new byte[32];
            System.arraycopy(data, 0, proposalId, 0, 32);
            byte[] isAddApproval = new byte[32];
            System.arraycopy(data, 32, isAddApproval, 0, 32);

            Contract.ProposalApproveContract.Builder builder = Contract.ProposalApproveContract
                    .newBuilder();
            ByteString byteAddress = ByteString.copyFrom(getCallerAddress());
            builder.setOwnerAddress(byteAddress);
            builder.setProposalId(ByteArray.toLong(proposalId));
            builder.setIsAddApproval(ByteArray.toInt(isAddApproval) == 1);
            ProposalApproveContract contract = builder.build();

            TransactionWrapper trx = new TransactionWrapper(contract,
                    ContractType.ProposalApproveContract);

            final List<Operator> operatorList = OperatorFactory
                    .createOperator(trx, getDeposit().getDbManager());
            ProposalApproveOperator proposalApproveOperator;
            try {
                if (Objects.isNull(operatorList) || operatorList.isEmpty()) {
                    throw new ContractExeException(
                            "can't initiate ProposalApproveOperator for precompiled vm method");
                } else {
                    Optional proposalApproveOptional = operatorList.stream().findFirst();
                    if (proposalApproveOptional.isPresent()) {
                        proposalApproveOperator = (ProposalApproveOperator) proposalApproveOptional.get();
                    } else {
                        throw new ContractExeException(
                                "can't initiate ProposalApproveOperator for precompiled vm method");
                    }
                }
                proposalApproveOperator.setDeposit(getDeposit());
                proposalApproveOperator.validate();
                proposalApproveOperator.execute(getResult().getRet());
            } catch (ContractExeException e) {
                logger.debug("ContractExeException when calling proposalApproveNative in vm");
                logger.debug("ContractExeException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractExecuteException(e));
                return Pair.of(false, new DataWord(0).getData());
            } catch (ContractValidateException e) {
                logger.debug("ContractValidateException when calling proposalApproveNative in vm");
                logger.debug("ContractValidateException: {}", e.getMessage());
                return Pair.of(false, new DataWord(0).getData());
            }
            return Pair.of(true, new DataWord(1).getData());
        }
    }

    /**
     * Native function for a witness to create a proposal. <br/> <br/>
     * <p>
     * Input data[]: <br/> an array of key,value (key1, value1, key2, value2... , keyn, valuen),
     * <br/>
     * <p>
     * Output: <br/> proposalId <br/>
     */
    public static class ProposalCreateNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (isStaticCall()) {
                return Pair.of(true, new DataWord(0).getData());
            }

            if (data == null || data.length == 0 || (data.length % (2 * DataWord.WORD_SIZE)
                    != 0)) {
                return Pair.of(false, new DataWord(0).getData());
            }

            HashMap<Long, Long> parametersMap = new HashMap<>();
            int index = 0;
            while (index <= data.length - 1) {
                byte[] id = new byte[32];
                System.arraycopy(data, index, id, 0, 32);
                byte[] value = new byte[32];
                System.arraycopy(data, 32 + index, value, 0, 32);
                parametersMap.put(ByteArray.toLong(id), ByteArray.toLong(value));
                index += 64;
            }

            Contract.ProposalCreateContract.Builder builder = Contract.ProposalCreateContract
                    .newBuilder();
            ByteString byteAddress = ByteString.copyFrom(getCallerAddress());
            builder.setOwnerAddress(byteAddress);
            builder.putAllParameters(parametersMap);

            ProposalCreateContract contract = builder.build();

            long id = 0;
            TransactionWrapper trx = new TransactionWrapper(contract,
                    ContractType.ProposalCreateContract);

            final List<Operator> operatorList = OperatorFactory
                    .createOperator(trx, getDeposit().getDbManager());
            ProposalCreateOperator proposalCreateOperator;
            try {
                if (Objects.isNull(operatorList) || operatorList.isEmpty()) {
                    throw new ContractExeException(
                            "can't initiate ProposalCreateOperator for precompiled vm method");
                } else {
                    Optional proposalCreateOptional = operatorList.stream().findFirst();
                    if (proposalCreateOptional.isPresent()) {
                        proposalCreateOperator = (ProposalCreateOperator) proposalCreateOptional.get();
                    } else {
                        throw new ContractExeException(
                                "can't initiate ProposalCreateOperator for precompiled vm method");
                    }
                }
                proposalCreateOperator.setDeposit(getDeposit());
                proposalCreateOperator.validate();
                proposalCreateOperator.execute(getResult().getRet());
                id = getDeposit().getLatestProposalNum();
            } catch (ContractExeException e) {
                logger.debug("ContractExeException when calling proposalCreateNative in vm");
                logger.debug("ContractExeException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractExecuteException(e));
                return Pair.of(false, new DataWord(0).getData());
            } catch (ContractValidateException e) {
                logger.debug("ContractValidateException when calling proposalCreateNative in vm");
                logger.debug("ContractValidateException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractValidateException(e));
                return Pair.of(false, new DataWord(0).getData());
            }
            return Pair.of(true, new DataWord(id).getData());
        }
    }

    /**
     * Native function for a witness to delete a proposal. <br/> <br/>
     * <p>
     * Input data[]: <br/> ProposalId <br/>
     * <p>
     * Output: <br/> isSuccess <br/>
     */
    public static class ProposalDeleteNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (isStaticCall()) {
                return Pair.of(true, new DataWord(0).getData());
            }

            if (data == null || data.length != DataWord.WORD_SIZE) {
                return Pair.of(false, new DataWord(0).getData());
            }
            Contract.ProposalDeleteContract.Builder builder = Contract.ProposalDeleteContract
                    .newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(getCallerAddress()));
            builder.setProposalId(ByteArray.toLong(data));

            ProposalDeleteContract contract = builder.build();

            TransactionWrapper trx = new TransactionWrapper(contract,
                    ContractType.ProposalDeleteContract);

            final List<Operator> operatorList = OperatorFactory
                    .createOperator(trx, getDeposit().getDbManager());
            ProposalDeleteOperator proposalDeleteOperator;
            try {
                if (Objects.isNull(operatorList) || operatorList.isEmpty()) {
                    throw new ContractExeException(
                            "can't initiate ProposalDeleteOperator for precompiled vm method");
                } else {
                    Optional proposalDeleteOptional = operatorList.stream().findFirst();
                    if (proposalDeleteOptional.isPresent()) {
                        proposalDeleteOperator = (ProposalDeleteOperator) proposalDeleteOptional.get();
                    } else {
                        throw new ContractExeException(
                                "can't initiate ProposalDeleteOperator for precompiled vm method");
                    }
                }
                proposalDeleteOperator.setDeposit(getDeposit());
                proposalDeleteOperator.validate();
                proposalDeleteOperator.execute(getResult().getRet());
            } catch (ContractExeException e) {
                logger.debug("ContractExeException when calling proposalDeleteContract in vm");
                logger.debug("ContractExeException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractExecuteException(e));
                return Pair.of(false, new DataWord(0).getData());
            } catch (ContractValidateException e) {
                logger.debug("ContractValidateException when calling proposalDeleteContract in vm");
                logger.debug("ContractValidateException: {}", e.getMessage());
                this.getResult().setException(Program.Exception.contractValidateException(e));
                return Pair.of(false, new DataWord(0).getData());
            }
            return Pair.of(true, new DataWord(1).getData());
        }
    }

    /**
     * Native function for converting bytes32 gsc address to solidity address type value. <br/>
     * <br/>
     * <p>
     * Input data[]: <br/> bytes32 gsc address <br/>
     * <p>
     * Output: <br/> solidity address <br/>
     */
    public static class ConvertFromGSCBytesAddressNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (data == null || data.length != DataWord.WORD_SIZE) {
                return Pair.of(false, new DataWord(0).getData());
            }
            DataWord address = new DataWord(data);
            return Pair.of(true, new DataWord(address.getLast20Bytes()).getData());
        }
    }

    /**
     * Native function for converting Base58String gsc address to solidity address type value. <br/>
     * <br/>
     * <p>
     * Input data[]: <br/> Base58String gsc address <br/>
     * <p>
     * Output: <br/> solidity address <br/>
     */
    public static class ConvertFromGSCBase58AddressNative extends PrecompiledContract {

        @Override
        // TODO: Please re-implement this function after GSC cost is well designed.
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            int checklength = addressCheckECKeyAddress.length();
            if (data == null || data.length != checklength) {
                return Pair.of(false, new DataWord(0).getData());
            }

            String addressBase58 = new String(data);
            byte[] resultBytes = Wallet.decodeFromBase58Check(addressBase58);
            String hexString = Hex.toHexString(resultBytes);

            return Pair.of(true, new DataWord(new DataWord(hexString).getLast20Bytes()).getData());
        }
    }

    /**
     * Native function for transferring Asset to another account. <br/> <br/>
     * <p>
     * Input data[]: <br/> toAddress, amount, assetName <br/>
     * <p>
     * Output: <br/> transfer asset operation success or not <br/>
     */
    public static class TransferAssetNative extends PrecompiledContract {

        @Override
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

//      if (isStaticCall()){
//        return Pair.of(true, new DataWord(0).getData());
//      }
//
//      if (data == null || (data.length <= DataWord.WORD_SIZE * 2 || data.length > DataWord.WORD_SIZE * 3)) {
//        return Pair.of(false, new DataWord(0).getData());
//      }
//
//      byte[] toAddress = new byte[32];
//      System.arraycopy(data, 0, toAddress, 0, 32);
//      byte[] amount = new byte[8];
//      System.arraycopy(data, 32 + 16 + 8, amount, 0, 8);
//      // we already have a restrict for token name length, no more than 32 bytes. don't need to check again
//      byte[] name = new byte[32];
//      System.arraycopy(data, 64, name, 0, data.length-64);
//      int length =name.length;
//      while(length>0 && name[length -1] ==0){
//        length--;
//      }
//      name = ByteArray.subArray(name,0,length);
//      Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract
//          .newBuilder();
//      builder.setOwnerAddress(ByteString.copyFrom(getCallerAddress()));
//      builder.setToAddress(ByteString.copyFrom(convertToGSCAddress(new DataWord(toAddress).getLast20Bytes())));
//      builder.setAmount(Longs.fromByteArray(amount));
//      builder.setAssetName(ByteString.copyFrom(name));
//
//
//      TransferAssetContract contract = builder.build();
//
//      TransactionWrapper trx = new TransactionWrapper(contract,
//          ContractType.TransferAssetContract);
//
//      final List<Operator> operatorList = OperatorFactory
//          .createOperator(trx, getDeposit().getDbManager());
//      try {
//        operatorList.get(0).validate();
//        operatorList.get(0).execute(getResult().getRet());
//      } catch (ContractExeException e) {
//        logger.debug("ContractExeException when calling transferAssetContract in vm");
//        logger.debug("ContractExeException: {}", e.getMessage());
//        this.getResult().setException(new Program.Exception().contractExecuteException(e));
//        return Pair.of(false, new DataWord(0).getData());
//      } catch (ContractValidateException e) {
//        logger.debug("ContractValidateException when calling transferAssetContract in vm");
//        logger.debug("ContractValidateException: {}", e.getMessage());
//        this.getResult().setException(new Program.Exception().contractValidateException(e));
//        return Pair.of(false, new DataWord(0).getData());
//      }
            return Pair.of(true, new DataWord(1).getData());
        }
    }


    /**
     * Native function for check Asset balance basing on targetAddress and Asset name. <br/> <br/>
     * <p>
     * Input data[]: <br/> address targetAddress, byte[] assetName <br/>
     * <p>
     * Output: <br/> balance <br/>
     */
    public static class GetTransferAssetNative extends PrecompiledContract {

        @Override
        public long getCpuForData(byte[] data) {
            return 200;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {

            if (data == null || data.length != DataWord.WORD_SIZE * 2) {
                return Pair.of(false, new DataWord(0).getData());
            }

            byte[] targetAddress = new byte[32];
            System.arraycopy(data, 0, targetAddress, 0, 32);
            // we already have a restrict for token name length, no more than 32 bytes. don't need to check again
            byte[] name = new byte[32];
            System.arraycopy(data, 32, name, 0, 32);
            int length = name.length;
            while (length > 0 && name[length - 1] == 0) {
                length--;
            }
            name = ByteArray.subArray(name, 0, length);

            long assetBalance = this.getDeposit().
                    getAccount(MUtil.convertToGSCAddress(new DataWord(targetAddress).getLast20Bytes())).
                    getAssetMap().get(ByteArray.toStr(name));

            return Pair.of(true, new DataWord(Longs.toByteArray(assetBalance)).getData());
        }
    }
}
