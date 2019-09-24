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

package org.gsc.runtime.event.wrapper;

import static org.gsc.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.gsc.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.google.protobuf.Any;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.runtime.event.trigger.InternalTransactionPojo;
import org.gsc.runtime.event.trigger.TransactionLogTrigger;
import org.gsc.runtime.vm.program.InternalTransaction;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.TransactionTrace;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol;

@Slf4j
public class TransactionLogTriggerWrapper extends TriggerWrapper {

    @Getter
    @Setter
    TransactionLogTrigger transactionLogTrigger;

    public void setLatestConfirmedBlockNumber(long latestConfirmedBlockNumber) {
        transactionLogTrigger.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
    }

    public TransactionLogTriggerWrapper(TransactionWrapper trxCasule, BlockWrapper blockWrapper) {
        transactionLogTrigger = new TransactionLogTrigger();
        if (Objects.nonNull(blockWrapper)) {
            transactionLogTrigger.setBlockHash(blockWrapper.getBlockId().toString());
        }
        transactionLogTrigger.setTransactionId(trxCasule.getTransactionId().toString());
        transactionLogTrigger.setTimeStamp(blockWrapper.getTimeStamp());
        transactionLogTrigger.setBlockNumber(trxCasule.getBlockNum());

        TransactionTrace trxTrace = trxCasule.getTrxTrace();

        //result
        if (Objects.nonNull(trxCasule.getContractRet())) {
            transactionLogTrigger.setResult(trxCasule.getContractRet().toString());
        }

        if (Objects.nonNull(trxCasule.getInstance().getRawData())) {
            // feelimit
            transactionLogTrigger.setFeeLimit(trxCasule.getInstance().getRawData().getFeeLimit());

            Protocol.Transaction.Contract contract = trxCasule.getInstance().getRawData().getContract(0);
            Any contractParameter = null;
            // contract type
            if (Objects.nonNull(contract)) {
                Protocol.Transaction.Contract.ContractType contractType = contract.getType();
                if (Objects.nonNull(contractType)) {
                    transactionLogTrigger.setContractType(contractType.toString());
                }

                contractParameter = contract.getParameter();

                transactionLogTrigger.setContractCallValue(TransactionWrapper.getCallValue(contract));
            }

            if (Objects.nonNull(contractParameter) && Objects.nonNull(contract)) {
                try {
                    if (contract.getType() == TransferContract) {
                        TransferContract contractTransfer = contractParameter.unpack(TransferContract.class);

                        if (Objects.nonNull(contractTransfer)) {
                            transactionLogTrigger.setAssetName("gsc");

                            if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                                transactionLogTrigger.setFromAddress(
                                        Wallet.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
                            }

                            if (Objects.nonNull(contractTransfer.getToAddress())) {
                                transactionLogTrigger.setToAddress(
                                        Wallet.encode58Check(contractTransfer.getToAddress().toByteArray()));
                            }

                            transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
                        }

                    } else if (contract.getType() == TransferAssetContract) {
                        TransferAssetContract contractTransfer = contractParameter
                                .unpack(TransferAssetContract.class);

                        if (Objects.nonNull(contractTransfer)) {
                            if (Objects.nonNull(contractTransfer.getAssetName())) {
                                transactionLogTrigger.setAssetName(contractTransfer.getAssetName().toStringUtf8());
                            }

                            if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                                transactionLogTrigger.setFromAddress(
                                        Wallet.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
                            }

                            if (Objects.nonNull(contractTransfer.getToAddress())) {
                                transactionLogTrigger.setToAddress(
                                        Wallet.encode58Check(contractTransfer.getToAddress().toByteArray()));
                            }
                            transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
                        }
                    }
                } catch (Exception e) {
                    logger.error("failed to load transferAssetContract, error'{}'", e);
                }
            }
        }

        // receipt
        if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getReceipt())) {
            transactionLogTrigger.setCpuFee(trxTrace.getReceipt().getCpuFee());
            transactionLogTrigger.setOriginCpuUsage(trxTrace.getReceipt().getOriginCpuUsage());
            transactionLogTrigger.setCpuUsageTotal(trxTrace.getReceipt().getCpuUsageTotal());
            transactionLogTrigger.setNetUsage(trxTrace.getReceipt().getNetUsage());
            transactionLogTrigger.setNetFee(trxTrace.getReceipt().getNetFee());
            transactionLogTrigger.setCpuUsage(trxTrace.getReceipt().getCpuUsage());
        }

        // program result
        if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getRuntime()) && Objects.nonNull(trxTrace.getRuntime().getResult())) {
            ProgramResult programResult = trxTrace.getRuntime().getResult();
            ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
            ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

            if (Objects.nonNull(contractResult) && contractResult.size() > 0) {
                transactionLogTrigger.setContractResult(Hex.toHexString(contractResult.toByteArray()));
            }

            if (Objects.nonNull(contractAddress) && contractAddress.size() > 0) {
                transactionLogTrigger
                        .setContractAddress(Wallet.encode58Check((contractAddress.toByteArray())));
            }

            // internal transaction
            transactionLogTrigger.setInternalTrananctionList(
                    getInternalTransactionList(programResult.getInternalTransactions()));
        }
    }

    private List<InternalTransactionPojo> getInternalTransactionList(
            List<InternalTransaction> internalTransactionList) {
        List<InternalTransactionPojo> pojoList = new ArrayList<>();

        internalTransactionList.forEach(internalTransaction -> {
            InternalTransactionPojo item = new InternalTransactionPojo();

            item.setHash(Hex.toHexString(internalTransaction.getHash()));
            item.setCallValue(internalTransaction.getValue());
            item.setTokenInfo(internalTransaction.getTokenInfo());
            item.setCaller_address(Hex.toHexString(internalTransaction.getSender()));
            item.setTransferTo_address(Hex.toHexString(internalTransaction.getTransferToAddress()));
            item.setData(Hex.toHexString(internalTransaction.getData()));
            item.setRejected(internalTransaction.isRejected());
            item.setNote(internalTransaction.getNote());

            pojoList.add(item);
        });

        return pojoList;
    }

    @Override
    public void processTrigger() {
        EventPluginLoader.getInstance().postTransactionTrigger(transactionLogTrigger);
    }
}
