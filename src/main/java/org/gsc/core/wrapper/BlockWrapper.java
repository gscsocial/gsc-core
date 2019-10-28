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



package org.gsc.core.wrapper;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.ECKey.ECDSASignature;
import org.gsc.utils.ByteUtil;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.Time;
import org.gsc.core.wrapper.utils.MerkleTree;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.db.Manager;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.BlockHeader;
import org.gsc.protos.Protocol.Transaction;

@Slf4j(topic = "wrapper")
public class BlockWrapper implements ProtoWrapper<Block> {

    @Getter
    @Setter
    private TransactionRetWrapper result;

    public static class BlockId extends Sha256Hash {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || (getClass() != o.getClass() && !(o instanceof Sha256Hash))) {
                return false;
            }
            return Arrays.equals(getBytes(), ((Sha256Hash) o).getBytes());
        }

        public String getString() {
            return "Num:" + num + ",ID:" + super.toString();
        }

        @Override
        public String toString() {
            return super.toString();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public int compareTo(Sha256Hash other) {
            if (other.getClass().equals(BlockId.class)) {
                long otherNum = ((BlockId) other).getNum();
                return Long.compare(num, otherNum);
            }
            return super.compareTo(other);
        }

        private long num;

        public BlockId() {
            super(Sha256Hash.ZERO_HASH.getBytes());
            num = 0;
        }

        public BlockId(Sha256Hash blockId) {
            super(blockId.getBytes());
            byte[] blockNum = new byte[8];
            System.arraycopy(blockId.getBytes(), 0, blockNum, 0, 8);
            num = Longs.fromByteArray(blockNum);
        }

        /**
         * Use {@link #wrap(byte[])} instead.
         */
        public BlockId(byte[] hash, long num) {
            super(num, hash);
            this.num = num;
        }

        public BlockId(Sha256Hash hash, long num) {
            super(num, hash);
            this.num = num;
        }

        public BlockId(ByteString hash, long num) {
            super(num, hash.toByteArray());
            this.num = num;
        }

        public long getNum() {
            return num;
        }
    }

    private BlockId blockId = new BlockId(Sha256Hash.ZERO_HASH, 0);

    private Block block;
    public boolean generatedByMyself = false;
    private List<TransactionWrapper> transactions = new ArrayList<>();

    public BlockWrapper(long number, Sha256Hash hash, long when, ByteString extraData, ByteString witnessAddress) {
        // blockheader raw
        BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
        BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
                .setNumber(number)
                .setParentHash(hash.getByteString())
                .setTimestamp(when)
                .setExtraData(extraData)
                .setVersion(ChainConstant.BLOCK_VERSION)
                .setWitnessAddress(witnessAddress)
                .build();

        // block header
        BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
        BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();

        // block
        Block.Builder blockBuild = Block.newBuilder();
        this.block = blockBuild.setBlockHeader(blockHeader).build();
        initTxs();
    }


    public BlockWrapper(long timestamp, ByteString parentHash, long number, int version, ByteString extraData,
                        List<Transaction> transactionList) {
        // blockheader raw
        BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
        BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
                .setTimestamp(timestamp)
                .setParentHash(parentHash)
                .setExtraData(extraData)
                .setVersion(version)
                .setNumber(number)
                .build();

        // block header
        BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
        BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();

        // block
        Block.Builder blockBuild = Block.newBuilder();
        transactionList.forEach(trx -> blockBuild.addTransactions(trx));
        this.block = blockBuild.setBlockHeader(blockHeader).build();
        initTxs();
    }

    public BlockWrapper(Block block) {
        this.block = block;
        initTxs();
    }

    public BlockWrapper(byte[] data) throws BadItemException {
        try {
            this.block = Block.parseFrom(data);
            initTxs();
        } catch (InvalidProtocolBufferException e) {
            throw new BadItemException("Block proto data parse exception");
        }
    }

    public BlockWrapper(CodedInputStream codedInputStream) throws BadItemException {
        try {
            this.block = Block.parseFrom(codedInputStream);
            initTxs();
        } catch (Exception e) {
            logger.error("constructor block error : {}", e.getMessage());
            throw new BadItemException("Block proto data parse exception");
        }
    }

    public void addTransaction(TransactionWrapper pendingTrx) {
        this.block = this.block.toBuilder().addTransactions(pendingTrx.getInstance()).build();
        getTransactions().add(pendingTrx);
    }

    public List<TransactionWrapper> getTransactions() {
        return transactions;
    }

    private void initTxs() {
        transactions = this.block.getTransactionsList().stream()
                .map(trx -> new TransactionWrapper(trx))
                .collect(Collectors.toList());
    }

    public void sign(byte[] privateKey) {
        // TODO private_key == null
        ECKey ecKey = ECKey.fromPrivate(privateKey);
        ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
        ByteString sig = ByteString.copyFrom(signature.toByteArray());

        BlockHeader blockHeader = this.block.getBlockHeader().toBuilder().setWitnessSignature(sig)
                .build();

        this.block = this.block.toBuilder().setBlockHeader(blockHeader).build();
    }

    private Sha256Hash getRawHash() {
        return Sha256Hash.of(this.block.getBlockHeader().getRawData().toByteArray());
    }

    public boolean validateSignature(Manager dbManager) throws ValidateSignatureException {
        try {
            byte[] sigAddress = ECKey.signatureToAddress(getRawHash().getBytes(),
                    TransactionWrapper.getBase64FromByteString(block.getBlockHeader().getWitnessSignature()));
            byte[] witnessAccountAddress = block.getBlockHeader().getRawData().getWitnessAddress()
                    .toByteArray();

            if (dbManager.getDynamicPropertiesStore().getAllowMultiSign() != 1) {
                return Arrays.equals(sigAddress, witnessAccountAddress);
            } else {
                byte[] witnessPermissionAddress = dbManager.getAccountStore().get(witnessAccountAddress)
                        .getWitnessPermissionAddress();
                return Arrays.equals(sigAddress, witnessPermissionAddress);
            }

        } catch (SignatureException e) {
            throw new ValidateSignatureException(e.getMessage());
        }
    }

    public BlockId getBlockId() {
        if (blockId.equals(Sha256Hash.ZERO_HASH)) {
            blockId = new BlockId(Sha256Hash.of(this.block.getBlockHeader().getRawData().toByteArray()),
                    getNum());
        }
        return blockId;
    }

    public Sha256Hash calcMerkleRoot() {
        List<Transaction> transactionsList = this.block.getTransactionsList();

        if (CollectionUtils.isEmpty(transactionsList)) {
            return Sha256Hash.ZERO_HASH;
        }

        Vector<Sha256Hash> ids = transactionsList.stream()
                .map(TransactionWrapper::new)
                .map(TransactionWrapper::getMerkleHash)
                .collect(Collectors.toCollection(Vector::new));

        return MerkleTree.getInstance().createTree(ids).getRoot().getHash();
    }

    public void setMerkleRoot() {
        BlockHeader.raw blockHeaderRaw =
                this.block.getBlockHeader().getRawData().toBuilder()
                        .setTxTrieRoot(calcMerkleRoot().getByteString()).build();

        this.block = this.block.toBuilder().setBlockHeader(
                this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
    }

    public void setAccountStateRoot(byte[] root) {
        BlockHeader.raw blockHeaderRaw =
                this.block.getBlockHeader().getRawData().toBuilder()
                        .setAccountStateRoot(ByteString.copyFrom(root)).build();

        this.block = this.block.toBuilder().setBlockHeader(
                this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
    }

    /* only for genisis */
    public void setWitness(String witness) {
        BlockHeader.raw blockHeaderRaw =
                this.block.getBlockHeader().getRawData().toBuilder().setWitnessAddress(
                        ByteString.copyFrom(witness.getBytes())).build();

        this.block = this.block.toBuilder().setBlockHeader(
                this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
    }

    public Sha256Hash getMerkleRoot() {
        return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getTxTrieRoot());
    }

    public Sha256Hash getAccountRoot() {
        if (this.block.getBlockHeader().getRawData().getAccountStateRoot() != null
                && !this.block.getBlockHeader().getRawData().getAccountStateRoot().isEmpty()) {
            return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getAccountStateRoot());
        }
        return Sha256Hash.ZERO_HASH;
    }

    public ByteString getWitnessAddress() {
        return this.block.getBlockHeader().getRawData().getWitnessAddress();
    }

    @Override
    public byte[] getData() {
        return this.block.toByteArray();
    }

    @Override
    public Block getInstance() {
        return this.block;
    }

    public Sha256Hash getParentHash() {
        return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getParentHash());
    }

    public BlockId getParentBlockId() {
        return new BlockId(getParentHash(), getNum() - 1);
    }

    public ByteString getParentHashStr() {
        return this.block.getBlockHeader().getRawData().getParentHash();
    }

    public long getNum() {
        return this.block.getBlockHeader().getRawData().getNumber();
    }

    public long getVersion() {
        return this.block.getBlockHeader().getRawData().getVersion();
    }

    public long getTimeStamp() {
        return this.block.getBlockHeader().getRawData().getTimestamp();
    }

    private StringBuffer toStringBuff = new StringBuffer();

    @Override
    public String toString() {
        toStringBuff.setLength(0);

        toStringBuff.append("BlockWrapper \n[ ");
        toStringBuff.append("hash=").append(getBlockId()).append("\n");
        toStringBuff.append("number=").append(getNum()).append("\n");
        toStringBuff.append("parentId=").append(getParentHash()).append("\n");
        toStringBuff.append("witness address=")
                .append(ByteUtil.toHexString(getWitnessAddress().toByteArray())).append("\n");

        toStringBuff.append("generated by myself=").append(generatedByMyself).append("\n");
        toStringBuff.append("generate time=").append(Time.getTimeString(getTimeStamp())).append("\n");
        toStringBuff.append("version=").append(getVersion()).append("\n");
        if (!getTransactions().isEmpty()) {
            toStringBuff.append("merkle root=").append(getMerkleRoot()).append("\n");
            toStringBuff.append("account root=").append(getAccountRoot()).append("\n");
            toStringBuff.append("txs size=").append(getTransactions().size()).append("\n");
        } else {
            toStringBuff.append("txs are empty\n");
        }
        toStringBuff.append("]");
        return toStringBuff.toString();
    }
}
