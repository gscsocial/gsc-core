package org.gsc.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.config.Parameter;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.core.wrapper.BytesWrapper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@Component
public class DynamicPropertiesStore extends GSCStoreWithRevoking<BytesWrapper> {

  private static final byte[] LATEST_BLOCK_HEADER_TIMESTAMP = "latest_block_header_timestamp"
      .getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_NUMBER = "latest_block_header_number".getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_HASH = "latest_block_header_hash".getBytes();
  private static final byte[] STATE_FLAG = "state_flag"
      .getBytes(); // 1 : is maintenance, 0 : is not maintenance
  private static final byte[] LATEST_SOLIDIFIED_BLOCK_NUM = "LATEST_SOLIDIFIED_BLOCK_NUM"
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

  private static final byte[] ONE_DAY_NET_LIMIT = "ONE_DAY_NET_LIMIT".getBytes();

  //public free bandwidth
  private static final byte[] PUBLIC_NET_USAGE = "PUBLIC_NET_USAGE".getBytes();

  private static final byte[] PUBLIC_NET_LIMIT = "PUBLIC_NET_LIMIT".getBytes();

  private static final byte[] PUBLIC_NET_TIME = "PUBLIC_NET_TIME".getBytes();

  private static final byte[] FREE_NET_LIMIT = "FREE_NET_LIMIT".getBytes();

  private static final byte[] TOTAL_NET_WEIGHT = "TOTAL_NET_WEIGHT".getBytes();
  //ONE_DAY_NET_LIMIT - PUBLIC_NET_LIMIT
  private static final byte[] TOTAL_NET_LIMIT = "TOTAL_NET_LIMIT".getBytes();

  private static final byte[] TOTAL_ENERGY_WEIGHT = "TOTAL_ENERGY_WEIGHT".getBytes();

  private static final byte[] TOTAL_ENERGY_LIMIT = "TOTAL_ENERGY_LIMIT".getBytes();

  private static final byte[] ENERGY_FEE = "ENERGY_FEE".getBytes();

  private static final byte[] MAX_CPU_TIME_OF_ONE_TX = "MAX_CPU_TIME_OF_ONE_TX".getBytes();

  //abandon
  private static final byte[] CREATE_ACCOUNT_FEE = "CREATE_ACCOUNT_FEE".getBytes();

  private static final byte[] CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT
      = "CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT".getBytes();

  private static final byte[] CREATE_NEW_ACCOUNT_BANDWIDTH_RATE = "CREATE_NEW_ACCOUNT_BANDWIDTH_RATE"
      .getBytes();

  private static final byte[] TRANSACTION_FEE = "TRANSACTION_FEE".getBytes(); // 1 byte

  private static final byte[] ASSET_ISSUE_FEE = "ASSET_ISSUE_FEE".getBytes();

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

  //This value is only allowed to be 0, 1, -1
  private static final byte[] REMOVE_THE_POWER_OF_THE_GR = "REMOVE_THE_POWER_OF_THE_GR".getBytes();

  //If the parameter is larger than 0, the contract is allowed to be created.
  private static final byte[] ALLOW_CREATION_OF_CONTRACTS = "ALLOW_CREATION_OF_CONTRACTS".getBytes();

  @Autowired
  private DynamicPropertiesStore(@Value("properties") String dbName) {
    super(dbName);

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
      this.getLatestSolidifiedBlockNum();
    } catch (IllegalArgumentException e) {
      this.saveLatestSolidifiedBlockNum(0);
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
      this.getMaxFrozenTime();
    } catch (IllegalArgumentException e) {
      this.saveMaxFrozenTime(5); //adjust 3 to 5
    }

    try {
      this.getMinFrozenTime();
    } catch (IllegalArgumentException e) {
      this.saveMinFrozenTime(5);//adjust 3 to 5
    }

    try {
      this.getMaxFrozenSupplyNumber();
    } catch (IllegalArgumentException e) {
      this.saveMaxFrozenSupplyNumber(10);
    }

    try {
      this.getMaxFrozenSupplyTime();
    } catch (IllegalArgumentException e) {
      this.saveMaxFrozenSupplyTime(3652);
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
      //this.saveWitnessPayPerBlockByBlockNum(32000000L);
      this.saveWitnessPayPerBlockByBlockNum(0L);
    }

    try {
      this.getWitnessStandbyAllowance();
    } catch (IllegalArgumentException e) {
      //this.saveWitnessStandbyAllowance(115_200_000_000L);
      this.saveWitnessStandbyAllowance(0L);
    }

    try {
      this.getMaintenanceTimeInterval();
    } catch (IllegalArgumentException e) {
      this.saveMaintenanceTimeInterval(Args.getInstance().getMaintenanceTimeInterval()); // 6 hours
    }

    try {
      this.getAccountUpgradeCost();
    } catch (IllegalArgumentException e) {
      this.saveAccountUpgradeCost(10_000_000_000L); // adjust 9999 to 10000 by snow 10/22/2018
    }

    try {
      this.getPublicNetUsage();
    } catch (IllegalArgumentException e) {
      this.savePublicNetUsage(0L);
    }

    try {
      this.getOneDayNetLimit();
    } catch (IllegalArgumentException e) {
      this.saveOneDayNetLimit(57_600_000_000L);
    }

    try {
      this.getPublicNetLimit();
    } catch (IllegalArgumentException e) {
      this.savePublicNetLimit(14_400_000_000L);
    }

    try {
      this.getPublicNetTime();
    } catch (IllegalArgumentException e) {
      this.savePublicNetTime(0L);
    }

    try {
      this.getFreeNetLimit();
    } catch (IllegalArgumentException e) {
      this.saveFreeNetLimit(5000L);
    }

    try {
      this.getTotalNetWeight();
    } catch (IllegalArgumentException e) {
      this.saveTotalNetWeight(0L);
    }

    try {
      this.getTotalNetLimit();
    } catch (IllegalArgumentException e) {
      this.saveTotalNetLimit(43_200_000_000L);
    }

    try {
      this.getTotalEnergyWeight();
    } catch (IllegalArgumentException e) {
      this.saveTotalEnergyWeight(0L);
    }

    try {
      this.getTotalEnergyLimit();
    } catch (IllegalArgumentException e) {
      this.saveTotalEnergyLimit(50_000_000_000L);
    }

    try {
      this.getEnergyFee();
    } catch (IllegalArgumentException e) {
      this.saveEnergyFee(100L);// 100 dot per energy
    }

    try {
      this.getMaxCpuTimeOfOneTX();
    } catch (IllegalArgumentException e) {
      this.saveMaxCpuTimeOfOneTX(50L);
    }

    try {
      this.getCreateAccountFee();
    } catch (IllegalArgumentException e) {
      this.saveCreateAccountFee(100_000L); // 0.1GSC
    }

    try {
      this.getCreateNewAccountFeeInSystemContract();
    } catch (IllegalArgumentException e) {
      this.saveCreateNewAccountFeeInSystemContract(0L); //changed by committee later
    }

    try {
      this.getCreateNewAccountBandwidthRate();
    } catch (IllegalArgumentException e) {
      this.saveCreateNewAccountBandwidthRate(1L); //changed by committee later
    }

    try {
      this.getTransactionFee();
    } catch (IllegalArgumentException e) {
      this.saveTransactionFee(10L);
    }

    try {
      this.getAssetIssueFee();
    } catch (IllegalArgumentException e) {
      this.saveAssetIssueFee(1000000000L);//adjust 1024 to 1000
    }

    try {
      this.getExchangeCreateFee();
    } catch (IllegalArgumentException e) {
      this.saveExchangeCreateFee(1024000000L);
    }

    try {
      this.getExchangeBalanceLimit();
    } catch (IllegalArgumentException e) {
      this.saveExchangeBalanceLimit(1_000_000_000_000_000L);
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
      this.saveTotalStoragePool(100_000_000_000000L);
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

  public void saveWitnessPayPerBlockByBlockNum(long blockNum) {
    logger.debug("WITNESS_PAY_PER_BLOCK BLOCK_NUM:" + blockNum);
    long pay = 0L;
    if(blockNum >= 21024000){//witness pay 6% of total during 1st year,4.5% 2nd year,3% 3rd year
        pay = 1902587;
    }else if(blockNum >= 10512000){
        pay = 2853881;
    }else{
        pay = 3805175;
    }
    if(blockNum == 0 || pay != getWitnessPayPerBlock()){
      this.put(WITNESS_PAY_PER_BLOCK,
              new BytesWrapper(ByteArray.fromLong(pay)));
      logger.info("WITNESS_PAY_PER_BLOCK changed to={} from blockNum={}",pay,blockNum);
    }
  }

  public long getWitnessPayPerBlock() {
    return Optional.ofNullable(getUnchecked(WITNESS_PAY_PER_BLOCK))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found WITNESS_PAY_PER_BLOCK"));
  }

  public void saveWitnessStandbyAllowance(long blockNum) {
//    logger.debug("WITNESS_STANDBY_ALLOWANCE blocknum:" + allowance);
    long pay = 0L;
    if(blockNum >= 21024000){//witness pay 6% of total during 1st year,4.5% 2nd year,3% 3rd year
      pay = 6849315068L;
    }else if(blockNum >= 10512000){
      pay = 10273972602L;
    }else{
      pay = 13698630136L;
    }
    if(blockNum == 0 || pay != getWitnessStandbyAllowance()){
      this.put(WITNESS_STANDBY_ALLOWANCE,
              new BytesWrapper(ByteArray.fromLong(pay)));
      logger.info("WITNESS_STANDBY_ALLOWANCE changed to={} from blockNum={}",pay,blockNum);
    }
  }

  public long getWitnessStandbyAllowance() {
    return Optional.ofNullable(getUnchecked(WITNESS_STANDBY_ALLOWANCE))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found WITNESS_STANDBY_ALLOWANCE"));
  }

  public void saveOneDayNetLimit(long oneDayNetLimit) {
    this.put(ONE_DAY_NET_LIMIT,
        new BytesWrapper(ByteArray.fromLong(oneDayNetLimit)));
  }

  public long getOneDayNetLimit() {
    return Optional.ofNullable(getUnchecked(ONE_DAY_NET_LIMIT))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ONE_DAY_NET_LIMIT"));
  }

  public void savePublicNetUsage(long publicNetUsage) {
    this.put(PUBLIC_NET_USAGE,
        new BytesWrapper(ByteArray.fromLong(publicNetUsage)));
  }

  public long getPublicNetUsage() {
    return Optional.ofNullable(getUnchecked(PUBLIC_NET_USAGE))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found PUBLIC_NET_USAGE"));
  }

  public void savePublicNetLimit(long publicNetLimit) {
    this.put(PUBLIC_NET_LIMIT,
        new BytesWrapper(ByteArray.fromLong(publicNetLimit)));
  }

  public long getPublicNetLimit() {
    return Optional.ofNullable(getUnchecked(PUBLIC_NET_LIMIT))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found PUBLIC_NET_LIMIT"));
  }

  public void savePublicNetTime(long publicNetTime) {
    this.put(PUBLIC_NET_TIME,
        new BytesWrapper(ByteArray.fromLong(publicNetTime)));
  }

  public long getPublicNetTime() {
    return Optional.ofNullable(getUnchecked(PUBLIC_NET_TIME))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found PUBLIC_NET_TIME"));
  }

  public void saveFreeNetLimit(long freeNetLimit) {
    this.put(FREE_NET_LIMIT,
        new BytesWrapper(ByteArray.fromLong(freeNetLimit)));
  }

  public long getFreeNetLimit() {
    return Optional.ofNullable(getUnchecked(FREE_NET_LIMIT))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found FREE_NET_LIMIT"));
  }

  public void saveTotalNetWeight(long totalNetWeight) {
    this.put(TOTAL_NET_WEIGHT,
        new BytesWrapper(ByteArray.fromLong(totalNetWeight)));
  }

  public long getTotalNetWeight() {
    return Optional.ofNullable(getUnchecked(TOTAL_NET_WEIGHT))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_NET_WEIGHT"));
  }

  public void saveTotalEnergyWeight(long totalEnergyWeight) {
    this.put(TOTAL_ENERGY_WEIGHT,
        new BytesWrapper(ByteArray.fromLong(totalEnergyWeight)));
  }

  public long getTotalEnergyWeight() {
    return Optional.ofNullable(getUnchecked(TOTAL_ENERGY_WEIGHT))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_ENERGY_WEIGHT"));
  }


  public void saveTotalNetLimit(long totalNetLimit) {
    this.put(TOTAL_NET_LIMIT,
        new BytesWrapper(ByteArray.fromLong(totalNetLimit)));
  }

  public long getTotalNetLimit() {
    return Optional.ofNullable(getUnchecked(TOTAL_NET_LIMIT))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_NET_LIMIT"));
  }

  public void saveTotalEnergyLimit(long totalEnergyLimit) {
    this.put(TOTAL_ENERGY_LIMIT,
        new BytesWrapper(ByteArray.fromLong(totalEnergyLimit)));
  }

  public long getTotalEnergyLimit() {
    return Optional.ofNullable(getUnchecked(TOTAL_ENERGY_LIMIT))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_ENERGY_LIMIT"));
  }


  public void saveEnergyFee(long totalEnergyFee) {
    this.put(ENERGY_FEE,
        new BytesWrapper(ByteArray.fromLong(totalEnergyFee)));
  }

  public long getEnergyFee() {
    return Optional.ofNullable(getUnchecked(ENERGY_FEE))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ENERGY_FEE"));
  }

  public void saveMaxCpuTimeOfOneTX(long time) {
    this.put(MAX_CPU_TIME_OF_ONE_TX,
        new BytesWrapper(ByteArray.fromLong(time)));
  }

  public long getMaxCpuTimeOfOneTX() {
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
            () -> new IllegalArgumentException("not found CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT"));
  }

  public void saveCreateNewAccountBandwidthRate(long rate) {
    this.put(CREATE_NEW_ACCOUNT_BANDWIDTH_RATE,
        new BytesWrapper(ByteArray.fromLong(rate)));
  }

  public long getCreateNewAccountBandwidthRate() {
    return Optional.ofNullable(getUnchecked(CREATE_NEW_ACCOUNT_BANDWIDTH_RATE))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found CREATE_NsEW_ACCOUNT_BANDWIDTH_RATE2"));
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

  public long getAssetIssueFee() {
    return Optional.ofNullable(getUnchecked(ASSET_ISSUE_FEE))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ASSET_ISSUE_FEE"));
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

  public void saveTotalStoragePool(long trx) {
    this.put(TOTAL_STORAGE_POOL,
        new BytesWrapper(ByteArray.fromLong(trx)));
  }

  public long getTotalStoragePool() {
    return Optional.ofNullable(getUnchecked(TOTAL_STORAGE_POOL))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_STORAGE_POOL"));
  }

  public void saveTotalStorageTax(long trx) {
    this.put(TOTAL_STORAGE_TAX,
        new BytesWrapper(ByteArray.fromLong(trx)));
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



  public void saveAllowCreationOfContracts(long allowCreationOfContracts) {
    this.put(DynamicPropertiesStore.ALLOW_CREATION_OF_CONTRACTS,
        new BytesWrapper(ByteArray.fromLong(allowCreationOfContracts)));
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
            () -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM timestamp"));
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

  public void saveLatestSolidifiedBlockNum(long number) {
    this.put(LATEST_SOLIDIFIED_BLOCK_NUM, new BytesWrapper(ByteArray.fromLong(number)));
  }


  public long getLatestSolidifiedBlockNum() {
    return Optional.ofNullable(getUnchecked(LATEST_SOLIDIFIED_BLOCK_NUM))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM"));
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
        .orElseThrow(() -> new IllegalArgumentException("not found latest block header timestamp"));
  }

  /**
   * get number of global latest block.
   */
  public long getLatestBlockHeaderNumber() {
    return Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_NUMBER))
        .map(BytesWrapper::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found latest block header number"));
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
    // update latest block header timestamp = 1540189404000
    logger.info("update latest block header timestamp = {}", t);
    this.put(LATEST_BLOCK_HEADER_TIMESTAMP, new BytesWrapper(ByteArray.fromLong(t)));
  }

  /**
   * save number of global latest block.
   */
  public void saveLatestBlockHeaderNumber(long n) {
    // update latest block header number = 279
    logger.info("update latest block header number = {}", n);
    this.put(LATEST_BLOCK_HEADER_NUMBER, new BytesWrapper(ByteArray.fromLong(n)));
  }

  /**
   * save id of global latest block.
   */
  public void saveLatestBlockHeaderHash(ByteString h) {
    // 2.update latest block header id = 000000000000011732b59a4c89d1a30edd0f52626b68324c118b745bc544c46e
    logger.info("update latest block header id = {}", ByteArray.toHexString(h.toByteArray()));
    this.put(LATEST_BLOCK_HEADER_HASH, new BytesWrapper(h.toByteArray()));
    if (revokingDB.getUnchecked(LATEST_BLOCK_HEADER_HASH).length == 32) {
    }
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

    /**
     *
     * blockTime: 1540210389000 1540252800000
     * maintenanceTimeInterval: 43200000
     * round: 35653
     * currentMaintenanceTime: 0
     * nextMaintenanceTime: 1540252800000
     *
     */
    logger.info("blockTime: " + blockTime);
    logger.info("maintenanceTimeInterval: " + maintenanceTimeInterval);
    logger.info("round: " + round);
    logger.info("currentMaintenanceTime: " + currentMaintenanceTime);
    logger.info("nextMaintenanceTime: " + nextMaintenanceTime);
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
  public void addTotalEnergyWeight(long amount) {
    long totalEnergyWeight = getTotalEnergyWeight();
    totalEnergyWeight += amount;
    saveTotalEnergyWeight(totalEnergyWeight);
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

  public boolean getForked() {
    byte[] value = revokingDB.getUnchecked(FORK_CONTROLLER);
    return value == null ? Boolean.FALSE : Boolean.valueOf(new String(value));
  }
}
