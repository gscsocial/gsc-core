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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.wrapper.ContractWrapper;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.event.trigger.ContractTrigger;
import org.gsc.runtime.utils.MUtil;
import org.gsc.db.dbsource.Deposit;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.SmartContract.ABI;

@Slf4j
public class LogInfoTriggerParser {

    private Long blockNum;
    private Long blockTimestamp;
    private String txId;
    private String originAddress;

    public LogInfoTriggerParser(Long blockNum,
                                Long blockTimestamp,
                                byte[] txId, byte[] originAddress) {

        this.blockNum = blockNum;
        this.blockTimestamp = blockTimestamp;
        this.txId = ArrayUtils.isEmpty(txId) ? "" : Hex.toHexString(txId);
        this.originAddress =
                ArrayUtils.isEmpty(originAddress) ? "" : Wallet.encode58Check(originAddress);

    }

    public List<ContractTrigger> parseLogInfos(List<LogInfo> logInfos, Deposit deposit) {

        List<ContractTrigger> list = new LinkedList<>();
        if (logInfos == null || logInfos.size() <= 0) {
            return list;
        }

        Map<String, String> addrMap = new HashMap<>();
        Map<String, ABI> abiMap = new HashMap<>();

        for (LogInfo logInfo : logInfos) {

            byte[] contractAddress = MUtil.convertToGSCAddress(logInfo.getAddress());
            String strContractAddr =
                    ArrayUtils.isEmpty(contractAddress) ? "" : Wallet.encode58Check(contractAddress);
            if (addrMap.get(strContractAddr) != null) {
                continue;
            }
            ContractWrapper contract = deposit.getContract(contractAddress);
            if (contract == null) {
                // never
                addrMap.put(strContractAddr, originAddress);
                abiMap.put(strContractAddr, ABI.getDefaultInstance());
                continue;
            }
            ABI abi = contract.getInstance().getAbi();
            String creatorAddr = Wallet.encode58Check(
                    MUtil.convertToGSCAddress(contract.getInstance().getOriginAddress().toByteArray()));
            addrMap.put(strContractAddr, creatorAddr);
            abiMap.put(strContractAddr, abi);
        }

        int index = 1;
        for (LogInfo logInfo : logInfos) {

            byte[] contractAddress = MUtil.convertToGSCAddress(logInfo.getAddress());
            String strContractAddr =
                    ArrayUtils.isEmpty(contractAddress) ? "" : Wallet.encode58Check(contractAddress);
            ABI abi = abiMap.get(strContractAddr);
            ContractTrigger event = new ContractTrigger();
            String creatorAddr = addrMap.get(strContractAddr);
            event.setUniqueId(txId + "_" + index);
            event.setTransactionId(txId);
            event.setContractAddress(strContractAddr);
            event.setOriginAddress(originAddress);
            event.setCallerAddress("");
            event.setCreatorAddress(StringUtils.isEmpty(creatorAddr) ? "" : creatorAddr);
            event.setBlockNumber(blockNum);
            event.setTimeStamp(blockTimestamp);
            event.setLogInfo(logInfo);
            event.setAbi(abi);

            list.add(event);
            index++;
        }

        return list;
    }

    public static String getEntrySignature(ABI.Entry entry) {
        String signature = entry.getName() + "(";
        StringBuilder builder = new StringBuilder();
        for (ABI.Entry.Param param : entry.getInputsList()) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(param.getType());
        }
        signature += builder.toString() + ")";
        return signature;
    }
}
