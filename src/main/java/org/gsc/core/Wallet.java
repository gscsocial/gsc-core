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



package org.gsc.core;

import static org.gsc.config.Parameter.DatabaseConstants.EXCHANGE_COUNT_LIMIT_MAX;
import static org.gsc.config.Parameter.DatabaseConstants.PROPOSAL_COUNT_LIMIT_MAX;

import com.google.common.base.CaseFormat;
import com.google.common.collect.*;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;

import java.security.SignatureException;
import java.util.*;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.operator.OperatorFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.AccountNetMessage;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.GrpcAPI.Address;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BlockList;
import org.gsc.api.GrpcAPI.DelegatedResourceList;
import org.gsc.api.GrpcAPI.ExchangeList;
import org.gsc.api.GrpcAPI.Node;
import org.gsc.api.GrpcAPI.NodeList;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.ProposalList;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.api.GrpcAPI.TransactionApprovedList;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.api.GrpcAPI.TransactionExtention.Builder;
import org.gsc.api.GrpcAPI.TransactionSignWeight;
import org.gsc.api.GrpcAPI.TransactionSignWeight.Result;
import org.gsc.api.GrpcAPI.WitnessList;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.Hash;
import org.gsc.net.node.NodeHandler;
import org.gsc.net.node.NodeManager;
import org.gsc.net.peer.p2p.Message;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.RuntimeImpl;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.utils.Base58;
import org.gsc.utils.ByteArray;
import org.gsc.utils.ByteUtil;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.Utils;
import org.gsc.core.operator.Operator;
import org.gsc.core.wrapper.*;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.*;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.DupTransactionException;
import org.gsc.core.exception.HeaderNotFound;
import org.gsc.core.exception.NonUniqueObjectException;
import org.gsc.core.exception.PermissionException;
import org.gsc.core.exception.SignatureFormatException;
import org.gsc.core.exception.StoreException;
import org.gsc.core.exception.TaposException;
import org.gsc.core.exception.TooBigTransactionException;
import org.gsc.core.exception.TransactionExpirationException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.GSCNetService;
import org.gsc.net.peer.message.TransactionMessage;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.DelegatedResourceAccountIndex;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.protos.Protocol.Permission;
import org.gsc.protos.Protocol.Permission.PermissionType;
import org.gsc.protos.Protocol.Proposal;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.SmartContract.ABI;
import org.gsc.protos.Protocol.SmartContract.ABI.Entry.StateMutabilityType;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.Result.code;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.protos.Protocol.TransactionSign;

@Slf4j
@Component
public class Wallet {

    @Getter
    private final ECKey ecKey;
    @Autowired
    private GSCNetService gscNetService;
    @Autowired
    private GSCNetDelegate gscNetDelegate;
    @Autowired
    private Manager dbManager;
    @Autowired
    private NodeManager nodeManager;
    private static String addressPreFixString = Constant.ADD_PRE_FIX_STRING;//default testnet
    private static byte[] addressPreFixByte = Constant.ADD_PRE_FIX_BYTE;

    private int minEffectiveConnection = Args.getInstance().getMinEffectiveConnection();

    /**
     * Creates a new Wallet with a random ECKey.
     */
    public Wallet() {
        this.ecKey = new ECKey(Utils.getRandom());
    }

    /**
     * Creates a Wallet with an existing ECKey.
     */
    public Wallet(final ECKey ecKey) {
        this.ecKey = ecKey;
        logger.info("wallet address: {}", ByteArray.toHexString(this.ecKey.getAddress()));
    }

    public static boolean isConstant(ABI abi, TriggerSmartContract triggerSmartContract)
            throws ContractValidateException {
        try {
            boolean constant = isConstant(abi, getSelector(triggerSmartContract.getData().toByteArray()));
            if (constant) {
                if (!Args.getInstance().isSupportConstant()) {
                    throw new ContractValidateException("this node don't support constant");
                }
            }
            return constant;
        } catch (ContractValidateException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    public byte[] getAddress() {
        return ecKey.getAddress();
    }

    public static String getAddressPreFixString() {
        return addressPreFixString;
    }

    public static void setAddressPreFixString(String addressPreFixString) {
        Wallet.addressPreFixString = addressPreFixString;
    }

    public static byte[] getAddressPreFixByte() {
        return addressPreFixByte;
    }

    public static void setAddressPreFixByte(byte[] addressPreFixByte) {
        Wallet.addressPreFixByte = addressPreFixByte;
    }

    public static boolean addressValid(byte[] address) {
        if (ArrayUtils.isEmpty(address)) {
            logger.warn("Warning: Address is empty !!");
            return false;
        }
        if (address.length != Constant.ADDRESS_SIZE / 2) {
            logger.warn(
                    "Warning: Address length need " + Constant.ADDRESS_SIZE + " but " + address.length
                            + " !!");
            return false;
        }
        byte[] addressPre = new byte[3];
        System.arraycopy(address, 0 , addressPre, 0, addressPre.length);
        if (!Arrays.equals(addressPre, addressPreFixByte)) {
            logger.warn("Warning: Address need prefix with " + addressPreFixByte + " but "
                    + Hex.toHexString(addressPre) + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

    public static String encode58Check(byte[] input) {

        byte[] hash0 = Sha256Hash.hash(input);
        byte[] hash1 = Sha256Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    private static byte[] decode58Check(String input) {
        // 01 f8 0cfff10ea6c789b272a5257baa5a6eb8fa5aa31b81
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Sha256Hash.hash(decodeData);
        byte[] hash1 = Sha256Hash.hash(hash0);

        if (hash1[0] == decodeCheck[decodeData.length] &&
                hash1[1] == decodeCheck[decodeData.length + 1] &&
                hash1[2] == decodeCheck[decodeData.length + 2] &&
                hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    public static byte[] generateContractAddress(Transaction trx) {

        CreateSmartContract contract = ContractWrapper.getSmartContractFromTransaction(trx);
        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        TransactionWrapper trxCap = new TransactionWrapper(trx);
        byte[] txRawDataHash = trxCap.getTransactionId().getBytes();

        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

        return Hash.sha3omit12(combined);

    }

    public static byte[] generateContractAddress(byte[] ownerAddress, byte[] txRawDataHash) {

        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

        return Hash.sha3omit12(combined);

    }

    // for `CREATE2`
    public static byte[] generateContractAddress2(byte[] address, byte[] salt, byte[] code) {
        byte[] mergedData = ByteUtil.merge(address, salt, Hash.sha3(code));
        return Hash.sha3omit12(mergedData);
    }

    // for `CREATE`
    public static byte[] generateContractAddress(byte[] transactionRootId, long nonce) {
        byte[] nonceBytes = Longs.toByteArray(nonce);
        byte[] combined = new byte[transactionRootId.length + nonceBytes.length];
        System.arraycopy(transactionRootId, 0, combined, 0, transactionRootId.length);
        System.arraycopy(nonceBytes, 0, combined, transactionRootId.length, nonceBytes.length);

        return Hash.sha3omit12(combined);
    }

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            logger.warn("Warning: Address is empty !!");
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (address == null) {
            return null;
        }

        if (!addressValid(address)) {
            return null;
        }

        return address;
    }


    public Account getAccount(Account account) {
        AccountStore accountStore = dbManager.getAccountStore();
        AccountWrapper accountWrapper = accountStore.get(account.getAddress().toByteArray());
        if (accountWrapper == null) {
            return null;
        }
        NetProcessor processor = new NetProcessor(dbManager);
        processor.updateUsage(accountWrapper);

        CpuProcessor cpuProcessor = new CpuProcessor(dbManager);
        cpuProcessor.updateUsage(accountWrapper);

        long genesisTimeStamp = dbManager.getGenesisBlock().getTimeStamp();
        accountWrapper.setLatestConsumeTime(genesisTimeStamp
                + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountWrapper.getLatestConsumeTime());
        accountWrapper.setLatestConsumeFreeTime(genesisTimeStamp
                + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountWrapper.getLatestConsumeFreeTime());
        accountWrapper.setLatestConsumeTimeForCpu(genesisTimeStamp
                + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountWrapper.getLatestConsumeTimeForCpu());

        return accountWrapper.getInstance();
    }


    public Account getAccountById(Account account) {
        AccountStore accountStore = dbManager.getAccountStore();
        AccountIdIndexStore accountIdIndexStore = dbManager.getAccountIdIndexStore();
        byte[] address = accountIdIndexStore.get(account.getAccountId());
        if (address == null) {
            return null;
        }
        AccountWrapper accountWrapper = accountStore.get(address);
        if (accountWrapper == null) {
            return null;
        }
        NetProcessor processor = new NetProcessor(dbManager);
        processor.updateUsage(accountWrapper);

        CpuProcessor cpuProcessor = new CpuProcessor(dbManager);
        cpuProcessor.updateUsage(accountWrapper);

        return accountWrapper.getInstance();
    }

    /**
     * Create a transaction.
     */
  /*public Transaction createTransaction(byte[] address, String to, long amount) {
    long balance = getBalance(address);
    return new TransactionWrapper(address, to, amount, balance, utxoStore).getInstance();
  } */

    /**
     * Create a transaction by contract.
     */
    @Deprecated
    public Transaction createTransaction(TransferContract contract) {
        AccountStore accountStore = dbManager.getAccountStore();
        return new TransactionWrapper(contract, accountStore).getInstance();
    }

    public TransactionWrapper createTransactionWrapper(com.google.protobuf.Message message,
                                                       ContractType contractType) throws ContractValidateException {
        TransactionWrapper trx = new TransactionWrapper(message, contractType);
        if (contractType != ContractType.CreateSmartContract
                && contractType != ContractType.TriggerSmartContract) {
            List<Operator> actList = OperatorFactory.createOperator(trx, dbManager);
            for (Operator act : actList) {
                act.validate();
            }
        }

        if (contractType == ContractType.CreateSmartContract) {

            CreateSmartContract contract = ContractWrapper
                    .getSmartContractFromTransaction(trx.getInstance());
            long percent = contract.getNewContract().getConsumeUserResourcePercent();
            if (percent < 0 || percent > 100) {
                throw new ContractValidateException("percent must be >= 0 and <= 100");
            }
        }

        try {
            BlockId blockId = dbManager.getHeadBlockId();
            if (Args.getInstance().getTrxReferenceBlock().equals("confirmed")) {
                blockId = dbManager.getConfirmedBlockId();
            }
            trx.setReference(blockId.getNum(), blockId.getBytes());
            long expiration =
                    dbManager.getHeadBlockTimeStamp() + Args.getInstance()
                            .getTrxExpirationTimeInMilliseconds();
            trx.setExpiration(expiration);
            trx.setTimestamp();
        } catch (Exception e) {
            logger.error("Create transaction wrapper failed.", e);
        }
        return trx;
    }

    /**
     * Broadcast a transaction.
     */
    public GrpcAPI.Return broadcastTransaction(Transaction signaturedTransaction) {
        GrpcAPI.Return.Builder builder = GrpcAPI.Return.newBuilder();
        TransactionWrapper trx = new TransactionWrapper(signaturedTransaction);
        try {
            Message message = new TransactionMessage(signaturedTransaction.toByteArray());
            if (minEffectiveConnection != 0) {
                if (gscNetDelegate.getActivePeer().isEmpty()) {
                    logger.warn("Broadcast transaction {} failed, no connection.", trx.getTransactionId());
                    return builder.setResult(false).setCode(response_code.NO_CONNECTION)
                            .setMessage(ByteString.copyFromUtf8("no connection"))
                            .build();
                }

                int count = (int) gscNetDelegate.getActivePeer().stream()
                        .filter(p -> !p.isNeedSyncFromUs() && !p.isNeedSyncFromPeer())
                        .count();

                if (count < minEffectiveConnection) {
                    String info = "effective connection:" + count + " lt minEffectiveConnection:"
                            + minEffectiveConnection;
                    logger.warn("Broadcast transaction {} failed, {}.", trx.getTransactionId(), info);
                    return builder.setResult(false).setCode(response_code.NOT_ENOUGH_EFFECTIVE_CONNECTION)
                            .setMessage(ByteString.copyFromUtf8(info))
                            .build();
                }
            }

            if (dbManager.isTooManyPending()) {
                logger.warn("Broadcast transaction {} failed, too many pending.", trx.getTransactionId());
                return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
            }

            if (dbManager.isGeneratingBlock()) {
                logger
                        .warn("Broadcast transaction {} failed, is generating block.", trx.getTransactionId());
                return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
            }

            if (dbManager.getTransactionIdCache().getIfPresent(trx.getTransactionId()) != null) {
                logger.warn("Broadcast transaction {} failed, is already exist.", trx.getTransactionId());
                return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR).build();
            } else {
                dbManager.getTransactionIdCache().put(trx.getTransactionId(), true);
            }
            if (dbManager.getDynamicPropertiesStore().supportVM()) {
                trx.resetResult();
            }
            dbManager.pushTransaction(trx);
            gscNetService.broadcast(message);
            logger.info("Broadcast transaction {} successfully.", trx.getTransactionId());
            return builder.setResult(true).setCode(response_code.SUCCESS).build();
        } catch (ValidateSignatureException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.SIGERROR)
                    .setMessage(ByteString.copyFromUtf8("validate signature error " + e.getMessage()))
                    .build();
        } catch (ContractValidateException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()))
                    .build();
        } catch (ContractExeException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
                    .setMessage(ByteString.copyFromUtf8("contract execute error : " + e.getMessage()))
                    .build();
        } catch (AccountResourceInsufficientException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.NET_ERROR)
                    .setMessage(ByteString.copyFromUtf8("AccountResourceInsufficient error"))
                    .build();
        } catch (DupTransactionException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR)
                    .setMessage(ByteString.copyFromUtf8("dup transaction"))
                    .build();
        } catch (TaposException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.TAPOS_ERROR)
                    .setMessage(ByteString.copyFromUtf8("Tapos check error"))
                    .build();
        } catch (TooBigTransactionException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.TOO_BIG_TRANSACTION_ERROR)
                    .setMessage(ByteString.copyFromUtf8("transaction size is too big"))
                    .build();
        } catch (TransactionExpirationException e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.TRANSACTION_EXPIRATION_ERROR)
                    .setMessage(ByteString.copyFromUtf8("transaction expired"))
                    .build();
        } catch (Exception e) {
            logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
            return builder.setResult(false).setCode(response_code.OTHER_ERROR)
                    .setMessage(ByteString.copyFromUtf8("other error : " + e.getMessage()))
                    .build();
        }
    }

    public TransactionWrapper getTransactionSign(TransactionSign transactionSign) {
        byte[] privateKey = transactionSign.getPrivateKey().toByteArray();
        TransactionWrapper trx = new TransactionWrapper(transactionSign.getTransaction());
        trx.sign(privateKey);
        return trx;
    }

    public TransactionWrapper addSign(TransactionSign transactionSign)
            throws PermissionException, SignatureException, SignatureFormatException {
        byte[] privateKey = transactionSign.getPrivateKey().toByteArray();
        TransactionWrapper trx = new TransactionWrapper(transactionSign.getTransaction());
        trx.addSign(privateKey, dbManager.getAccountStore());
        return trx;
    }

    public static boolean checkPermissionOprations(Permission permission, Contract contract)
            throws PermissionException {
        ByteString operations = permission.getOperations();
        if (operations.size() != 32) {
            throw new PermissionException("operations size must 32");
        }
        int contractType = contract.getTypeValue();
        boolean b = (operations.byteAt(contractType / 8) & (1 << (contractType % 8))) != 0;
        return b;
    }

    public TransactionSignWeight getTransactionSignWeight(Transaction trx) {
        TransactionSignWeight.Builder tswBuilder = TransactionSignWeight.newBuilder();
        TransactionExtention.Builder trxExBuilder = TransactionExtention.newBuilder();
        trxExBuilder.setTransaction(trx);
        trxExBuilder.setTxid(ByteString.copyFrom(Sha256Hash.hash(trx.getRawData().toByteArray())));
        Return.Builder retBuilder = Return.newBuilder();
        retBuilder.setResult(true).setCode(response_code.SUCCESS);
        trxExBuilder.setResult(retBuilder);
        tswBuilder.setTransaction(trxExBuilder);
        Result.Builder resultBuilder = Result.newBuilder();
        try {
            Contract contract = trx.getRawData().getContract(0);
            byte[] owner = TransactionWrapper.getOwner(contract);
            AccountWrapper account = dbManager.getAccountStore().get(owner);
            if (account == null) {
                throw new PermissionException("Account is not exist!");
            }
            int permissionId = contract.getPermissionId();
            Permission permission = account.getPermissionById(permissionId);
            if (permission == null) {
                throw new PermissionException("permission isn't exit");
            }
            if (permissionId != 0) {
                if (permission.getType() != PermissionType.Active) {
                    throw new PermissionException("Permission type is error");
                }
                //check oprations
                if (!checkPermissionOprations(permission, contract)) {
                    throw new PermissionException("Permission denied");
                }
            }
            tswBuilder.setPermission(permission);
            if (trx.getSignatureCount() > 0) {
                List<ByteString> approveList = new ArrayList<ByteString>();
                long currentWeight = TransactionWrapper.checkWeight(permission, trx.getSignatureList(),
                        Sha256Hash.hash(trx.getRawData().toByteArray()), approveList);
                tswBuilder.addAllApprovedList(approveList);
                tswBuilder.setCurrentWeight(currentWeight);
            }
            if (tswBuilder.getCurrentWeight() >= permission.getThreshold()) {
                resultBuilder.setCode(Result.response_code.ENOUGH_PERMISSION);
            } else {
                resultBuilder.setCode(Result.response_code.NOT_ENOUGH_PERMISSION);
            }
        } catch (SignatureFormatException signEx) {
            resultBuilder.setCode(Result.response_code.SIGNATURE_FORMAT_ERROR);
            resultBuilder.setMessage(signEx.getMessage());
        } catch (SignatureException signEx) {
            resultBuilder.setCode(Result.response_code.COMPUTE_ADDRESS_ERROR);
            resultBuilder.setMessage(signEx.getMessage());
        } catch (PermissionException permEx) {
            resultBuilder.setCode(Result.response_code.PERMISSION_ERROR);
            resultBuilder.setMessage(permEx.getMessage());
        } catch (Exception ex) {
            resultBuilder.setCode(Result.response_code.OTHER_ERROR);
            resultBuilder.setMessage(ex.getClass() + " : " + ex.getMessage());
        }
        tswBuilder.setResult(resultBuilder);
        return tswBuilder.build();
    }

    public TransactionApprovedList getTransactionApprovedList(Transaction trx) {
        TransactionApprovedList.Builder tswBuilder = TransactionApprovedList.newBuilder();
        TransactionExtention.Builder trxExBuilder = TransactionExtention.newBuilder();
        trxExBuilder.setTransaction(trx);
        trxExBuilder.setTxid(ByteString.copyFrom(Sha256Hash.hash(trx.getRawData().toByteArray())));
        Return.Builder retBuilder = Return.newBuilder();
        retBuilder.setResult(true).setCode(response_code.SUCCESS);
        trxExBuilder.setResult(retBuilder);
        tswBuilder.setTransaction(trxExBuilder);
        TransactionApprovedList.Result.Builder resultBuilder = TransactionApprovedList.Result
                .newBuilder();
        try {
            Contract contract = trx.getRawData().getContract(0);
            byte[] owner = TransactionWrapper.getOwner(contract);
            AccountWrapper account = dbManager.getAccountStore().get(owner);
            if (account == null) {
                throw new PermissionException("Account is not exist!");
            }

            if (trx.getSignatureCount() > 0) {
                List<ByteString> approveList = new ArrayList<ByteString>();
                byte[] hash = Sha256Hash.hash(trx.getRawData().toByteArray());
                for (ByteString sig : trx.getSignatureList()) {
                    if (sig.size() < 65) {
                        throw new SignatureFormatException(
                                "Signature size is " + sig.size());
                    }
                    String base64 = TransactionWrapper.getBase64FromByteString(sig);
                    byte[] address = ECKey.signatureToAddress(hash, base64);
                    approveList.add(ByteString.copyFrom(address)); //out put approve list.
                }
                tswBuilder.addAllApprovedList(approveList);
            }
            resultBuilder.setCode(TransactionApprovedList.Result.response_code.SUCCESS);
        } catch (SignatureFormatException signEx) {
            resultBuilder.setCode(TransactionApprovedList.Result.response_code.SIGNATURE_FORMAT_ERROR);
            resultBuilder.setMessage(signEx.getMessage());
        } catch (SignatureException signEx) {
            resultBuilder.setCode(TransactionApprovedList.Result.response_code.COMPUTE_ADDRESS_ERROR);
            resultBuilder.setMessage(signEx.getMessage());
        } catch (Exception ex) {
            resultBuilder.setCode(TransactionApprovedList.Result.response_code.OTHER_ERROR);
            resultBuilder.setMessage(ex.getClass() + " : " + ex.getMessage());
        }
        tswBuilder.setResult(resultBuilder);
        return tswBuilder.build();
    }

    public byte[] pass2Key(byte[] passPhrase) {
        return Sha256Hash.hash(passPhrase);
    }

    public byte[] createAdresss(byte[] passPhrase) {
        byte[] privateKey = pass2Key(passPhrase);
        ECKey ecKey = ECKey.fromPrivate(privateKey);
        return ecKey.getAddress();
    }

    public GrpcAPI.VoteStatistics getWitnessVoteStatistics() {
        VotesStore votesStore = dbManager.getVotesStore();

        final Map<ByteString, Long> countWitnessMap = Maps.newHashMap();
        Iterator<Map.Entry<byte[], VotesWrapper>> dbIterator = votesStore.iterator();

        while (dbIterator.hasNext()) {
            Map.Entry<byte[], VotesWrapper> next = dbIterator.next();
            VotesWrapper votes = next.getValue();

            votes.getNewVotes().forEach(vote -> {
                ByteString voteAddress = vote.getVoteAddress();
                long voteCount = vote.getVoteCount();
                if (countWitnessMap.containsKey(voteAddress)) {
                    countWitnessMap.put(voteAddress, countWitnessMap.get(voteAddress) + voteCount);
                } else {
                    countWitnessMap.put(voteAddress, voteCount);
                }
            });
        }

        final GrpcAPI.VoteStatistics.Builder countWitness = GrpcAPI.VoteStatistics.newBuilder();
        countWitnessMap.forEach((key, value) -> {
            Protocol.Vote.Builder vote = Protocol.Vote.newBuilder();
            vote.setVoteAddress(key).setVoteCount(value);
            countWitness.addVotes(vote.build());
        });
        return countWitness.build();
    }

    public Block getNowBlock() {
        List<BlockWrapper> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
        if (CollectionUtils.isEmpty(blockList)) {
            return null;
        } else {
            return blockList.get(0).getInstance();
        }
    }

    public Block getBlockByNum(long blockNum) {
        try {
            return dbManager.getBlockByNum(blockNum).getInstance();
        } catch (StoreException e) {
            logger.info(e.getMessage());
            return null;
        }
    }

    public long getTransactionCountByBlockNum(long blockNum) {
        long count = 0;

        try {
            Block block = dbManager.getBlockByNum(blockNum).getInstance();
            count = block.getTransactionsCount();
        } catch (StoreException e) {
            logger.error(e.getMessage());
        }

        return count;
    }

    public WitnessList getWitnessList() {
        WitnessList.Builder builder = WitnessList.newBuilder();
        List<WitnessWrapper> witnessWrapperList = dbManager.getWitnessStore().getAllWitnesses();
        witnessWrapperList
                .forEach(witnessWrapper -> builder.addWitnesses(witnessWrapper.getInstance()));
        return builder.build();
    }

    public ProposalList getProposalList() {
        ProposalList.Builder builder = ProposalList.newBuilder();
        List<ProposalWrapper> proposalWrapperList = dbManager.getProposalStore().getAllProposals();
        proposalWrapperList
                .forEach(proposalWrapper -> builder.addProposals(proposalWrapper.getInstance()));
        return builder.build();
    }

    public DelegatedResourceList getDelegatedResource(ByteString fromAddress, ByteString toAddress) {
        DelegatedResourceList.Builder builder = DelegatedResourceList.newBuilder();
        byte[] dbKey = DelegatedResourceWrapper
                .createDbKey(fromAddress.toByteArray(), toAddress.toByteArray());
        DelegatedResourceWrapper delegatedResourceWrapper = dbManager.getDelegatedResourceStore()
                .get(dbKey);
        if (delegatedResourceWrapper != null) {
            builder.addDelegatedResource(delegatedResourceWrapper.getInstance());
        }
        return builder.build();
    }

    public DelegatedResourceAccountIndex getDelegatedResourceAccountIndex(ByteString address) {
        DelegatedResourceAccountIndexWrapper accountIndexWrapper =
                dbManager.getDelegatedResourceAccountIndexStore().get(address.toByteArray());
        if (accountIndexWrapper != null) {
            return accountIndexWrapper.getInstance();
        } else {
            return null;
        }
    }

    public ExchangeList getExchangeList() {
        ExchangeList.Builder builder = ExchangeList.newBuilder();
        List<ExchangeWrapper> exchangeWrapperList = dbManager.getExchangeStoreFinal().getAllExchanges();

        exchangeWrapperList
                .forEach(exchangeWrapper -> builder.addExchanges(exchangeWrapper.getInstance()));
        return builder.build();
    }

    public Protocol.ChainParameters getChainParameters() {
        Protocol.ChainParameters.Builder builder = Protocol.ChainParameters.newBuilder();

        // MAINTENANCE_TIME_INTERVAL, //ms  ,0
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getMaintenanceTimeInterval")
                        .setValue(dbManager.getDynamicPropertiesStore().getMaintenanceTimeInterval())
                        .build());
        //    ACCOUNT_UPGRADE_COST, //drop ,1
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAccountUpgradeCost")
                        .setValue(dbManager.getDynamicPropertiesStore().getAccountUpgradeCost())
                        .build());
        //    CREATE_ACCOUNT_FEE, //drop ,2
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getCreateAccountFee")
                        .setValue(dbManager.getDynamicPropertiesStore().getCreateAccountFee())
                        .build());
        //    TRANSACTION_FEE, //drop ,3
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getTransactionFee")
                        .setValue(dbManager.getDynamicPropertiesStore().getTransactionFee())
                        .build());
        //    ASSET_ISSUE_FEE, //drop ,4
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAssetIssueFee")
                        .setValue(dbManager.getDynamicPropertiesStore().getAssetIssueFee())
                        .build());
        //    WITNESS_PAY_PER_BLOCK, //drop ,5
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getWitnessPayPerBlock")
                        .setValue(dbManager.getDynamicPropertiesStore().getWitnessPayPerBlock())
                        .build());
        //    WITNESS_STANDBY_ALLOWANCE, //drop ,6
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getWitnessStandbyAllowance")
                        .setValue(dbManager.getDynamicPropertiesStore().getWitnessStandbyAllowance())
                        .build());
        //    CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT, //drop ,7
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getCreateNewAccountFeeInSystemContract")
                        .setValue(
                                dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract())
                        .build());
        //    CREATE_NEW_ACCOUNT_NET_RATE, // 1 ~ ,8
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getCreateNewAccountNetRate")
                        .setValue(dbManager.getDynamicPropertiesStore().getCreateNewAccountNetRate())
                        .build());
        //    ALLOW_CREATION_OF_CONTRACTS, // 0 / >0 ,9
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowCreationOfContracts")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowCreationOfContracts())
                        .build());
        //    REMOVE_THE_POWER_OF_THE_GR,  // 1 ,10
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getRemoveThePowerOfTheGr")
                        .setValue(dbManager.getDynamicPropertiesStore().getRemoveThePowerOfTheGr())
                        .build());
        //    CPU_FEE, // drop, 11
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getCpuFee")
                        .setValue(dbManager.getDynamicPropertiesStore().getCpuFee())
                        .build());
        //    EXCHANGE_CREATE_FEE, // drop, 12
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getExchangeCreateFee")
                        .setValue(dbManager.getDynamicPropertiesStore().getExchangeCreateFee())
                        .build());
        //    MAX_CPU_TIME_OF_ONE_TX, // ms, 13
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getMaxCpuTimeOfOneTx")
                        .setValue(dbManager.getDynamicPropertiesStore().getMaxCpuTimeOfOneTx())
                        .build());
        //    ALLOW_UPDATE_ACCOUNT_NAME, // 1, 14
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowUpdateAccountName")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowUpdateAccountName())
                        .build());
        //    ALLOW_SAME_TOKEN_NAME, // 1, 15
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowSameTokenName")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowSameTokenName())
                        .build());
        //    ALLOW_DELEGATE_RESOURCE, // 0, 16
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowDelegateResource")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowDelegateResource())
                        .build());
        //    TOTAL_CPU_LIMIT, // 50,000,000,000, 17
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getTotalCpuLimit")
                        .setValue(dbManager.getDynamicPropertiesStore().getTotalCpuLimit())
                        .build());
        //    ALLOW_GVM_TRANSFER_GRC10, // 1, 18
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowGvmTransferGrc10")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowGvmTransferGrc10())
                        .build());
        //    TOTAL_CURRENT_CPU_LIMIT, // 50,000,000,000, 19
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getTotalCpuCurrentLimit")
                        .setValue(dbManager.getDynamicPropertiesStore().getTotalCpuCurrentLimit())
                        .build());
        //    ALLOW_MULTI_SIGN, // 1, 20
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowMultiSign")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowMultiSign())
                        .build());
        //    ALLOW_ADAPTIVE_CPU, // 1, 21
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowAdaptiveCpu")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowAdaptiveCpu())
                        .build());
        //other chainParameters
        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
                .setKey("getTotalCpuTargetLimit")
                .setValue(dbManager.getDynamicPropertiesStore().getTotalCpuTargetLimit())
                .build());

        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
                .setKey("getTotalCpuAverageUsage")
                .setValue(dbManager.getDynamicPropertiesStore().getTotalCpuAverageUsage())
                .build());

        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
                .setKey("getUpdateAccountPermissionFee")
                .setValue(dbManager.getDynamicPropertiesStore().getUpdateAccountPermissionFee())
                .build());

        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
                .setKey("getMultiSignFee")
                .setValue(dbManager.getDynamicPropertiesStore().getMultiSignFee())
                .build());

        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
                .setKey("getUpdateAccountPermissionFee")
                .setValue(dbManager.getDynamicPropertiesStore().getUpdateAccountPermissionFee())
                .build());

        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
                .setKey("getAllowAccountStateRoot")
                .setValue(dbManager.getDynamicPropertiesStore().getAllowAccountStateRoot())
                .build());

        builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
                .setKey("getAllowProtoFilterNum")
                .setValue(dbManager.getDynamicPropertiesStore().getAllowProtoFilterNum())
                .build());

        // ALLOW_GVM_CONSTANTINOPLE, // 1, 30
        builder.addChainParameter(
                Protocol.ChainParameters.ChainParameter.newBuilder()
                        .setKey("getAllowGvmConstantinople")
                        .setValue(dbManager.getDynamicPropertiesStore().getAllowGvmConstantinople())
                        .build());

        return builder.build();
    }

    public static String makeUpperCamelMethod(String originName) {
        return "get" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, originName)
                .replace("_", "");
    }

    public AssetIssueList getAssetIssueList() {
        AssetIssueList.Builder builder = AssetIssueList.newBuilder();

        dbManager.getAssetIssueStoreFinal().getAllAssetIssues()
                .forEach(issueWrapper -> builder.addAssetIssue(issueWrapper.getInstance()));

        return builder.build();
    }


    public AssetIssueList getAssetIssueList(long offset, long limit) {
        AssetIssueList.Builder builder = AssetIssueList.newBuilder();

        List<AssetIssueWrapper> assetIssueList =
                dbManager.getAssetIssueStoreFinal().getAssetIssuesPaginated(offset, limit);

        if (CollectionUtils.isEmpty(assetIssueList)) {
            return null;
        }

        assetIssueList.forEach(issueWrapper -> builder.addAssetIssue(issueWrapper.getInstance()));
        return builder.build();
    }

    public AssetIssueList getAssetIssueByAccount(ByteString accountAddress) {
        if (accountAddress == null || accountAddress.isEmpty()) {
            return null;
        }

        List<AssetIssueWrapper> assetIssueWrapperList =
                dbManager.getAssetIssueStoreFinal().getAllAssetIssues();

        AssetIssueList.Builder builder = AssetIssueList.newBuilder();
        assetIssueWrapperList.stream()
                .filter(assetIssueWrapper -> assetIssueWrapper.getOwnerAddress().equals(accountAddress))
                .forEach(issueWrapper -> {
                    builder.addAssetIssue(issueWrapper.getInstance());
                });

        return builder.build();
    }

    public AccountNetMessage getAccountNet(ByteString accountAddress) {
        if (accountAddress == null || accountAddress.isEmpty()) {
            return null;
        }
        AccountNetMessage.Builder builder = AccountNetMessage.newBuilder();
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(accountAddress.toByteArray());
        if (accountWrapper == null) {
            return null;
        }

        NetProcessor processor = new NetProcessor(dbManager);
        processor.updateUsage(accountWrapper);

        long netLimit = processor
                .calculateGlobalNetLimit(accountWrapper);
        long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
        long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
        long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

        Map<String, Long> assetNetLimitMap = new HashMap<>();
        Map<String, Long> allFreeAssetNetUsage;
        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            allFreeAssetNetUsage = accountWrapper.getAllFreeAssetNetUsage();
            allFreeAssetNetUsage.keySet().forEach(asset -> {
                byte[] key = ByteArray.fromString(asset);
                assetNetLimitMap
                        .put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
            });
        } else {
            allFreeAssetNetUsage = accountWrapper.getAllFreeAssetNetUsageV2();
            allFreeAssetNetUsage.keySet().forEach(asset -> {
                byte[] key = ByteArray.fromString(asset);
                assetNetLimitMap
                        .put(asset, dbManager.getAssetIssueV2Store().get(key).getFreeAssetNetLimit());
            });
        }

        builder.setFreeNetUsed(accountWrapper.getFreeNetUsage())
                .setFreeNetLimit(freeNetLimit)
                .setNetUsed(accountWrapper.getNetUsage())
                .setNetLimit(netLimit)
                .setTotalNetLimit(totalNetLimit)
                .setTotalNetWeight(totalNetWeight)
                .putAllAssetNetUsed(allFreeAssetNetUsage)
                .putAllAssetNetLimit(assetNetLimitMap);
        return builder.build();
    }

    public AccountResourceMessage getAccountResource(ByteString accountAddress) {
        if (accountAddress == null || accountAddress.isEmpty()) {
            return null;
        }
        AccountResourceMessage.Builder builder = AccountResourceMessage.newBuilder();
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(accountAddress.toByteArray());
        if (accountWrapper == null) {
            return null;
        }

        NetProcessor processor = new NetProcessor(dbManager);
        processor.updateUsage(accountWrapper);

        CpuProcessor cpuProcessor = new CpuProcessor(dbManager);
        cpuProcessor.updateUsage(accountWrapper);

        long netLimit = processor
                .calculateGlobalNetLimit(accountWrapper);
        long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
        long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
        long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
        long cpuLimit = cpuProcessor
                .calculateGlobalCpuLimit(accountWrapper);
        long totalCpuLimit = dbManager.getDynamicPropertiesStore().getTotalCpuCurrentLimit();
        long totalCpuWeight = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();

        long storageLimit = accountWrapper.getAccountResource().getStorageLimit();
        long storageUsage = accountWrapper.getAccountResource().getStorageUsage();

        Map<String, Long> assetNetLimitMap = new HashMap<>();
        Map<String, Long> allFreeAssetNetUsage;
        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            allFreeAssetNetUsage = accountWrapper.getAllFreeAssetNetUsage();
            allFreeAssetNetUsage.keySet().forEach(asset -> {
                byte[] key = ByteArray.fromString(asset);
                assetNetLimitMap
                        .put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
            });
        } else {
            allFreeAssetNetUsage = accountWrapper.getAllFreeAssetNetUsageV2();
            allFreeAssetNetUsage.keySet().forEach(asset -> {
                byte[] key = ByteArray.fromString(asset);
                assetNetLimitMap
                        .put(asset, dbManager.getAssetIssueV2Store().get(key).getFreeAssetNetLimit());
            });
        }

        builder.setFreeNetUsed(accountWrapper.getFreeNetUsage())
                .setFreeNetLimit(freeNetLimit)
                .setNetUsed(accountWrapper.getNetUsage())
                .setNetLimit(netLimit)
                .setTotalNetLimit(totalNetLimit)
                .setTotalNetWeight(totalNetWeight)
                .setCpuLimit(cpuLimit)
                .setCpuUsed(accountWrapper.getAccountResource().getCpuUsage())
                .setTotalCpuLimit(totalCpuLimit)
                .setTotalCpuWeight(totalCpuWeight)
                .setStorageLimit(storageLimit)
                .setStorageUsed(storageUsage)
                .putAllAssetNetUsed(allFreeAssetNetUsage)
                .putAllAssetNetLimit(assetNetLimitMap);
        return builder.build();
    }

    public AssetIssueContract getAssetIssueByName(ByteString assetName)
            throws NonUniqueObjectException {
        if (assetName == null || assetName.isEmpty()) {
            return null;
        }

        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            // fetch from old DB, same as old logic ops
            AssetIssueWrapper assetIssueWrapper =
                    dbManager.getAssetIssueStore().get(assetName.toByteArray());
            return assetIssueWrapper != null ? assetIssueWrapper.getInstance() : null;
        } else {
            // get asset issue by name from new DB
            List<AssetIssueWrapper> assetIssueWrapperList =
                    dbManager.getAssetIssueV2Store().getAllAssetIssues();
            AssetIssueList.Builder builder = AssetIssueList.newBuilder();
            assetIssueWrapperList
                    .stream()
                    .filter(assetIssueWrapper -> assetIssueWrapper.getName().equals(assetName))
                    .forEach(
                            issueWrapper -> {
                                builder.addAssetIssue(issueWrapper.getInstance());
                            });

            // check count
            if (builder.getAssetIssueCount() > 1) {
                throw new NonUniqueObjectException("get more than one asset, please use getassetissuebyid");
            } else {
                // fetch from DB by assetName as id
                AssetIssueWrapper assetIssueWrapper =
                        dbManager.getAssetIssueV2Store().get(assetName.toByteArray());

                if (assetIssueWrapper != null) {
                    // check already fetch
                    if (builder.getAssetIssueCount() > 0
                            && builder.getAssetIssue(0).getId().equals(assetIssueWrapper.getInstance().getId())) {
                        return assetIssueWrapper.getInstance();
                    }

                    builder.addAssetIssue(assetIssueWrapper.getInstance());
                    // check count
                    if (builder.getAssetIssueCount() > 1) {
                        throw new NonUniqueObjectException(
                                "get more than one asset, please use getassetissuebyid");
                    }
                }
            }

            if (builder.getAssetIssueCount() > 0) {
                return builder.getAssetIssue(0);
            } else {
                return null;
            }
        }
    }

    public AssetIssueList getAssetIssueListByName(ByteString assetName) {
        if (assetName == null || assetName.isEmpty()) {
            return null;
        }

        List<AssetIssueWrapper> assetIssueWrapperList =
                dbManager.getAssetIssueStoreFinal().getAllAssetIssues();

        AssetIssueList.Builder builder = AssetIssueList.newBuilder();
        assetIssueWrapperList.stream()
                .filter(assetIssueWrapper -> assetIssueWrapper.getName().equals(assetName))
                .forEach(issueWrapper -> {
                    builder.addAssetIssue(issueWrapper.getInstance());
                });

        return builder.build();
    }

    public AssetIssueContract getAssetIssueById(String assetId) {
        if (assetId == null || assetId.isEmpty()) {
            return null;
        }
        AssetIssueWrapper assetIssueWrapper = dbManager.getAssetIssueV2Store()
                .get(ByteArray.fromString(assetId));
        return assetIssueWrapper != null ? assetIssueWrapper.getInstance() : null;
    }

    public NumberMessage totalTransaction() {
        NumberMessage.Builder builder = NumberMessage.newBuilder()
                .setNum(dbManager.getTransactionStore().getTotalTransactions());
        return builder.build();
    }

    public NumberMessage getNextMaintenanceTime() {
        NumberMessage.Builder builder = NumberMessage.newBuilder()
                .setNum(dbManager.getDynamicPropertiesStore().getNextMaintenanceTime());
        return builder.build();
    }

    public Block getBlockById(ByteString blockId) {
        if (Objects.isNull(blockId)) {
            return null;
        }
        Block block = null;
        try {
            block = dbManager.getBlockStore().get(blockId.toByteArray()).getInstance();
        } catch (StoreException e) {
        }
        return block;
    }

    public BlockList getBlocksByLimitNext(long number, long limit) {
        if (limit <= 0) {
            return null;
        }
        BlockList.Builder blockListBuilder = BlockList.newBuilder();
        dbManager.getBlockStore().getLimitNumber(number, limit).forEach(
                blockWrapper -> blockListBuilder.addBlock(blockWrapper.getInstance()));
        return blockListBuilder.build();
    }

    public BlockList getBlockByLatestNum(long getNum) {
        BlockList.Builder blockListBuilder = BlockList.newBuilder();
        dbManager.getBlockStore().getBlockByLatestNum(getNum).forEach(
                blockWrapper -> blockListBuilder.addBlock(blockWrapper.getInstance()));
        return blockListBuilder.build();
    }

    public Transaction getTransactionById(ByteString transactionId) {
        if (Objects.isNull(transactionId)) {
            return null;
        }
        TransactionWrapper transactionWrapper = null;
        try {
            transactionWrapper = dbManager.getTransactionStore()
                    .get(transactionId.toByteArray());
        } catch (StoreException e) {
            return null;
        }
        if (transactionWrapper != null) {
            return transactionWrapper.getInstance();
        }
        return null;
    }

    public TransactionInfo getTransactionInfoById(ByteString transactionId) {
        if (Objects.isNull(transactionId)) {
            return null;
        }
        TransactionInfoWrapper transactionInfoWrapper;
        try {
            transactionInfoWrapper = dbManager.getTransactionHistoryStore()
                    .get(transactionId.toByteArray());
        } catch (StoreException e) {
            return null;
        }
        if (transactionInfoWrapper != null) {
            return transactionInfoWrapper.getInstance();
        }
        try {
            transactionInfoWrapper = dbManager.getTransactionRetStore()
                    .getTransactionInfo(transactionId.toByteArray());
        } catch (BadItemException e) {
            return null;
        }

        return transactionInfoWrapper == null ? null : transactionInfoWrapper.getInstance();
    }

    public Proposal getProposalById(ByteString proposalId) {
        if (Objects.isNull(proposalId)) {
            return null;
        }
        ProposalWrapper proposalWrapper = null;
        try {
            proposalWrapper = dbManager.getProposalStore()
                    .get(proposalId.toByteArray());
        } catch (StoreException e) {
        }
        if (proposalWrapper != null) {
            return proposalWrapper.getInstance();
        }
        return null;
    }

    public Exchange getExchangeById(ByteString exchangeId) {
        if (Objects.isNull(exchangeId)) {
            return null;
        }
        ExchangeWrapper exchangeWrapper = null;
        try {
            exchangeWrapper = dbManager.getExchangeStoreFinal().get(exchangeId.toByteArray());
        } catch (StoreException e) {
            return null;
        }
        if (exchangeWrapper != null) {
            return exchangeWrapper.getInstance();
        }
        return null;
    }


    public NodeList listNodes() {
        List<NodeHandler> handlerList = nodeManager.dumpActiveNodes();

        Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
        for (NodeHandler handler : handlerList) {
            String key = handler.getNode().getHexId() + handler.getNode().getHost();
            nodeHandlerMap.put(key, handler);
        }

        NodeList.Builder nodeListBuilder = NodeList.newBuilder();

        nodeHandlerMap.entrySet().stream()
                .forEach(v -> {
                    org.gsc.net.node.Node node = v.getValue().getNode();
                    nodeListBuilder.addNodes(Node.newBuilder().setAddress(
                            Address.newBuilder()
                                    .setHost(ByteString.copyFrom(ByteArray.fromString(node.getHost())))
                                    .setPort(node.getPort())));
                });
        return nodeListBuilder.build();
    }

    public Transaction deployContract(CreateSmartContract createSmartContract,
                                      TransactionWrapper trxCap) {

        // do nothing, so can add some useful function later
        // trxcap contract para cacheUnpackValue has value
        return trxCap.getInstance();
    }

    public Transaction triggerContract(TriggerSmartContract triggerSmartContract,
                                       TransactionWrapper trxCap, Builder builder,
                                       Return.Builder retBuilder)
            throws ContractValidateException, ContractExeException, HeaderNotFound, VMIllegalException {

        ContractStore contractStore = dbManager.getContractStore();
        byte[] contractAddress = triggerSmartContract.getContractAddress().toByteArray();
        SmartContract.ABI abi = contractStore.getABI(contractAddress);
        if (abi == null) {
            throw new ContractValidateException("No contract or not a smart contract");
        }

        byte[] selector = getSelector(triggerSmartContract.getData().toByteArray());

        if (isConstant(abi, selector)) {
            return callConstantContract(trxCap, builder, retBuilder);
        } else {
            return trxCap.getInstance();
        }
    }

    public Transaction triggerConstantContract(TriggerSmartContract triggerSmartContract,
                                               TransactionWrapper trxCap, Builder builder,
                                               Return.Builder retBuilder)
            throws ContractValidateException, ContractExeException, HeaderNotFound, VMIllegalException {

        ContractStore contractStore = dbManager.getContractStore();
        byte[] contractAddress = triggerSmartContract.getContractAddress().toByteArray();
        byte[] isContractExiste = contractStore.findContractByHash(contractAddress);

        if (ArrayUtils.isEmpty(isContractExiste)) {
            throw new ContractValidateException("No contract or not a smart contract");
        }

        if (!Args.getInstance().isSupportConstant()) {
            throw new ContractValidateException("this node don't support constant");
        }

        return callConstantContract(trxCap, builder, retBuilder);
    }

    public Transaction callConstantContract(TransactionWrapper trxCap, Builder builder,
                                            Return.Builder retBuilder)
            throws ContractValidateException, ContractExeException, HeaderNotFound, VMIllegalException {

        if (!Args.getInstance().isSupportConstant()) {
            throw new ContractValidateException("this node don't support constant");
        }
        DepositImpl deposit = DepositImpl.createRoot(dbManager);

        Block headBlock;
        List<BlockWrapper> blockWrapperList = dbManager.getBlockStore().getBlockByLatestNum(1);
        if (CollectionUtils.isEmpty(blockWrapperList)) {
            throw new HeaderNotFound("latest block not found");
        } else {
            headBlock = blockWrapperList.get(0).getInstance();
        }

        Runtime runtime = new RuntimeImpl(trxCap.getInstance(), new BlockWrapper(headBlock), deposit,
                new ProgramInvokeFactoryImpl(), true);
//        VMConfig.initVmHardFork();
        VMConfig.initAllowGvmTransferGrc10(
                dbManager.getDynamicPropertiesStore().getAllowGvmTransferGrc10());
        VMConfig.initAllowMultiSign(dbManager.getDynamicPropertiesStore().getAllowMultiSign());
        runtime.execute();
        runtime.go();
        runtime.finalization();
        // TODO exception
        if (runtime.getResult().getException() != null) {
            RuntimeException e = runtime.getResult().getException();
            logger.warn("Constant call has error {}", e.getMessage());
            throw e;
        }

        ProgramResult result = runtime.getResult();
        TransactionResultWrapper ret = new TransactionResultWrapper();

        builder.addConstantResult(ByteString.copyFrom(result.getHReturn()));
        ret.setStatus(0, code.SUCESS);
        if (StringUtils.isNoneEmpty(runtime.getRuntimeError())) {
            ret.setStatus(0, code.FAILED);
            retBuilder.setMessage(ByteString.copyFromUtf8(runtime.getRuntimeError())).build();
        }
        if (runtime.getResult().isRevert()) {
            ret.setStatus(0, code.FAILED);
            retBuilder.setMessage(ByteString.copyFromUtf8("REVERT opcode executed")).build();
        }
        trxCap.setResult(ret);
        return trxCap.getInstance();
    }

    public SmartContract getContract(GrpcAPI.BytesMessage bytesMessage) {
        byte[] address = bytesMessage.getValue().toByteArray();
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
        if (accountWrapper == null) {
            logger.error(
                    "Get contract failed, the account is not exist or the account does not have code hash!");
            return null;
        }

        ContractWrapper contractWrapper = dbManager.getContractStore()
                .get(bytesMessage.getValue().toByteArray());
        if (Objects.nonNull(contractWrapper)) {
            return contractWrapper.getInstance();
        }
        return null;
    }


    private static byte[] getSelector(byte[] data) {
        if (data == null ||
                data.length < 4) {
            return null;
        }

        byte[] ret = new byte[4];
        System.arraycopy(data, 0, ret, 0, 4);
        return ret;
    }

    private static boolean isConstant(SmartContract.ABI abi, byte[] selector) {

        if (selector == null || selector.length != 4 || abi.getEntrysList().size() == 0) {
            return false;
        }

        for (int i = 0; i < abi.getEntrysCount(); i++) {
            ABI.Entry entry = abi.getEntrys(i);
            if (entry.getType() != ABI.Entry.EntryType.Function) {
                continue;
            }

            int inputCount = entry.getInputsCount();
            StringBuffer sb = new StringBuffer();
            sb.append(entry.getName());
            sb.append("(");
            for (int k = 0; k < inputCount; k++) {
                ABI.Entry.Param param = entry.getInputs(k);
                sb.append(param.getType());
                if (k + 1 < inputCount) {
                    sb.append(",");
                }
            }
            sb.append(")");

            byte[] funcSelector = new byte[4];
            System.arraycopy(Hash.sha3(sb.toString().getBytes()), 0, funcSelector, 0, 4);
            if (Arrays.equals(funcSelector, selector)) {
                if (entry.getConstant() == true || entry.getStateMutability()
                        .equals(StateMutabilityType.View)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    /*
    input
    offset:100,limit:10
    return
    id: 101~110
     */
    public ProposalList getPaginatedProposalList(long offset, long limit) {

        if (limit < 0 || offset < 0) {
            return null;
        }

        long latestProposalNum = dbManager.getDynamicPropertiesStore().getLatestProposalNum();
        if (latestProposalNum <= offset) {
            return null;
        }
        limit = limit > PROPOSAL_COUNT_LIMIT_MAX ? PROPOSAL_COUNT_LIMIT_MAX : limit;
        long end = offset + limit;
        end = end > latestProposalNum ? latestProposalNum : end;
        ProposalList.Builder builder = ProposalList.newBuilder();

        ImmutableList<Long> rangeList = ContiguousSet
                .create(Range.openClosed(offset, end), DiscreteDomain.longs()).asList();
        rangeList.stream().map(ProposalWrapper::calculateDbKey).map(key -> {
            try {
                return dbManager.getProposalStore().get(key);
            } catch (Exception ex) {
                return null;
            }
        }).filter(Objects::nonNull)
                .forEach(proposalWrapper -> builder.addProposals(proposalWrapper.getInstance()));
        return builder.build();
    }

    public ExchangeList getPaginatedExchangeList(long offset, long limit) {

        if (limit < 0 || offset < 0) {
            return null;
        }

        long latestExchangeNum = dbManager.getDynamicPropertiesStore().getLatestExchangeNum();
        if (latestExchangeNum <= offset) {
            return null;
        }
        limit = limit > EXCHANGE_COUNT_LIMIT_MAX ? EXCHANGE_COUNT_LIMIT_MAX : limit;
        long end = offset + limit;
        end = end > latestExchangeNum ? latestExchangeNum : end;

        ExchangeList.Builder builder = ExchangeList.newBuilder();
        ImmutableList<Long> rangeList = ContiguousSet
                .create(Range.openClosed(offset, end), DiscreteDomain.longs()).asList();
        rangeList.stream().map(ExchangeWrapper::calculateDbKey).map(key -> {
            try {
                return dbManager.getExchangeStoreFinal().get(key);
            } catch (Exception ex) {
                return null;
            }
        }).filter(Objects::nonNull)
                .forEach(exchangeWrapper -> builder.addExchanges(exchangeWrapper.getInstance()));
        return builder.build();

    }
}
