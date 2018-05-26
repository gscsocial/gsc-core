package org.gsc.db;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DynamicPropertiesStore extends ChainStore<BytesWrapper> {

  private static final byte[] MAINTENANCE_TIME_INTERVAL = "MAINTENANCE_TIME_INTERVAL".getBytes();
  private static final long MAINTENANCE_SKIP_SLOTS = 2;

  private static final byte[] VOTE_REWARD_RATE = "VOTE_REWARD_RATE".getBytes(); // percent
  private static final byte[] SINGLE_REPEAT = "SINGLE_REPEAT".getBytes();

  private static final byte[] LATEST_BLOCK_HEADER_TIMESTAMP = "latest_block_header_timestamp"
      .getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_NUMBER = "latest_block_header_number".getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_HASH = "latest_block_header_hash".getBytes();
  private static final byte[] STATE_FLAG = "state_flag"
      .getBytes(); // 1 : is maintenance, 0 : is not maintenance
  private static final byte[] LATEST_SOLIDIFIED_BLOCK_NUM = "LATEST_SOLIDIFIED_BLOCK_NUM"
      .getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS = "BLOCK_FILLED_SLOTS".getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS_INDEX = "BLOCK_FILLED_SLOTS_INDEX".getBytes();

  private static final byte[] NEXT_MAINTENANCE_TIME = "NEXT_MAINTENANCE_TIME".getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS_NUMBER = "BLOCK_FILLED_SLOTS_NUMBER".getBytes();

  private static final byte[] MAX_VOTE_NUMBER = "MAX_VOTE_NUMBER".getBytes();

  private static final byte[] MAX_FROZEN_NUMBER = "MAX_FROZEN_NUMBER".getBytes();

  private static final byte[] MAX_FROZEN_TIME = "MAX_FROZEN_TIME".getBytes();

  private static final byte[] MIN_FROZEN_TIME = "MIN_FROZEN_TIME".getBytes();

  private static final byte[] MAX_FROZEN_SUPPLY_NUMBER = "MAX_FROZEN_SUPPLY_NUMBER".getBytes();

  private static final byte[] MAX_FROZEN_SUPPLY_TIME = "MAX_FROZEN_SUPPLY_TIME".getBytes();

  private static final byte[] MIN_FROZEN_SUPPLY_TIME = "MIN_FROZEN_SUPPLY_TIME".getBytes();

  private static final byte[] WITNESS_ALLOWANCE_FROZEN_TIME = "WITNESS_ALLOWANCE_FROZEN_TIME"
      .getBytes();

  private static final byte[] BANDWIDTH_PER_TRANSACTION = "BANDWIDTH_PER_TRANSACTION".getBytes();

  private static final byte[] BANDWIDTH_PER_COINDAY = "BANDWIDTH_PER_COINDAY".getBytes();

  private static final byte[] ACCOUNT_UPGRADE_COST = "ACCOUNT_UPGRADE_COST".getBytes();
  // 1_000_000L
  private static final byte[] NON_EXISTENT_ACCOUNT_TRANSFER_MIN = "NON_EXISTENT_ACCOUNT_TRANSFER_MIN"
      .getBytes();

  private static final byte[] OPERATING_TIME_INTERVAL = "OPERATING_TIME_INTERVAL".getBytes();


  @Autowired
  private DynamicPropertiesStore(@Qualifier("properties") String dbName) {

    super(dbName);

  }

  @Override
  public BytesWrapper get(byte[] key) {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }



}
