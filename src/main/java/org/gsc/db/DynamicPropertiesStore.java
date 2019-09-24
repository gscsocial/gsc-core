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

package org.gsc.db;

import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BytesWrapper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.config.Parameter;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;

@Slf4j(topic = "DB")
@Component
public class DynamicPropertiesStore extends GSCStoreWithRevoking<BytesWrapper> {

    private static final byte[] LATEST_BLOCK_HEADER_TIMESTAMP = "latest_block_header_timestamp"
            .getBytes();
    private static final byte[] LATEST_BLOCK_HEADER_NUMBER = "latest_block_header_number".getBytes();
    private static final byte[] LATEST_BLOCK_HEADER_HASH = "latest_block_header_hash".getBytes();
    private static final byte[] STATE_FLAG = "state_flag"
            .getBytes(); // 1 : is maintenance, 0 : is not maintenance
    private static final byte[] LATEST_CONFIRMED_BLOCK_NUM = "LATEST_CONFIRMED_BLOCK_NUM"
            .getBytes();

    private static final byte[] LATEST_PROPOSAL_NUM = "LATEST_PROPOSAL_NUM".getBytes();

    private static final byte[] LATEST_EXCHANGE_NUM = "LATEST_EXCHANGE_NUM".getBytes();

    private static final byte[] BLOCK_FILLED_SLOTS = "BLOCK_FILLED_SLOTS".getBytes();

    private static final byte[] BLOCK_FILLED_SLOTS_INDEX = "BLOCK_FILLED_SLOTS_INDEX".getBytes();

    private static final byte[] NEXT_MAINTENANCE_TIME = "NEXT_MAINTENANCE_TIME".getBytes();

    private static final byte[] MAX_FROZEN_TIME = "MAX_FROZEN_TIME".getBytes();

    private static final byte[] MIN_FROZEN_TIME = "MIN_FROZEN_TIME".getBytes();

    private static final byte[] MAX_FROZEN_SUPPLY_NUMBER = "MAX_FROZEN_SUPPLY_NUMBER".getBytes();

    private static final byte[] MAX_FROZEN_SUPPLY_TIME = "MAX_FROZEN_SUPPLY_TIME".getBytes();

    private static final byte[] MIN_FROZEN_SUPPLY_TIME = "MIN_FROZEN_SUPPLY_TIME".getBytes();

    private static final byte[] WITNESS_ALLOWANCE_FROZEN_TIME = "WITNESS_ALLOWANCE_FROZEN_TIME"
            .getBytes();

    private static final byte[] MAINTENANCE_TIME_INTERVAL = "MAINTENANCE_TIME_INTERVAL".getBytes();

    private static final byte[] ACCOUNT_UPGRADE_COST = "ACCOUNT_UPGRADE_COST".getBytes();

    private static final byte[] WITNESS_PAY_PER_BLOCK = "WITNESS_PAY_PER_BLOCK".getBytes();

    private static final byte[] WITNESS_STANDBY_ALLOWANCE = "WITNESS_STANDBY_ALLOWANCE".getBytes();

    private static class DynamicResourceProperties {

        private static final byte[] ONE_DAY_NET_LIMIT = "ONE_DAY_NET_LIMIT".getBytes();
        //public free net
        private static final byte[] PUBLIC_NET_USAGE = "PUBLIC_NET_USAGE".getBytes();
        //fixed
        private static final byte[] PUBLIC_NET_LIMIT = "PUBLIC_NET_LIMIT".getBytes();
        private static final byte[] PUBLIC_NET_TIME = "PUBLIC_NET_TIME".getBytes();
        private static final byte[] FREE_NET_LIMIT = "FREE_NET_LIMIT".getBytes();
        private static final byte[] TOTAL_NET_WEIGHT = "TOTAL_NET_WEIGHT".getBytes();
        // ONE_DAY_NET_LIMIT - PUBLIC_NET_LIMIT，current TOTAL_NET_LIMIT
        private static final byte[] TOTAL_NET_LIMIT = "TOTAL_NET_LIMIT".getBytes();
        private static final byte[] TOTAL_CPU_TARGET_LIMIT = "TOTAL_CPU_TARGET_LIMIT".getBytes();
        private static final byte[] TOTAL_CPU_CURRENT_LIMIT = "TOTAL_CPU_CURRENT_LIMIT"
                .getBytes();
        private static final byte[] TOTAL_CPU_AVERAGE_USAGE = "TOTAL_CPU_AVERAGE_USAGE"
                .getBytes();
        private static final byte[] TOTAL_CPU_AVERAGE_TIME = "TOTAL_CPU_AVERAGE_TIME".getBytes();
        private static final byte[] TOTAL_CPU_WEIGHT = "TOTAL_CPU_WEIGHT".getBytes();
        private static final byte[] TOTAL_CPU_LIMIT = "TOTAL_CPU_LIMIT".getBytes();
        private static final byte[] BLOCK_CPU_USAGE = "BLOCK_CPU_USAGE".getBytes();
    }

    private static final byte[] CPU_FEE = "CPU_FEE".getBytes();

    private static final byte[] MAX_CPU_TIME_OF_ONE_TX = "MAX_CPU_TIME_OF_ONE_TX".getBytes();

    //abandon
    private static final byte[] CREATE_ACCOUNT_FEE = "CREATE_ACCOUNT_FEE".getBytes();

    private static final byte[] CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT
            = "CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT".getBytes();

    private static final byte[] CREATE_NEW_ACCOUNT_NET_RATE =
            "CREATE_NEW_ACCOUNT_NET_RATE"
                    .getBytes();

    private static final byte[] TRANSACTION_FEE = "TRANSACTION_FEE".getBytes(); // 1 byte

    private static final byte[] ASSET_ISSUE_FEE = "ASSET_ISSUE_FEE".getBytes();

    private static final byte[] UPDATE_ACCOUNT_PERMISSION_FEE = "UPDATE_ACCOUNT_PERMISSION_FEE"
            .getBytes();

    private static final byte[] MULTI_SIGN_FEE = "MULTI_SIGN_FEE"
            .getBytes();


    private static final byte[] EXCHANGE_CREATE_FEE = "EXCHANGE_CREATE_FEE".getBytes();

    private static final byte[] EXCHANGE_BALANCE_LIMIT = "EXCHANGE_BALANCE_LIMIT".getBytes();

    private static final byte[] TOTAL_TRANSACTION_COST = "TOTAL_TRANSACTION_COST".getBytes();

    private static final byte[] TOTAL_CREATE_ACCOUNT_COST = "TOTAL_CREATE_ACCOUNT_COST".getBytes();

    private static final byte[] TOTAL_CREATE_WITNESS_COST = "TOTAL_CREATE_WITNESS_FEE".getBytes();

    private static final byte[] TOTAL_STORAGE_POOL = "TOTAL_STORAGE_POOL".getBytes();

    private static final byte[] TOTAL_STORAGE_TAX = "TOTAL_STORAGE_TAX".getBytes();

    private static final byte[] TOTAL_STORAGE_RESERVED = "TOTAL_STORAGE_RESERVED".getBytes();

    private static final byte[] STORAGE_EXCHANGE_TAX_RATE = "STORAGE_EXCHANGE_TAX_RATE".getBytes();

    private static final byte[] FORK_CONTROLLER = "FORK_CONTROLLER".getBytes();
    private static final String FORK_PREFIX = "FORK_VERSION_";

    //This value is only allowed to be 0, 1, -1
    private static final byte[] REMOVE_THE_POWER_OF_THE_GR = "REMOVE_THE_POWER_OF_THE_GR".getBytes();

    //This value is only allowed to be 0, 1, -1
    private static final byte[] ALLOW_DELEGATE_RESOURCE = "ALLOW_DELEGATE_RESOURCE".getBytes();

    //This value is only allowed to be 0, 1, -1
    private static final byte[] ALLOW_ADAPTIVE_CPU = "ALLOW_ADAPTIVE_CPU".getBytes();

    //This value is only allowed to be 0, 1, -1
    private static final byte[] ALLOW_UPDATE_ACCOUNT_NAME = "ALLOW_UPDATE_ACCOUNT_NAME".getBytes();

    //This value is only allowed to be 0, 1, -1
    private static final byte[] ALLOW_SAME_TOKEN_NAME = " ALLOW_SAME_TOKEN_NAME".getBytes();

    //If the parameter is larger than 0, the contract is allowed to be created.
    private static final byte[] ALLOW_CREATION_OF_CONTRACTS = "ALLOW_CREATION_OF_CONTRACTS"
            .getBytes();

    //Used only for multi sign
    private static final byte[] TOTAL_SIGN_NUM = "TOTAL_SIGN_NUM".getBytes();

    //Used only for multi sign, once，value is {0,1}
    private static final byte[] ALLOW_MULTI_SIGN = "ALLOW_MULTI_SIGN".getBytes();

    //token id,Incremental，The initial value is 1000000
    private static final byte[] TOKEN_ID_NUM = "TOKEN_ID_NUM".getBytes();

    //Used only for token updates, once，value is {0,1}
    private static final byte[] TOKEN_UPDATE_DONE = "TOKEN_UPDATE_DONE".getBytes();

    //This value is only allowed to be 0, 1, -1
    private static final byte[] ALLOW_GVM_TRANSFER_GRC10 = "ALLOW_GVM_TRANSFER_GRC10".getBytes();
    private static final byte[] ALLOW_GVM_CONSTANTINOPLE = "ALLOW_GVM_CONSTANTINOPLE".getBytes();

    //Used only for protobuf data filter , once，value is 0,1
    private static final byte[] ALLOW_PROTO_FILTER_NUM = "ALLOW_PROTO_FILTER_NUM"
            .getBytes();

    private static final byte[] AVAILABLE_CONTRACT_TYPE = "AVAILABLE_CONTRACT_TYPE".getBytes();
    private static final byte[] ACTIVE_DEFAULT_OPERATIONS = "ACTIVE_DEFAULT_OPERATIONS".getBytes();
    //Used only for account state root, once，value is {0,1} allow is 1
    private static final byte[] ALLOW_ACCOUNT_STATE_ROOT = "ALLOW_ACCOUNT_STATE_ROOT".getBytes();

    @Autowired
    private DynamicPropertiesStore(@Value("dynamic_parameter") String dbName) {
        super(dbName);

        try {
            this.getTotalSignNum();
        } catch (IllegalArgumentException e) {
            this.saveTotalSignNum(5);
        }

        try {
            this.getAllowMultiSign();
        } catch (IllegalArgumentException e) {
            this.saveAllowMultiSign(Args.getInstance().getAllowMultiSign());
        }

        try {
            this.getLatestBlockHeaderTimestamp();
        } catch (IllegalArgumentException e) {
            this.saveLatestBlockHeaderTimestamp(0);
        }

        try {
            this.getLatestBlockHeaderNumber();
        } catch (IllegalArgumentException e) {
            this.saveLatestBlockHeaderNumber(0);
        }

        try {
            this.getLatestBlockHeaderHash();
        } catch (IllegalArgumentException e) {
            this.saveLatestBlockHeaderHash(ByteString.copyFrom(ByteArray.fromHexString("00")));
        }

        try {
            this.getStateFlag();
        } catch (IllegalArgumentException e) {
            this.saveStateFlag(0);
        }

        try {
            this.getLatestConfirmedBlockNum();
        } catch (IllegalArgumentException e) {
            this.saveLatestConfirmedBlockNum(0);
        }

        try {
            this.getLatestProposalNum();
        } catch (IllegalArgumentException e) {
            this.saveLatestProposalNum(0);
        }

        try {
            this.getLatestExchangeNum();
        } catch (IllegalArgumentException e) {
            this.saveLatestExchangeNum(0);
        }

        try {
            this.getBlockFilledSlotsIndex();
        } catch (IllegalArgumentException e) {
            this.saveBlockFilledSlotsIndex(0);
        }

        try {
            this.getTokenIdNum();
        } catch (IllegalArgumentException e) {
            this.saveTokenIdNum(1000000L);
        }

        try {
            this.getTokenUpdateDone();
        } catch (IllegalArgumentException e) {
            this.saveTokenUpdateDone(0);
        }

        try {
            this.getMaxFrozenTime();
        } catch (IllegalArgumentException e) {
            this.saveMaxFrozenTime(5);
        }

        try {
            this.getMinFrozenTime();
        } catch (IllegalArgumentException e) {
            this.saveMinFrozenTime(5);
        }

        try {
            this.getMaxFrozenSupplyNumber();
        } catch (IllegalArgumentException e) {
            this.saveMaxFrozenSupplyNumber(15); // 10 -> 15
        }

        try {
            this.getMaxFrozenSupplyTime();
        } catch (IllegalArgumentException e) {
            this.saveMaxFrozenSupplyTime(36525);
        }

        try {
            this.getMinFrozenSupplyTime();
        } catch (IllegalArgumentException e) {
            this.saveMinFrozenSupplyTime(1);
        }

        try {
            this.getWitnessAllowanceFrozenTime();
        } catch (IllegalArgumentException e) {
            this.saveWitnessAllowanceFrozenTime(1);
        }

        try {
            this.getWitnessPayPerBlock();
        } catch (IllegalArgumentException e) {
            this.saveWitnessPayPerBlock(3_805_175L);  // 32GSC to 3805175dot by kay 7/16/2019
        }

        try {
            this.getWitnessStandbyAllowance();
        } catch (IllegalArgumentException e) {
            this.saveWitnessStandbyAllowance(13_698_630_136L);  // 13698630136 by kay 7/16/2019
        }

        try {
            this.getMaintenanceTimeInterval();
        } catch (IllegalArgumentException e) {
            this.saveMaintenanceTimeInterval(Args.getInstance().getMaintenanceTimeInterval()); // 6 hours
        }

        try {
            this.getAccountUpgradeCost();
        } catch (IllegalArgumentException e) {
            this.saveAccountUpgradeCost(10_000_000_000L); // adjust 9999 to 10000 by kay 7/16/2019
        }

        try {
            this.getPublicNetUsage();
        } catch (IllegalArgumentException e) {
            this.savePublicNetUsage(0L);
        }

        try {
            this.getOneDayNetLimit();
        } catch (IllegalArgumentException e) {
            this.saveOneDayNetLimit(69_120_000_000L);
        }

        try {
            this.getPublicNetLimit();
        } catch (IllegalArgumentException e) {
            this.savePublicNetLimit(17_280_000_000L);
        }

        try {
            this.getPublicNetTime();
        } catch (IllegalArgumentException e) {
            this.savePublicNetTime(0L);
        }

        try {
            this.getFreeNetLimit();
        } catch (IllegalArgumentException e) {
            this.saveFreeNetLimit(4000L);
        }

        try {
            this.getTotalNetWeight();
        } catch (IllegalArgumentException e) {
            this.saveTotalNetWeight(0L);
        }

        try {
            this.getTotalNetLimit();
        } catch (IllegalArgumentException e) {
            this.saveTotalNetLimit(51_840_000_000L);
        }

        try {
            this.getTotalCpuWeight();
        } catch (IllegalArgumentException e) {
            this.saveTotalCpuWeight(0L);
        }

        try {
            this.getAllowAdaptiveCpu();
        } catch (IllegalArgumentException e) {
            this.saveAllowAdaptiveCpu(Args.getInstance().getAllowAdaptiveCpu());
        }

        try {
            this.getTotalCpuLimit();
        } catch (IllegalArgumentException e) {
            this.saveTotalCpuLimit(12_500_000_000L);
        }

        try {
            this.getCpuFee();
        } catch (IllegalArgumentException e) {
            this.saveCpuFee(10L);  // 10 dot per cpu by kay 7/16/2019
        }

        try {
            this.getMaxCpuTimeOfOneTx();
        } catch (IllegalArgumentException e) {
            this.saveMaxCpuTimeOfOneTx(50L);
        }

        try {
            this.getCreateAccountFee();
        } catch (IllegalArgumentException e) {
            this.saveCreateAccountFee(100_000L); // 0.1 GSC
        }

        try {
            this.getCreateNewAccountFeeInSystemContract();
        } catch (IllegalArgumentException e) {
            this.saveCreateNewAccountFeeInSystemContract(0L); //changed by committee later
        }

        try {
            this.getCreateNewAccountNetRate();
        } catch (IllegalArgumentException e) {
            this.saveCreateNewAccountNetRate(1L); //changed by committee later
        }

        try {
            this.getTransactionFee();
        } catch (IllegalArgumentException e) {
            this.saveTransactionFee(10L); // 10 gsc/byte
        }

        try {
            this.getAssetIssueFee();
        } catch (IllegalArgumentException e) {
            this.saveAssetIssueFee(1000000000L); //adjust 1024 to 1000 by kay 7/16/2019
        }

        try {
            this.getUpdateAccountPermissionFee();
        } catch (IllegalArgumentException e) {
            this.saveUpdateAccountPermissionFee(100000000L);
        }

        try {
            this.getMultiSignFee();
        } catch (IllegalArgumentException e) {
            this.saveMultiSignFee(1000000L);
        }

        try {
            this.getExchangeCreateFee();
        } catch (IllegalArgumentException e) {
            this.saveExchangeCreateFee(1000000000L); //adjust 1024 to 1000 by kay 7/16/2019
        }

        try {
            this.getExchangeBalanceLimit();
        } catch (IllegalArgumentException e) {
            this.saveExchangeBalanceLimit(1_00_000_000_000_000L);
        }

        try {
            this.getTotalTransactionCost();
        } catch (IllegalArgumentException e) {
            this.saveTotalTransactionCost(0L);
        }

        try {
            this.getTotalCreateWitnessCost();
        } catch (IllegalArgumentException e) {
            this.saveTotalCreateWitnessFee(0L);
        }

        try {
            this.getTotalCreateAccountCost();
        } catch (IllegalArgumentException e) {
            this.saveTotalCreateAccountFee(0L);
        }

        try {
            this.getTotalStoragePool();
        } catch (IllegalArgumentException e) {
            this.saveTotalStoragePool(100_000_000_000_000L);
        }

        try {
            this.getTotalStorageTax();
        } catch (IllegalArgumentException e) {
            this.saveTotalStorageTax(0);
        }

        try {
            this.getTotalStorageReserved();
        } catch (IllegalArgumentException e) {
            this.saveTotalStorageReserved(128L * 1024 * 1024 * 1024); // 137438953472 bytes
        }

        try {
            this.getStorageExchangeTaxRate();
        } catch (IllegalArgumentException e) {
            this.saveStorageExchangeTaxRate(10);
        }

        try {
            this.getRemoveThePowerOfTheGr();
        } catch (IllegalArgumentException e) {
            this.saveRemoveThePowerOfTheGr(0);
        }

        try {
            this.getAllowDelegateResource();
        } catch (IllegalArgumentException e) {
            this.saveAllowDelegateResource(Args.getInstance().getAllowDelegateResource());
        }

        try {
            this.getAllowGvmTransferGrc10();
        } catch (IllegalArgumentException e) {
            this.saveAllowGvmTransferGrc10(Args.getInstance().getAllowGvmTransferGrc10());
        }

        try {
            this.getAllowGvmConstantinople();
        } catch (IllegalArgumentException e) {
            this.saveAllowGvmConstantinople(Args.getInstance().getAllowGvmConstantinople());
        }
        try {
            this.getAvailableContractType();
        } catch (IllegalArgumentException e) {
            String contractType = "7fff1fc0037e0000000000000000000000000000000000000000000000000000";
            byte[] bytes = ByteArray.fromHexString(contractType);
            this.saveAvailableContractType(bytes);
        }

        try {
            this.getActiveDefaultOperations();
        } catch (IllegalArgumentException e) {
            String contractType = "7fff1fc0033e0000000000000000000000000000000000000000000000000000";
            byte[] bytes = ByteArray.fromHexString(contractType);
            this.saveActiveDefaultOperations(bytes);
        }

        try {
            this.getAllowSameTokenName();
        } catch (IllegalArgumentException e) {
            this.saveAllowSameTokenName(Args.getInstance().getAllowSameTokenName());
        }

        try {
            this.getAllowUpdateAccountName();
        } catch (IllegalArgumentException e) {
            this.saveAllowUpdateAccountName(Args.getInstance().getAllowUpdateAccountName());
        }

        try {
            this.getAllowCreationOfContracts();
        } catch (IllegalArgumentException e) {
            this.saveAllowCreationOfContracts(Args.getInstance().getAllowCreationOfContracts());
        }

        try {
            this.getBlockFilledSlots();
        } catch (IllegalArgumentException e) {
            int[] blockFilledSlots = new int[getBlockFilledSlotsNumber()];
            Arrays.fill(blockFilledSlots, 1);
            this.saveBlockFilledSlots(blockFilledSlots);
        }

        try {
            this.getNextMaintenanceTime();
        } catch (IllegalArgumentException e) {
            this.saveNextMaintenanceTime(
                    Long.parseLong(Args.getInstance().getGenesisBlock().getTimestamp()));
        }

        try {
            this.getTotalCpuCurrentLimit();
        } catch (IllegalArgumentException e) {
            this.saveTotalCpuCurrentLimit(getTotalCpuLimit());
        }

        try {
            this.getTotalCpuTargetLimit();
        } catch (IllegalArgumentException e) {
            this.saveTotalCpuTargetLimit(getTotalCpuLimit() / 14400);
        }

        try {
            this.getTotalCpuAverageUsage();
        } catch (IllegalArgumentException e) {
            this.saveTotalCpuAverageUsage(0);
        }

        try {
            this.getTotalCpuAverageTime();
        } catch (IllegalArgumentException e) {
            this.saveTotalCpuAverageTime(0);
        }

        try {
            this.getBlockCpuUsage();
        } catch (IllegalArgumentException e) {
            this.saveBlockCpuUsage(0);
        }

        try {
            this.getAllowAccountStateRoot();
        } catch (IllegalArgumentException e) {
            this.saveAllowAccountStateRoot(Args.getInstance().getAllowAccountStateRoot());
        }

        try {
            this.getAllowProtoFilterNum();
        } catch (IllegalArgumentException e) {
            this.saveAllowProtoFilterNum(Args.getInstance().getAllowProtoFilterNum());
        }
    }

    public String intArrayToString(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i : a) {
            sb.append(i);
        }
        return sb.toString();
    }

    public int[] stringToIntArray(String s) {
        int length = s.length();
        int[] result = new int[length];
        for (int i = 0; i < length; ++i) {
            result[i] = Integer.parseInt(s.substring(i, i + 1));
        }
        return result;
    }


    public void saveTokenIdNum(long num) {
        this.put(TOKEN_ID_NUM,
                new BytesWrapper(ByteArray.fromLong(num)));
    }

    public long getTokenIdNum() {
        return Optional.ofNullable(getUnchecked(TOKEN_ID_NUM))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOKEN_ID_NUM"));
    }

    public void saveTokenUpdateDone(long num) {
        this.put(TOKEN_UPDATE_DONE,
                new BytesWrapper(ByteArray.fromLong(num)));
    }

    public long getTokenUpdateDone() {
        return Optional.ofNullable(getUnchecked(TOKEN_UPDATE_DONE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOKEN_UPDATE_DONE"));
    }


    public void saveBlockFilledSlotsIndex(int blockFilledSlotsIndex) {
        logger.debug("blockFilledSlotsIndex:" + blockFilledSlotsIndex);
        this.put(BLOCK_FILLED_SLOTS_INDEX,
                new BytesWrapper(ByteArray.fromInt(blockFilledSlotsIndex)));
    }

    public int getBlockFilledSlotsIndex() {
        return Optional.ofNullable(getUnchecked(BLOCK_FILLED_SLOTS_INDEX))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found BLOCK_FILLED_SLOTS_INDEX"));
    }

    public void saveMaxFrozenTime(int maxFrozenTime) {
        logger.debug("MAX_FROZEN_NUMBER:" + maxFrozenTime);
        this.put(MAX_FROZEN_TIME,
                new BytesWrapper(ByteArray.fromInt(maxFrozenTime)));
    }

    public int getMaxFrozenTime() {
        return Optional.ofNullable(getUnchecked(MAX_FROZEN_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MAX_FROZEN_TIME"));
    }

    public void saveMinFrozenTime(int minFrozenTime) {
        logger.debug("MIN_FROZEN_NUMBER:" + minFrozenTime);
        this.put(MIN_FROZEN_TIME,
                new BytesWrapper(ByteArray.fromInt(minFrozenTime)));
    }

    public int getMinFrozenTime() {
        return Optional.ofNullable(getUnchecked(MIN_FROZEN_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MIN_FROZEN_TIME"));
    }

    public void saveMaxFrozenSupplyNumber(int maxFrozenSupplyNumber) {
        logger.debug("MAX_FROZEN_SUPPLY_NUMBER:" + maxFrozenSupplyNumber);
        this.put(MAX_FROZEN_SUPPLY_NUMBER,
                new BytesWrapper(ByteArray.fromInt(maxFrozenSupplyNumber)));
    }

    public int getMaxFrozenSupplyNumber() {
        return Optional.ofNullable(getUnchecked(MAX_FROZEN_SUPPLY_NUMBER))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MAX_FROZEN_SUPPLY_NUMBER"));
    }

    public void saveMaxFrozenSupplyTime(int maxFrozenSupplyTime) {
        logger.debug("MAX_FROZEN_SUPPLY_NUMBER:" + maxFrozenSupplyTime);
        this.put(MAX_FROZEN_SUPPLY_TIME,
                new BytesWrapper(ByteArray.fromInt(maxFrozenSupplyTime)));
    }

    public int getMaxFrozenSupplyTime() {
        return Optional.ofNullable(getUnchecked(MAX_FROZEN_SUPPLY_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MAX_FROZEN_SUPPLY_TIME"));
    }

    public void saveMinFrozenSupplyTime(int minFrozenSupplyTime) {
        logger.debug("MIN_FROZEN_SUPPLY_NUMBER:" + minFrozenSupplyTime);
        this.put(MIN_FROZEN_SUPPLY_TIME,
                new BytesWrapper(ByteArray.fromInt(minFrozenSupplyTime)));
    }

    public int getMinFrozenSupplyTime() {
        return Optional.ofNullable(getUnchecked(MIN_FROZEN_SUPPLY_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MIN_FROZEN_SUPPLY_TIME"));
    }

    public void saveWitnessAllowanceFrozenTime(int witnessAllowanceFrozenTime) {
        logger.debug("WITNESS_ALLOWANCE_FROZEN_TIME:" + witnessAllowanceFrozenTime);
        this.put(WITNESS_ALLOWANCE_FROZEN_TIME,
                new BytesWrapper(ByteArray.fromInt(witnessAllowanceFrozenTime)));
    }

    public int getWitnessAllowanceFrozenTime() {
        return Optional.ofNullable(getUnchecked(WITNESS_ALLOWANCE_FROZEN_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found WITNESS_ALLOWANCE_FROZEN_TIME"));
    }

    public void saveMaintenanceTimeInterval(long timeInterval) {
        logger.debug("MAINTENANCE_TIME_INTERVAL:" + timeInterval);
        this.put(MAINTENANCE_TIME_INTERVAL,
                new BytesWrapper(ByteArray.fromLong(timeInterval)));
    }

    public long getMaintenanceTimeInterval() {
        return Optional.ofNullable(getUnchecked(MAINTENANCE_TIME_INTERVAL))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MAINTENANCE_TIME_INTERVAL"));
    }

    public void saveAccountUpgradeCost(long accountUpgradeCost) {
        logger.debug("ACCOUNT_UPGRADE_COST:" + accountUpgradeCost);
        this.put(ACCOUNT_UPGRADE_COST,
                new BytesWrapper(ByteArray.fromLong(accountUpgradeCost)));
    }

    public long getAccountUpgradeCost() {
        return Optional.ofNullable(getUnchecked(ACCOUNT_UPGRADE_COST))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ACCOUNT_UPGRADE_COST"));
    }

    public void saveWitnessPayPerBlock(long pay) {
        logger.debug("WITNESS_PAY_PER_BLOCK:" + pay);
        this.put(WITNESS_PAY_PER_BLOCK,
                new BytesWrapper(ByteArray.fromLong(pay)));
    }

    public long getWitnessPayPerBlock() {
        return Optional.ofNullable(getUnchecked(WITNESS_PAY_PER_BLOCK))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found WITNESS_PAY_PER_BLOCK"));
    }

    public void saveWitnessStandbyAllowance(long allowance) {
        logger.debug("WITNESS_STANDBY_ALLOWANCE:" + allowance);
        this.put(WITNESS_STANDBY_ALLOWANCE,
                new BytesWrapper(ByteArray.fromLong(allowance)));
    }

    public long getWitnessStandbyAllowance() {
        return Optional.ofNullable(getUnchecked(WITNESS_STANDBY_ALLOWANCE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found WITNESS_STANDBY_ALLOWANCE"));
    }

    public void saveOneDayNetLimit(long oneDayNetLimit) {
        this.put(DynamicResourceProperties.ONE_DAY_NET_LIMIT,
                new BytesWrapper(ByteArray.fromLong(oneDayNetLimit)));
    }

    public long getOneDayNetLimit() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.ONE_DAY_NET_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ONE_DAY_NET_LIMIT"));
    }

    public void savePublicNetUsage(long publicNetUsage) {
        this.put(DynamicResourceProperties.PUBLIC_NET_USAGE,
                new BytesWrapper(ByteArray.fromLong(publicNetUsage)));
    }

    public long getPublicNetUsage() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.PUBLIC_NET_USAGE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found PUBLIC_NET_USAGE"));
    }

    public void savePublicNetLimit(long publicNetLimit) {
        this.put(DynamicResourceProperties.PUBLIC_NET_LIMIT,
                new BytesWrapper(ByteArray.fromLong(publicNetLimit)));
    }

    public long getPublicNetLimit() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.PUBLIC_NET_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found PUBLIC_NET_LIMIT"));
    }

    public void savePublicNetTime(long publicNetTime) {
        this.put(DynamicResourceProperties.PUBLIC_NET_TIME,
                new BytesWrapper(ByteArray.fromLong(publicNetTime)));
    }

    public long getPublicNetTime() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.PUBLIC_NET_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found PUBLIC_NET_TIME"));
    }

    public void saveFreeNetLimit(long freeNetLimit) {
        this.put(DynamicResourceProperties.FREE_NET_LIMIT,
                new BytesWrapper(ByteArray.fromLong(freeNetLimit)));
    }

    public long getFreeNetLimit() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.FREE_NET_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found FREE_NET_LIMIT"));
    }

    public void saveTotalNetWeight(long totalNetWeight) {
        this.put(DynamicResourceProperties.TOTAL_NET_WEIGHT,
                new BytesWrapper(ByteArray.fromLong(totalNetWeight)));
    }

    public long getTotalNetWeight() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_NET_WEIGHT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_NET_WEIGHT"));
    }

    public void saveTotalCpuWeight(long totalCpuWeight) {
        this.put(DynamicResourceProperties.TOTAL_CPU_WEIGHT,
                new BytesWrapper(ByteArray.fromLong(totalCpuWeight)));
    }

    public long getTotalCpuWeight() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_CPU_WEIGHT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_CPU_WEIGHT"));
    }


    public void saveTotalNetLimit(long totalNetLimit) {
        this.put(DynamicResourceProperties.TOTAL_NET_LIMIT,
                new BytesWrapper(ByteArray.fromLong(totalNetLimit)));
    }

    public long getTotalNetLimit() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_NET_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_NET_LIMIT"));
    }

    public void saveTotalCpuLimit(long totalCpuLimit) {
        this.put(DynamicResourceProperties.TOTAL_CPU_LIMIT,
                new BytesWrapper(ByteArray.fromLong(totalCpuLimit)));

        saveTotalCpuTargetLimit(totalCpuLimit / 14400);
    }

    public void saveTotalCpuLimit2(long totalCpuLimit) {
        this.put(DynamicResourceProperties.TOTAL_CPU_LIMIT,
                new BytesWrapper(ByteArray.fromLong(totalCpuLimit)));

        saveTotalCpuTargetLimit(totalCpuLimit / 14400);
        if (getAllowAdaptiveCpu() == 0) {
            saveTotalCpuCurrentLimit(totalCpuLimit);
        }
    }

    public long getTotalCpuLimit() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_CPU_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_CPU_LIMIT"));
    }

    public void saveTotalCpuCurrentLimit(long totalCpuCurrentLimit) {
        this.put(DynamicResourceProperties.TOTAL_CPU_CURRENT_LIMIT,
                new BytesWrapper(ByteArray.fromLong(totalCpuCurrentLimit)));
    }

    public long getTotalCpuCurrentLimit() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_CPU_CURRENT_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_CPU_CURRENT_LIMIT"));
    }

    public void saveTotalCpuTargetLimit(long targetTotalCpuLimit) {
        this.put(DynamicResourceProperties.TOTAL_CPU_TARGET_LIMIT,
                new BytesWrapper(ByteArray.fromLong(targetTotalCpuLimit)));
    }

    public long getTotalCpuTargetLimit() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_CPU_TARGET_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_CPU_TARGET_LIMIT"));
    }

    public void saveTotalCpuAverageUsage(long totalCpuAverageUsage) {
        this.put(DynamicResourceProperties.TOTAL_CPU_AVERAGE_USAGE,
                new BytesWrapper(ByteArray.fromLong(totalCpuAverageUsage)));
    }

    public long getTotalCpuAverageUsage() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_CPU_AVERAGE_USAGE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_CPU_AVERAGE_USAGE"));
    }

    public void saveTotalCpuAverageTime(long totalCpuAverageTime) {
        this.put(DynamicResourceProperties.TOTAL_CPU_AVERAGE_TIME,
                new BytesWrapper(ByteArray.fromLong(totalCpuAverageTime)));
    }

    public long getTotalCpuAverageTime() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_CPU_AVERAGE_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_NET_AVERAGE_TIME"));
    }

    public void saveBlockCpuUsage(long blockCpuUsage) {
        this.put(DynamicResourceProperties.BLOCK_CPU_USAGE,
                new BytesWrapper(ByteArray.fromLong(blockCpuUsage)));
    }

    public long getBlockCpuUsage() {
        return Optional.ofNullable(getUnchecked(DynamicResourceProperties.BLOCK_CPU_USAGE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found BLOCK_CPU_USAGE"));
    }

    public void saveCpuFee(long totalCpuFee) {
        this.put(CPU_FEE,
                new BytesWrapper(ByteArray.fromLong(totalCpuFee)));
    }

    public long getCpuFee() {
        return Optional.ofNullable(getUnchecked(CPU_FEE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found CPU_FEE"));
    }

    public void saveMaxCpuTimeOfOneTx(long time) {
        this.put(MAX_CPU_TIME_OF_ONE_TX,
                new BytesWrapper(ByteArray.fromLong(time)));
    }

    public long getMaxCpuTimeOfOneTx() {
        return Optional.ofNullable(getUnchecked(MAX_CPU_TIME_OF_ONE_TX))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MAX_CPU_TIME_OF_ONE_TX"));
    }

    public void saveCreateAccountFee(long fee) {
        this.put(CREATE_ACCOUNT_FEE,
                new BytesWrapper(ByteArray.fromLong(fee)));
    }

    public long getCreateAccountFee() {
        return Optional.ofNullable(getUnchecked(CREATE_ACCOUNT_FEE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found CREATE_ACCOUNT_FEE"));
    }

    public void saveCreateNewAccountFeeInSystemContract(long fee) {
        this.put(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT,
                new BytesWrapper(ByteArray.fromLong(fee)));
    }

    public long getCreateNewAccountFeeInSystemContract() {
        return Optional.ofNullable(getUnchecked(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "not found CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT"));
    }

    public void saveCreateNewAccountNetRate(long rate) {
        this.put(CREATE_NEW_ACCOUNT_NET_RATE,
                new BytesWrapper(ByteArray.fromLong(rate)));
    }

    public long getCreateNewAccountNetRate() {
        return Optional.ofNullable(getUnchecked(CREATE_NEW_ACCOUNT_NET_RATE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "not found CREATE_NsEW_ACCOUNT_NET_RATE2"));
    }

    public void saveTransactionFee(long fee) {
        this.put(TRANSACTION_FEE,
                new BytesWrapper(ByteArray.fromLong(fee)));
    }

    public long getTransactionFee() {
        return Optional.ofNullable(getUnchecked(TRANSACTION_FEE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TRANSACTION_FEE"));
    }

    public void saveAssetIssueFee(long fee) {
        this.put(ASSET_ISSUE_FEE,
                new BytesWrapper(ByteArray.fromLong(fee)));
    }

    public void saveUpdateAccountPermissionFee(long fee) {
        this.put(UPDATE_ACCOUNT_PERMISSION_FEE,
                new BytesWrapper(ByteArray.fromLong(fee)));
    }

    public void saveMultiSignFee(long fee) {
        this.put(MULTI_SIGN_FEE,
                new BytesWrapper(ByteArray.fromLong(fee)));
    }


    public long getAssetIssueFee() {
        return Optional.ofNullable(getUnchecked(ASSET_ISSUE_FEE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ASSET_ISSUE_FEE"));
    }

    public long getUpdateAccountPermissionFee() {
        return Optional.ofNullable(getUnchecked(UPDATE_ACCOUNT_PERMISSION_FEE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found UPDATE_ACCOUNT_PERMISSION_FEE"));
    }

    public long getMultiSignFee() {
        return Optional.ofNullable(getUnchecked(MULTI_SIGN_FEE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found MULTI_SIGN_FEE"));
    }


    public void saveExchangeCreateFee(long fee) {
        this.put(EXCHANGE_CREATE_FEE,
                new BytesWrapper(ByteArray.fromLong(fee)));
    }

    public long getExchangeCreateFee() {
        return Optional.ofNullable(getUnchecked(EXCHANGE_CREATE_FEE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found EXCHANGE_CREATE_FEE"));
    }

    public void saveExchangeBalanceLimit(long limit) {
        this.put(EXCHANGE_BALANCE_LIMIT,
                new BytesWrapper(ByteArray.fromLong(limit)));
    }

    public long getExchangeBalanceLimit() {
        return Optional.ofNullable(getUnchecked(EXCHANGE_BALANCE_LIMIT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found EXCHANGE_BALANCE_LIMIT"));
    }

    public void saveTotalTransactionCost(long value) {
        this.put(TOTAL_TRANSACTION_COST,
                new BytesWrapper(ByteArray.fromLong(value)));
    }

    public long getTotalTransactionCost() {
        return Optional.ofNullable(getUnchecked(TOTAL_TRANSACTION_COST))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_TRANSACTION_COST"));
    }

    public void saveTotalCreateAccountFee(long value) {
        this.put(TOTAL_CREATE_ACCOUNT_COST,
                new BytesWrapper(ByteArray.fromLong(value)));
    }

    public long getTotalCreateAccountCost() {
        return Optional.ofNullable(getUnchecked(TOTAL_CREATE_ACCOUNT_COST))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_CREATE_ACCOUNT_COST"));
    }

    public void saveTotalCreateWitnessFee(long value) {
        this.put(TOTAL_CREATE_WITNESS_COST,
                new BytesWrapper(ByteArray.fromLong(value)));
    }

    public long getTotalCreateWitnessCost() {
        return Optional.ofNullable(getUnchecked(TOTAL_CREATE_WITNESS_COST))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_CREATE_WITNESS_COST"));
    }

    public void saveTotalStoragePool(long gsc) {
        this.put(TOTAL_STORAGE_POOL,
                new BytesWrapper(ByteArray.fromLong(gsc)));
    }

    public long getTotalStoragePool() {
        return Optional.ofNullable(getUnchecked(TOTAL_STORAGE_POOL))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_STORAGE_POOL"));
    }

    public void saveTotalStorageTax(long gsc) {
        this.put(TOTAL_STORAGE_TAX,
                new BytesWrapper(ByteArray.fromLong(gsc)));
    }

    public long getTotalStorageTax() {
        return Optional.ofNullable(getUnchecked(TOTAL_STORAGE_TAX))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_STORAGE_TAX"));
    }

    public void saveTotalStorageReserved(long bytes) {
        this.put(TOTAL_STORAGE_RESERVED,
                new BytesWrapper(ByteArray.fromLong(bytes)));
    }

    public long getTotalStorageReserved() {
        return Optional.ofNullable(getUnchecked(TOTAL_STORAGE_RESERVED))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_STORAGE_RESERVED"));
    }

    public void saveStorageExchangeTaxRate(long rate) {
        this.put(STORAGE_EXCHANGE_TAX_RATE,
                new BytesWrapper(ByteArray.fromLong(rate)));
    }

    public long getStorageExchangeTaxRate() {
        return Optional.ofNullable(getUnchecked(STORAGE_EXCHANGE_TAX_RATE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found STORAGE_EXCHANGE_TAX_RATE"));
    }

    public void saveRemoveThePowerOfTheGr(long rate) {
        this.put(REMOVE_THE_POWER_OF_THE_GR,
                new BytesWrapper(ByteArray.fromLong(rate)));
    }

    public long getRemoveThePowerOfTheGr() {
        return Optional.ofNullable(getUnchecked(REMOVE_THE_POWER_OF_THE_GR))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found REMOVE_THE_POWER_OF_THE_GR"));
    }

    public void saveAllowDelegateResource(long value) {
        this.put(ALLOW_DELEGATE_RESOURCE,
                new BytesWrapper(ByteArray.fromLong(value)));
    }

    public long getAllowDelegateResource() {
        return Optional.ofNullable(getUnchecked(ALLOW_DELEGATE_RESOURCE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_DELEGATE_RESOURCE"));
    }

    public void saveAllowAdaptiveCpu(long value) {
        this.put(ALLOW_ADAPTIVE_CPU,
                new BytesWrapper(ByteArray.fromLong(value)));
    }

    public long getAllowAdaptiveCpu() {
        return Optional.ofNullable(getUnchecked(ALLOW_ADAPTIVE_CPU))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_ADAPTIVE_CPU"));
    }

    public void saveAllowGvmTransferGrc10(long value) {
        this.put(ALLOW_GVM_TRANSFER_GRC10,
                new BytesWrapper(ByteArray.fromLong(value)));
    }

    public long getAllowGvmTransferGrc10() {
        return Optional.ofNullable(getUnchecked(ALLOW_GVM_TRANSFER_GRC10))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_GVM_TRANSFER_GRC10"));
    }

    public void saveAllowGvmConstantinople(long value) {
        this.put(ALLOW_GVM_CONSTANTINOPLE,
                new BytesWrapper(ByteArray.fromLong(value)));
    }

    public long getAllowGvmConstantinople() {
        return Optional.ofNullable(getUnchecked(ALLOW_GVM_CONSTANTINOPLE))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_GVM_CONSTANTINOPLE"));
    }

    public void saveAvailableContractType(byte[] value) {
        this.put(AVAILABLE_CONTRACT_TYPE,
                new BytesWrapper(value));
    }

    public byte[] getAvailableContractType() {
        return Optional.ofNullable(getUnchecked(AVAILABLE_CONTRACT_TYPE))
                .map(BytesWrapper::getData)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found AVAILABLE_CONTRACT_TYPE"));
    }


    public void addSystemContractAndSetPermission(int id) {
        byte[] availableContractType = getAvailableContractType();
        availableContractType[id / 8] |= (1 << id % 8);
        saveAvailableContractType(availableContractType);

        byte[] activeDefaultOperations = getActiveDefaultOperations();
        activeDefaultOperations[id / 8] |= (1 << id % 8);
        saveActiveDefaultOperations(activeDefaultOperations);
    }


    public void updateDynamicStoreByConfig() {
        if (Args.getInstance().getAllowGvmConstantinople() != 0) {
            saveAllowGvmConstantinople(Args.getInstance().getAllowGvmConstantinople());
            addSystemContractAndSetPermission(48);
        }
    }


    public void saveActiveDefaultOperations(byte[] value) {
        this.put(ACTIVE_DEFAULT_OPERATIONS,
                new BytesWrapper(value));
    }

    public byte[] getActiveDefaultOperations() {
        return Optional.ofNullable(getUnchecked(ACTIVE_DEFAULT_OPERATIONS))
                .map(BytesWrapper::getData)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ACTIVE_DEFAULT_OPERATIONS"));
    }

    public boolean supportDR() {
        return getAllowDelegateResource() == 1L;
    }

    public void saveAllowUpdateAccountName(long rate) {
        this.put(ALLOW_UPDATE_ACCOUNT_NAME,
                new BytesWrapper(ByteArray.fromLong(rate)));
    }

    public long getAllowUpdateAccountName() {
        return Optional.ofNullable(getUnchecked(ALLOW_UPDATE_ACCOUNT_NAME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_UPDATE_ACCOUNT_NAME"));
    }

    public void saveAllowSameTokenName(long rate) {
        this.put(ALLOW_SAME_TOKEN_NAME,
                new BytesWrapper(ByteArray.fromLong(rate)));
    }

    public long getAllowSameTokenName() {
        return Optional.ofNullable(getUnchecked(ALLOW_SAME_TOKEN_NAME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_SAME_TOKEN_NAME"));
    }

    public void saveAllowCreationOfContracts(long allowCreationOfContracts) {
        this.put(ALLOW_CREATION_OF_CONTRACTS,
                new BytesWrapper(ByteArray.fromLong(allowCreationOfContracts)));
    }

    public void saveTotalSignNum(int num) {
        this.put(DynamicPropertiesStore.TOTAL_SIGN_NUM,
                new BytesWrapper(ByteArray.fromInt(num)));
    }

    public int getTotalSignNum() {
        return Optional.ofNullable(getUnchecked(TOTAL_SIGN_NUM))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found TOTAL_SIGN_NUM"));
    }

    public void saveAllowMultiSign(long allowMultiSing) {
        this.put(ALLOW_MULTI_SIGN,
                new BytesWrapper(ByteArray.fromLong(allowMultiSing)));
    }

    public long getAllowMultiSign() {
        return Optional.ofNullable(getUnchecked(ALLOW_MULTI_SIGN))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_MULTI_SIGN"));
    }

    public long getAllowCreationOfContracts() {
        return Optional.ofNullable(getUnchecked(ALLOW_CREATION_OF_CONTRACTS))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_CREATION_OF_CONTRACTS"));
    }

    public boolean supportVM() {
        return getAllowCreationOfContracts() == 1L;
    }

    public void saveBlockFilledSlots(int[] blockFilledSlots) {
        logger.debug("blockFilledSlots:" + intArrayToString(blockFilledSlots));
        this.put(BLOCK_FILLED_SLOTS,
                new BytesWrapper(ByteArray.fromString(intArrayToString(blockFilledSlots))));
    }

    public int[] getBlockFilledSlots() {
        return Optional.ofNullable(getUnchecked(BLOCK_FILLED_SLOTS))
                .map(BytesWrapper::getData)
                .map(ByteArray::toStr)
                .map(this::stringToIntArray)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "not found latest CONFIRMED_BLOCK_NUM timestamp"));
    }

    public int getBlockFilledSlotsNumber() {
        return ChainConstant.BLOCK_FILLED_SLOTS_NUMBER;
    }

    public void applyBlock(boolean fillBlock) {
        int[] blockFilledSlots = getBlockFilledSlots();
        int blockFilledSlotsIndex = getBlockFilledSlotsIndex();
        blockFilledSlots[blockFilledSlotsIndex] = fillBlock ? 1 : 0;
        saveBlockFilledSlotsIndex((blockFilledSlotsIndex + 1) % getBlockFilledSlotsNumber());
        saveBlockFilledSlots(blockFilledSlots);
    }

    public int calculateFilledSlotsCount() {
        int[] blockFilledSlots = getBlockFilledSlots();
        return 100 * IntStream.of(blockFilledSlots).sum() / getBlockFilledSlotsNumber();
    }

    public void saveLatestConfirmedBlockNum(long number) {
        this.put(LATEST_CONFIRMED_BLOCK_NUM, new BytesWrapper(ByteArray.fromLong(number)));
    }


    public long getLatestConfirmedBlockNum() {
        return Optional.ofNullable(getUnchecked(LATEST_CONFIRMED_BLOCK_NUM))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found latest CONFIRMED_BLOCK_NUM"));
    }

    public void saveLatestProposalNum(long number) {
        this.put(LATEST_PROPOSAL_NUM, new BytesWrapper(ByteArray.fromLong(number)));
    }

    public long getLatestProposalNum() {
        return Optional.ofNullable(getUnchecked(LATEST_PROPOSAL_NUM))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found latest PROPOSAL_NUM"));
    }

    public void saveLatestExchangeNum(long number) {
        this.put(LATEST_EXCHANGE_NUM, new BytesWrapper(ByteArray.fromLong(number)));
    }

    public long getLatestExchangeNum() {
        return Optional.ofNullable(getUnchecked(LATEST_EXCHANGE_NUM))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found latest EXCHANGE_NUM"));
    }

    /**
     * get timestamp of creating global latest block.
     */
    public long getLatestBlockHeaderTimestamp() {
        return Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_TIMESTAMP))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found latest block header timestamp"));
    }

    /**
     * get number of global latest block.
     */
    public long getLatestBlockHeaderNumber() {
        return Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_NUMBER))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found latest block header number"));
    }

    public int getStateFlag() {
        return Optional.ofNullable(getUnchecked(STATE_FLAG))
                .map(BytesWrapper::getData)
                .map(ByteArray::toInt)
                .orElseThrow(() -> new IllegalArgumentException("not found maintenance flag"));
    }

    /**
     * get id of global latest block.
     */

    public Sha256Hash getLatestBlockHeaderHash() {
        byte[] blockHash = Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_HASH))
                .map(BytesWrapper::getData)
                .orElseThrow(() -> new IllegalArgumentException("not found block hash"));
        return Sha256Hash.wrap(blockHash);
    }

    /**
     * save timestamp of creating global latest block.
     */
    public void saveLatestBlockHeaderTimestamp(long t) {
        logger.info("update latest block header timestamp = {}", t);
        this.put(LATEST_BLOCK_HEADER_TIMESTAMP, new BytesWrapper(ByteArray.fromLong(t)));
    }

    /**
     * save number of global latest block.
     */
    public void saveLatestBlockHeaderNumber(long n) {
        logger.info("update latest block header number = {}", n);
        this.put(LATEST_BLOCK_HEADER_NUMBER, new BytesWrapper(ByteArray.fromLong(n)));
    }

    /**
     * save id of global latest block.
     */
    public void saveLatestBlockHeaderHash(ByteString h) {
        logger.info("update latest block header id = {}", ByteArray.toHexString(h.toByteArray()));
        this.put(LATEST_BLOCK_HEADER_HASH, new BytesWrapper(h.toByteArray()));
    }

    public void saveStateFlag(int n) {
        logger.info("update state flag = {}", n);
        this.put(STATE_FLAG, new BytesWrapper(ByteArray.fromInt(n)));
    }


    public long getNextMaintenanceTime() {
        return Optional.ofNullable(getUnchecked(NEXT_MAINTENANCE_TIME))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found NEXT_MAINTENANCE_TIME"));
    }

    public long getMaintenanceSkipSlots() {
        return Parameter.ChainConstant.MAINTENANCE_SKIP_SLOTS;
    }

    public void saveNextMaintenanceTime(long nextMaintenanceTime) {
        this.put(NEXT_MAINTENANCE_TIME,
                new BytesWrapper(ByteArray.fromLong(nextMaintenanceTime)));
    }


    public void updateNextMaintenanceTime(long blockTime) {
        long maintenanceTimeInterval = getMaintenanceTimeInterval();

        long currentMaintenanceTime = getNextMaintenanceTime();
        long round = (blockTime - currentMaintenanceTime) / maintenanceTimeInterval;
        long nextMaintenanceTime = currentMaintenanceTime + (round + 1) * maintenanceTimeInterval;
        saveNextMaintenanceTime(nextMaintenanceTime);

        logger.info(
                "do update nextMaintenanceTime,currentMaintenanceTime:{}, blockTime:{},nextMaintenanceTime:{}",
                new DateTime(currentMaintenanceTime), new DateTime(blockTime),
                new DateTime(nextMaintenanceTime)
        );
    }

    //The unit is gsc
    public void addTotalNetWeight(long amount) {
        long totalNetWeight = getTotalNetWeight();
        totalNetWeight += amount;
        saveTotalNetWeight(totalNetWeight);
    }

    //The unit is gsc
    public void addTotalCpuWeight(long amount) {
        long totalCpuWeight = getTotalCpuWeight();
        totalCpuWeight += amount;
        saveTotalCpuWeight(totalCpuWeight);
    }

    public void addTotalCreateAccountCost(long fee) {
        long newValue = getTotalCreateAccountCost() + fee;
        saveTotalCreateAccountFee(newValue);
    }

    public void addTotalCreateWitnessCost(long fee) {
        long newValue = getTotalCreateWitnessCost() + fee;
        saveTotalCreateWitnessFee(newValue);
    }

    public void addTotalTransactionCost(long fee) {
        long newValue = getTotalTransactionCost() + fee;
        saveTotalTransactionCost(newValue);
    }

    public void forked() {
        put(FORK_CONTROLLER, new BytesWrapper(Boolean.toString(true).getBytes()));
    }

    public void statsByVersion(int version, byte[] stats) {
        String statsKey = FORK_PREFIX + version;
        put(statsKey.getBytes(), new BytesWrapper(stats));
    }

    public byte[] statsByVersion(int version) {
        String statsKey = FORK_PREFIX + version;
        return revokingDB.getUnchecked(statsKey.getBytes());
    }

    public boolean getForked() {
        byte[] value = revokingDB.getUnchecked(FORK_CONTROLLER);
        return value == null ? Boolean.FALSE : Boolean.valueOf(new String(value));
    }

    /**
     * get allow protobuf number.
     */
    public long getAllowProtoFilterNum() {
        return Optional.ofNullable(getUnchecked(ALLOW_PROTO_FILTER_NUM))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(() -> new IllegalArgumentException("not found allow protobuf number"));
    }

    /**
     * save allow protobuf  number.
     */
    public void saveAllowProtoFilterNum(long num) {
        logger.info("update allow protobuf number = {}", num);
        this.put(ALLOW_PROTO_FILTER_NUM, new BytesWrapper(ByteArray.fromLong(num)));
    }

    public void saveAllowAccountStateRoot(long allowAccountStateRoot) {
        this.put(ALLOW_ACCOUNT_STATE_ROOT,
                new BytesWrapper(ByteArray.fromLong(allowAccountStateRoot)));
    }

    public long getAllowAccountStateRoot() {
        return Optional.ofNullable(getUnchecked(ALLOW_ACCOUNT_STATE_ROOT))
                .map(BytesWrapper::getData)
                .map(ByteArray::toLong)
                .orElseThrow(
                        () -> new IllegalArgumentException("not found ALLOW_ACCOUNT_STATE_ROOT"));
    }

    public boolean allowAccountStateRoot() {
        return getAllowAccountStateRoot() == 1;
    }
}
