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

import static org.gsc.core.exception.P2pException.TypeEnum.PROTOBUF_ERROR;
import static org.gsc.protos.Contract.AssetIssueContract;
import static org.gsc.protos.Contract.VoteAssetContract;
import static org.gsc.protos.Contract.VoteWitnessContract;
import static org.gsc.protos.Contract.WitnessCreateContract;
import static org.gsc.protos.Contract.WitnessUpdateContract;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.ECKey.ECDSASignature;
import org.gsc.net.peer.p2p.Message;
import org.gsc.runtime.vm.program.Program;
import org.gsc.runtime.vm.program.Program.BadJumpDestinationException;
import org.gsc.runtime.vm.program.Program.IllegalOperationException;
import org.gsc.runtime.vm.program.Program.JVMStackOverFlowException;
import org.gsc.runtime.vm.program.Program.OutOfCpuException;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.vm.program.Program.OutOfMemoryException;
import org.gsc.runtime.vm.program.Program.OutOfTimeException;
import org.gsc.runtime.vm.program.Program.PrecompiledContractException;
import org.gsc.runtime.vm.program.Program.StackTooLargeException;
import org.gsc.runtime.vm.program.Program.StackTooSmallException;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.Wallet;
import org.gsc.config.args.Args;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.db.TransactionTrace;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.PermissionException;
import org.gsc.core.exception.SignatureFormatException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AccountCreateContract;
import org.gsc.protos.Contract.AccountPermissionUpdateContract;
import org.gsc.protos.Contract.AccountUpdateContract;
import org.gsc.protos.Contract.ClearABIContract;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.ExchangeCreateContract;
import org.gsc.protos.Contract.ExchangeInjectContract;
import org.gsc.protos.Contract.ExchangeTransactionContract;
import org.gsc.protos.Contract.ExchangeWithdrawContract;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Contract.ParticipateAssetIssueContract;
import org.gsc.protos.Contract.ProposalApproveContract;
import org.gsc.protos.Contract.ProposalCreateContract;
import org.gsc.protos.Contract.ProposalDeleteContract;
import org.gsc.protos.Contract.SetAccountIdContract;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Contract.UnfreezeAssetContract;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Contract.UpdateAssetContract;
import org.gsc.protos.Contract.UpdateSettingContract;
import org.gsc.protos.Contract.WithdrawBalanceContract;
import org.gsc.protos.Protocol.Key;
import org.gsc.protos.Protocol.Permission;
import org.gsc.protos.Protocol.Permission.PermissionType;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.Result;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;
import org.gsc.protos.Protocol.Transaction.raw;

@Slf4j(topic = "wrapper")
public class TransactionWrapper implements ProtoWrapper<Transaction> {

    private Transaction transaction;
    @Setter
    private boolean isVerified = false;

    @Setter
    @Getter
    private long blockNum = -1;

    @Getter
    @Setter
    private TransactionTrace trxTrace;

    private static final ExecutorService executorService = Executors
            .newFixedThreadPool(Args.getInstance().getValidContractProtoThreadNum());

    /**
     * constructor TransactionWrapper.
     */
    public TransactionWrapper(Transaction trx) {
        this.transaction = trx;
    }

    /**
     * get account from bytes data.
     */
    public TransactionWrapper(byte[] data) throws BadItemException {
        try {
            this.transaction = Transaction.parseFrom(Message.getCodedInputStream(data));
        } catch (Exception e) {
            throw new BadItemException("Transaction proto data parse exception");
        }
    }

    public TransactionWrapper(CodedInputStream codedInputStream) throws BadItemException {
        try {
            this.transaction = Transaction.parseFrom(codedInputStream);
        } catch (IOException e) {
            throw new BadItemException("Transaction proto data parse exception");
        }
    }

  /*lll
  public TransactionWrapper(byte[] key, long value) throws IllegalArgumentException {
    if (!Wallet.addressValid(key)) {
      throw new IllegalArgumentException("Invalid address");
    }
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(value)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(key))
        .build();
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(ContractType.TransferContract).setParameter(
            Any.pack(transferContract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }*/

    public TransactionWrapper(AccountCreateContract contract, AccountStore accountStore) {
        AccountWrapper account = accountStore.get(contract.getOwnerAddress().toByteArray());
        if (account != null && account.getType() == contract.getType()) {
            return; // Account isexit
        }

        createTransaction(contract, ContractType.AccountCreateContract);
    }

    public TransactionWrapper(TransferContract contract, AccountStore accountStore) {
        Transaction.Contract.Builder contractBuilder = Transaction.Contract.newBuilder();

        AccountWrapper owner = accountStore.get(contract.getOwnerAddress().toByteArray());
        if (owner == null || owner.getBalance() < contract.getAmount()) {
            return; //The balance is not enough
        }

        createTransaction(contract, ContractType.TransferContract);
    }

    public TransactionWrapper(VoteWitnessContract voteWitnessContract) {
        createTransaction(voteWitnessContract, ContractType.VoteWitnessContract);
    }

    public TransactionWrapper(WitnessCreateContract witnessCreateContract) {
        createTransaction(witnessCreateContract, ContractType.WitnessCreateContract);
    }

    public TransactionWrapper(WitnessUpdateContract witnessUpdateContract) {
        createTransaction(witnessUpdateContract, ContractType.WitnessUpdateContract);
    }

    public TransactionWrapper(TransferAssetContract transferAssetContract) {
        createTransaction(transferAssetContract, ContractType.TransferAssetContract);
    }

    public TransactionWrapper(ParticipateAssetIssueContract participateAssetIssueContract) {
        createTransaction(participateAssetIssueContract, ContractType.ParticipateAssetIssueContract);
    }

    public TransactionWrapper(raw rawData, List<ByteString> signatureList) {
        this.transaction = Transaction.newBuilder().setRawData(rawData).addAllSignature(signatureList)
                .build();
    }

    public void resetResult() {
        if (this.getInstance().getRetCount() > 0) {
            this.transaction = this.getInstance().toBuilder().clearRet().build();
        }
    }

    public void setResult(TransactionResultWrapper transactionResultWrapper) {
        this.transaction = this.getInstance().toBuilder().addRet(transactionResultWrapper.getInstance())
                .build();
    }

    public void setReference(long blockNum, byte[] blockHash) {
        byte[] refBlockNum = ByteArray.fromLong(blockNum);
        Transaction.raw rawData = this.transaction.getRawData().toBuilder()
                .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockHash, 8, 16)))
                .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
                .build();
        this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
    }

    /**
     * @param expiration must be in milliseconds format
     */
    public void setExpiration(long expiration) {
        Transaction.raw rawData = this.transaction.getRawData().toBuilder().setExpiration(expiration)
                .build();
        this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
    }

    public long getExpiration() {
        return transaction.getRawData().getExpiration();
    }

    public void setTimestamp() {
        Transaction.raw rawData = this.transaction.getRawData().toBuilder()
                .setTimestamp(System.currentTimeMillis())
                .build();
        this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
    }

    public long getTimestamp() {
        return transaction.getRawData().getTimestamp();
    }

    @Deprecated
    public TransactionWrapper(AssetIssueContract assetIssueContract) {
        createTransaction(assetIssueContract, ContractType.AssetIssueContract);
    }

    public TransactionWrapper(com.google.protobuf.Message message, ContractType contractType) {
        Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
                Transaction.Contract.newBuilder().setType(contractType).setParameter(
                        Any.pack(message)).build());
        transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
    }

    @Deprecated
    public void createTransaction(com.google.protobuf.Message message, ContractType contractType) {
        Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
                Transaction.Contract.newBuilder().setType(contractType).setParameter(
                        Any.pack(message)).build());
        transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
    }

    public Sha256Hash getMerkleHash() {
        byte[] transBytes = this.transaction.toByteArray();
        return Sha256Hash.of(transBytes);
    }

    private Sha256Hash getRawHash() {
        return Sha256Hash.of(this.transaction.getRawData().toByteArray());
    }

    public void sign(byte[] privateKey) {
        ECKey ecKey = ECKey.fromPrivate(privateKey);
        ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
        ByteString sig = ByteString.copyFrom(signature.toByteArray());
        this.transaction = this.transaction.toBuilder().addSignature(sig).build();
    }

    public static long getWeight(Permission permission, byte[] address) {
        List<Key> list = permission.getKeysList();
        for (Key key : list) {
            if (key.getAddress().equals(ByteString.copyFrom(address))) {
                return key.getWeight();
            }
        }
        return 0;
    }

    public static long checkWeight(Permission permission, List<ByteString> sigs, byte[] hash,
                                   List<ByteString> approveList)
            throws SignatureException, PermissionException, SignatureFormatException {
        long currentWeight = 0;
        //    if (signature.size() % 65 != 0) {
        //      throw new SignatureFormatException("Signature size is " + signature.size());
        //    }
        if (sigs.size() > permission.getKeysCount()) {
            throw new PermissionException(
                    "Signature count is " + (sigs.size()) + " more than key counts of permission : "
                            + permission.getKeysCount());
        }
        HashMap addMap = new HashMap();
        for (ByteString sig : sigs) {
            if (sig.size() < 65) {
                throw new SignatureFormatException(
                        "Signature size is " + sig.size());
            }
            String base64 = TransactionWrapper.getBase64FromByteString(sig);
            byte[] address = ECKey.signatureToAddress(hash, base64);
            long weight = getWeight(permission, address);
            if (weight == 0) {
                throw new PermissionException(
                        ByteArray.toHexString(sig.toByteArray()) + " is signed by " + Wallet
                                .encode58Check(address) + " but it is not contained of permission.");
            }
            if (addMap.containsKey(base64)) {
                throw new PermissionException(Wallet.encode58Check(address) + " has signed twice!");
            }
            addMap.put(base64, weight);
            if (approveList != null) {
                approveList.add(ByteString.copyFrom(address)); //out put approve list.
            }
            currentWeight += weight;
        }
        return currentWeight;
    }

    public void addSign(byte[] privateKey, AccountStore accountStore)
            throws PermissionException, SignatureException, SignatureFormatException {
        Transaction.Contract contract = this.transaction.getRawData().getContract(0);
        int permissionId = contract.getPermissionId();
        byte[] owner = getOwner(contract);
        AccountWrapper account = accountStore.get(owner);
        if (account == null) {
            throw new PermissionException("Account is not exist!");
        }
        Permission permission = account.getPermissionById(permissionId);
        if (permission == null) {
            throw new PermissionException("permission isn't exit");
        }
        if (permissionId != 0) {
            if (permission.getType() != PermissionType.Active) {
                throw new PermissionException("Permission type is error");
            }
            //check oprations
            if (!Wallet.checkPermissionOprations(permission, contract)) {
                throw new PermissionException("Permission denied");
            }
        }
        List<ByteString> approveList = new ArrayList<>();
        ECKey ecKey = ECKey.fromPrivate(privateKey);
        byte[] address = ecKey.getAddress();
        if (this.transaction.getSignatureCount() > 0) {
            checkWeight(permission, this.transaction.getSignatureList(), this.getRawHash().getBytes(),
                    approveList);
            if (approveList.contains(ByteString.copyFrom(address))) {
                throw new PermissionException(Wallet.encode58Check(address) + " had signed!");
            }
        }

        long weight = getWeight(permission, address);
        if (weight == 0) {
            throw new PermissionException(
                    ByteArray.toHexString(privateKey) + "'s address is " + Wallet
                            .encode58Check(address) + " but it is not contained of permission.");
        }
        ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
        ByteString sig = ByteString.copyFrom(signature.toByteArray());
        this.transaction = this.transaction.toBuilder().addSignature(sig).build();
    }

    // todo mv this static function to wrapper util
    public static byte[] getOwner(Transaction.Contract contract) {
        ByteString owner;
        try {
            Any contractParameter = contract.getParameter();
            switch (contract.getType()) {
                case AccountCreateContract:
                    owner = contractParameter.unpack(AccountCreateContract.class).getOwnerAddress();
                    break;
                case TransferContract:
                    owner = contractParameter.unpack(TransferContract.class).getOwnerAddress();
                    break;
                case TransferAssetContract:
                    owner = contractParameter.unpack(TransferAssetContract.class).getOwnerAddress();
                    break;
                case VoteAssetContract:
                    owner = contractParameter.unpack(VoteAssetContract.class).getOwnerAddress();
                    break;
                case VoteWitnessContract:
                    owner = contractParameter.unpack(VoteWitnessContract.class).getOwnerAddress();
                    break;
                case WitnessCreateContract:
                    owner = contractParameter.unpack(WitnessCreateContract.class).getOwnerAddress();
                    break;
                case AssetIssueContract:
                    owner = contractParameter.unpack(AssetIssueContract.class).getOwnerAddress();
                    break;
                case WitnessUpdateContract:
                    owner = contractParameter.unpack(WitnessUpdateContract.class).getOwnerAddress();
                    break;
                case ParticipateAssetIssueContract:
                    owner = contractParameter.unpack(ParticipateAssetIssueContract.class).getOwnerAddress();
                    break;
                case AccountUpdateContract:
                    owner = contractParameter.unpack(AccountUpdateContract.class).getOwnerAddress();
                    break;
                case FreezeBalanceContract:
                    owner = contractParameter.unpack(FreezeBalanceContract.class).getOwnerAddress();
                    break;
                case UnfreezeBalanceContract:
                    owner = contractParameter.unpack(UnfreezeBalanceContract.class).getOwnerAddress();
                    break;
                case UnfreezeAssetContract:
                    owner = contractParameter.unpack(UnfreezeAssetContract.class).getOwnerAddress();
                    break;
                case WithdrawBalanceContract:
                    owner = contractParameter.unpack(WithdrawBalanceContract.class).getOwnerAddress();
                    break;
                case CreateSmartContract:
                    owner = contractParameter.unpack(Contract.CreateSmartContract.class).getOwnerAddress();
                    break;
                case TriggerSmartContract:
                    owner = contractParameter.unpack(Contract.TriggerSmartContract.class).getOwnerAddress();
                    break;
                case UpdateAssetContract:
                    owner = contractParameter.unpack(UpdateAssetContract.class).getOwnerAddress();
                    break;
                case ProposalCreateContract:
                    owner = contractParameter.unpack(ProposalCreateContract.class).getOwnerAddress();
                    break;
                case ProposalApproveContract:
                    owner = contractParameter.unpack(ProposalApproveContract.class).getOwnerAddress();
                    break;
                case ProposalDeleteContract:
                    owner = contractParameter.unpack(ProposalDeleteContract.class).getOwnerAddress();
                    break;
                case SetAccountIdContract:
                    owner = contractParameter.unpack(SetAccountIdContract.class).getOwnerAddress();
                    break;
                //case BuyStorageContract:
                //  owner = contractParameter.unpack(BuyStorageContract.class).getOwnerAddress();
                //  break;
                //case BuyStorageBytesContract:
                //  owner = contractParameter.unpack(BuyStorageBytesContract.class).getOwnerAddress();
                //  break;
                //case SellStorageContract:
                //  owner = contractParameter.unpack(SellStorageContract.class).getOwnerAddress();
                //  break;
                case UpdateSettingContract:
                    owner = contractParameter.unpack(UpdateSettingContract.class)
                            .getOwnerAddress();
                    break;
                case UpdateCpuLimitContract:
                    owner = contractParameter.unpack(Contract.UpdateCpuLimitContract.class)
                            .getOwnerAddress();
                    break;
                case ClearABIContract:
                    owner = contractParameter.unpack(ClearABIContract.class)
                            .getOwnerAddress();
                    break;
                case ExchangeCreateContract:
                    owner = contractParameter.unpack(ExchangeCreateContract.class).getOwnerAddress();
                    break;
                case ExchangeInjectContract:
                    owner = contractParameter.unpack(ExchangeInjectContract.class).getOwnerAddress();
                    break;
                case ExchangeWithdrawContract:
                    owner = contractParameter.unpack(ExchangeWithdrawContract.class).getOwnerAddress();
                    break;
                case ExchangeTransactionContract:
                    owner = contractParameter.unpack(ExchangeTransactionContract.class).getOwnerAddress();
                    break;
                case AccountPermissionUpdateContract:
                    owner = contractParameter.unpack(AccountPermissionUpdateContract.class).getOwnerAddress();
                    break;
                // todo add other contract
                default:
                    return null;
            }
            return owner.toByteArray();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    public static <T extends com.google.protobuf.Message> T parse(Class<T> clazz,
                                                                  CodedInputStream codedInputStream) throws InvalidProtocolBufferException {
        T defaultInstance = Internal.getDefaultInstance(clazz);
        return (T) defaultInstance.getParserForType().parseFrom(codedInputStream);
    }

    public static void validContractProto(List<Transaction> transactionList) throws P2pException {
        List<Future<Boolean>> futureList = new ArrayList<>();
        transactionList.forEach(transaction -> {
            Future<Boolean> future = executorService.submit(() -> {
                try {
                    validContractProto(transaction.getRawData().getContract(0));
                    return true;
                } catch (Exception e) {
                    logger.error("{}", e.getMessage());
                }
                return false;
            });
            futureList.add(future);
        });
        for (Future<Boolean> future : futureList) {
            try {
                if (!future.get()) {
                    throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
                }
            } catch (Exception e) {
                throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
            }
        }
    }

    public static void validContractProto(Transaction.Contract contract)
            throws InvalidProtocolBufferException, P2pException {
        Any contractParameter = contract.getParameter();
        Class clazz = null;
        switch (contract.getType()) {
            case AccountCreateContract:
                clazz = AccountCreateContract.class;
                break;
            case TransferContract:
                clazz = TransferContract.class;
                break;
            case TransferAssetContract:
                clazz = TransferAssetContract.class;
                break;
            case VoteAssetContract:
                clazz = VoteAssetContract.class;
                break;
            case VoteWitnessContract:
                clazz = VoteWitnessContract.class;
                break;
            case WitnessCreateContract:
                clazz = WitnessCreateContract.class;
                break;
            case AssetIssueContract:
                clazz = AssetIssueContract.class;
                break;
            case WitnessUpdateContract:
                clazz = WitnessUpdateContract.class;
                break;
            case ParticipateAssetIssueContract:
                clazz = ParticipateAssetIssueContract.class;
                break;
            case AccountUpdateContract:
                clazz = AccountUpdateContract.class;
                break;
            case FreezeBalanceContract:
                clazz = FreezeBalanceContract.class;
                break;
            case UnfreezeBalanceContract:
                clazz = UnfreezeBalanceContract.class;
                break;
            case UnfreezeAssetContract:
                clazz = UnfreezeAssetContract.class;
                break;
            case WithdrawBalanceContract:
                clazz = WithdrawBalanceContract.class;
                break;
            case CreateSmartContract:
                clazz = Contract.CreateSmartContract.class;
                break;
            case TriggerSmartContract:
                clazz = Contract.TriggerSmartContract.class;
                break;
            case UpdateAssetContract:
                clazz = UpdateAssetContract.class;
                break;
            case ProposalCreateContract:
                clazz = ProposalCreateContract.class;
                break;
            case ProposalApproveContract:
                clazz = ProposalApproveContract.class;
                break;
            case ProposalDeleteContract:
                clazz = ProposalDeleteContract.class;
                break;
            case SetAccountIdContract:
                clazz = SetAccountIdContract.class;
                break;
            case UpdateSettingContract:
                clazz = UpdateSettingContract.class;
                break;
            case UpdateCpuLimitContract:
                clazz = Contract.UpdateCpuLimitContract.class;
                break;
            case ClearABIContract:
                clazz = ClearABIContract.class;
                break;
            case ExchangeCreateContract:
                clazz = ExchangeCreateContract.class;
                break;
            case ExchangeInjectContract:
                clazz = ExchangeInjectContract.class;
                break;
            case ExchangeWithdrawContract:
                clazz = ExchangeWithdrawContract.class;
                break;
            case ExchangeTransactionContract:
                clazz = ExchangeTransactionContract.class;
                break;
            case AccountPermissionUpdateContract:
                clazz = AccountPermissionUpdateContract.class;
                break;
            // todo add other contract
            default:
                break;
        }
        if (clazz == null) {
            throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
        }
        com.google.protobuf.Message src = contractParameter.unpack(clazz);
        com.google.protobuf.Message contractMessage = parse(clazz,
                Message.getCodedInputStream(src.toByteArray()));

        //    if (!src.equals(contractMessage)) {
        //      throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
        //    }

        Message.compareBytes(src.toByteArray(), contractMessage.toByteArray());
    }

    // todo mv this static function to wrapper util
    public static byte[] getToAddress(Transaction.Contract contract) {
        ByteString to;
        try {
            Any contractParameter = contract.getParameter();
            switch (contract.getType()) {
                case TransferAssetContract:
                    to = contractParameter.unpack(TransferAssetContract.class).getToAddress();
                    break;
                case TransferContract:
                    to = contractParameter.unpack(TransferContract.class).getToAddress();
                    break;
                case ParticipateAssetIssueContract:
                    to = contractParameter.unpack(ParticipateAssetIssueContract.class).getToAddress();
                    break;
                // todo add other contract

                default:
                    return null;
            }
            return to.toByteArray();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    // todo mv this static function to wrapper util
    public static long getCallValue(Transaction.Contract contract) {
        int cpuForGsc;
        try {
            Any contractParameter = contract.getParameter();
            long callValue;
            switch (contract.getType()) {
                case TriggerSmartContract:
                    return contractParameter.unpack(TriggerSmartContract.class).getCallValue();

                case CreateSmartContract:
                    return contractParameter.unpack(CreateSmartContract.class).getNewContract()
                            .getCallValue();
                default:
                    return 0L;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return 0L;
        }
    }

    // todo mv this static function to wrapper util
    public static long getCallTokenValue(Transaction.Contract contract) {
        int cpuForGsc;
        try {
            Any contractParameter = contract.getParameter();
            long callValue;
            switch (contract.getType()) {
                case TriggerSmartContract:
                    return contractParameter.unpack(TriggerSmartContract.class).getCallTokenValue();

                case CreateSmartContract:
                    return contractParameter.unpack(CreateSmartContract.class).getCallTokenValue();
                default:
                    return 0L;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return 0L;
        }
    }

    public static String getBase64FromByteString(ByteString sign) {
        byte[] r = sign.substring(0, 32).toByteArray();
        byte[] s = sign.substring(32, 64).toByteArray();
        byte v = sign.byteAt(64);
        if (v < 27) {
            v += 27; //revId -> v
        }
        ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
        return signature.toBase64();
    }

    public static boolean validateSignature(Transaction transaction,
                                            byte[] hash, Manager manager)
            throws PermissionException, SignatureException, SignatureFormatException {
        AccountStore accountStore = manager.getAccountStore();
        Transaction.Contract contract = transaction.getRawData().getContractList().get(0);
        int permissionId = contract.getPermissionId();
        byte[] owner = getOwner(contract);
        AccountWrapper account = accountStore.get(owner);
        Permission permission = null;
        if (account == null) {
            if (permissionId == 0) {
                permission = AccountWrapper.getDefaultPermission(ByteString.copyFrom(owner));
            }
            if (permissionId == 2) {
                permission = AccountWrapper
                        .createDefaultActivePermission(ByteString.copyFrom(owner), manager);
            }
        } else {
            permission = account.getPermissionById(permissionId);
        }
        if (permission == null) {
            throw new PermissionException("permission isn't exit");
        }
        if (permissionId != 0) {
            if (permission.getType() != PermissionType.Active) {
                throw new PermissionException("Permission type is error");
            }
            //check oprations
            if (!Wallet.checkPermissionOprations(permission, contract)) {
                throw new PermissionException("Permission denied");
            }
        }
        long weight = checkWeight(permission, transaction.getSignatureList(), hash, null);
        if (weight >= permission.getThreshold()) {
            return true;
        }
        return false;
    }

    /**
     * validate signature
     */
    public boolean validateSignature(Manager manager)
            throws ValidateSignatureException {
        if (isVerified == true) {
            return true;
        }
        if (this.transaction.getSignatureCount() <= 0
                || this.transaction.getRawData().getContractCount() <= 0) {
            throw new ValidateSignatureException("miss sig or contract");
        }
        if (this.transaction.getSignatureCount() > manager.getDynamicPropertiesStore()
                .getTotalSignNum()) {
            throw new ValidateSignatureException("too many signatures");
        }
        byte[] hash = this.getRawHash().getBytes();
        try {
            if (!validateSignature(this.transaction, hash, manager)) {
                isVerified = false;
                throw new ValidateSignatureException("sig error");
            }
        } catch (SignatureException e) {
            isVerified = false;
            throw new ValidateSignatureException(e.getMessage());
        } catch (PermissionException e) {
            isVerified = false;
            throw new ValidateSignatureException(e.getMessage());
        } catch (SignatureFormatException e) {
            isVerified = false;
            throw new ValidateSignatureException(e.getMessage());
        }

        isVerified = true;
        return true;
    }

    public Sha256Hash getTransactionId() {
        return getRawHash();
    }

    @Override
    public byte[] getData() {
        return this.transaction.toByteArray();
    }

    public long getSerializedSize() {
        return this.transaction.getSerializedSize();
    }

    public long getResultSerializedSize() {
        long size = 0;
        for (Result result : this.transaction.getRetList()) {
            size += result.getSerializedSize();
        }
        return size;
    }

    @Override
    public Transaction getInstance() {
        return this.transaction;
    }

    private StringBuffer toStringBuff = new StringBuffer();

    @Override
    public String toString() {

        toStringBuff.setLength(0);
        toStringBuff.append("TransactionWrapper \n[ ");

        toStringBuff.append("hash=").append(getTransactionId()).append("\n");
        AtomicInteger i = new AtomicInteger();
        if (!getInstance().getRawData().getContractList().isEmpty()) {
            toStringBuff.append("contract list:{ ");
            getInstance().getRawData().getContractList().forEach(contract -> {
                toStringBuff.append("[" + i + "] ").append("type: ").append(contract.getType())
                        .append("\n");
                toStringBuff.append("from address=").append(getOwner(contract)).append("\n");
                toStringBuff.append("to address=").append(getToAddress(contract)).append("\n");
                if (contract.getType().equals(ContractType.TransferContract)) {
                    TransferContract transferContract;
                    try {
                        transferContract = contract.getParameter()
                                .unpack(TransferContract.class);
                        toStringBuff.append("transfer amount=").append(transferContract.getAmount())
                                .append("\n");
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                } else if (contract.getType().equals(ContractType.TransferAssetContract)) {
                    TransferAssetContract transferAssetContract;
                    try {
                        transferAssetContract = contract.getParameter()
                                .unpack(TransferAssetContract.class);
                        toStringBuff.append("transfer asset=").append(transferAssetContract.getAssetName())
                                .append("\n");
                        toStringBuff.append("transfer amount=").append(transferAssetContract.getAmount())
                                .append("\n");
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                }
                if (this.transaction.getSignatureList().size() >= i.get() + 1) {
                    toStringBuff.append("sign=").append(getBase64FromByteString(
                            this.transaction.getSignature(i.getAndIncrement()))).append("\n");
                }
            });
            toStringBuff.append("}\n");
        } else {
            toStringBuff.append("contract list is empty\n");
        }

        toStringBuff.append("]");
        return toStringBuff.toString();
    }

    public void setResult(Runtime runtime) {
        RuntimeException exception = runtime.getResult().getException();
        if (Objects.isNull(exception) && StringUtils
                .isEmpty(runtime.getRuntimeError()) && !runtime.getResult().isRevert()) {
            this.setResultCode(contractResult.SUCCESS);
            return;
        }
        if (runtime.getResult().isRevert()) {
            this.setResultCode(contractResult.REVERT);
            return;
        }
        if (exception instanceof IllegalOperationException) {
            this.setResultCode(contractResult.ILLEGAL_OPERATION);
            return;
        }
        if (exception instanceof OutOfCpuException) {
            this.setResultCode(contractResult.OUT_OF_CPU);
            return;
        }
        if (exception instanceof BadJumpDestinationException) {
            this.setResultCode(contractResult.BAD_JUMP_DESTINATION);
            return;
        }
        if (exception instanceof OutOfTimeException) {
            this.setResultCode(contractResult.OUT_OF_TIME);
            return;
        }
        if (exception instanceof OutOfMemoryException) {
            this.setResultCode(contractResult.OUT_OF_MEMORY);
            return;
        }
        if (exception instanceof PrecompiledContractException) {
            this.setResultCode(contractResult.PRECOMPILED_CONTRACT);
            return;
        }
        if (exception instanceof StackTooSmallException) {
            this.setResultCode(contractResult.STACK_TOO_SMALL);
            return;
        }
        if (exception instanceof StackTooLargeException) {
            this.setResultCode(contractResult.STACK_TOO_LARGE);
            return;
        }
        if (exception instanceof JVMStackOverFlowException) {
            this.setResultCode(contractResult.JVM_STACK_OVER_FLOW);
            return;
        }
        if (exception instanceof Program.TransferException) {
            this.setResultCode(contractResult.TRANSFER_FAILED);
            return;
        }
        this.setResultCode(contractResult.UNKNOWN);
    }

    public void setResultCode(contractResult code) {
        Result ret = Result.newBuilder().setContractRet(code).build();
        if (this.transaction.getRetCount() > 0) {
            ret = this.transaction.getRet(0).toBuilder().setContractRet(code).build();

            this.transaction = transaction.toBuilder().setRet(0, ret).build();
            return;
        }
        this.transaction = transaction.toBuilder().addRet(ret).build();
    }

    public contractResult getContractRet() {
        if (this.transaction.getRetCount() <= 0) {
            return null;
        }
        return this.transaction.getRet(0).getContractRet();
    }
}
