package org.gsc.config;

public interface GscConstants {

  interface ChainConstant {

    long TRANSFER_FEE = 0; // free
    long ASSET_ISSUE_FEE = 1024000000; // 1024 trx 1024*10^6
    long VOTE_WITNESS_FEE = 10000; // 10000 drop
    long CREATE_ACCOUNT_FEE = 10000; // 10000 drop
    long WITNESS_PAY_PER_BLOCK = 32000000;  // 32trx
    long WITNESS_STANDBY_ALLOWANCE = 115_200_000_000L;// 6 * 1200 * 16000000
    int WITNESS_STANDBY_LENGTH = 127;
    double SOLIDIFIED_THRESHOLD = 0.7;
    int PRIVATE_KEY_LENGTH = 64;
    int MAX_ACTIVE_WITNESS_NUM = 27;
    //int TRXS_SIZE = 2_000_000; // < 2MiB
    int BLOCK_SIZE = 2_000_000;
    int BLOCK_PRODUCED_INTERVAL = 3000; //ms,produce block period, must be divisible by 60. millisecond
    long CLOCK_MAX_DELAY = 3600000; // 3600 * 1000 ms
    double BLOCK_PRODUCED_TIME_OUT = 0.75;
    long BATCH_FETCH_RESPONSE_SIZE = 1000; //for each inventory message from peer, the max count of fetch inv message
    long PRECISION = 1000_000;
    long ONE_DAY_NET_LIMIT = 57_600_000_000L;
    long WINDOW_SIZE_MS = 24 * 3600 * 1000L;
    long CREATE_NEW_ACCOUNT_BANDWIDTH_COST = 200;
  }


  interface NetConstants {
  }

  interface DatabaseConstants {

    int TRANSACTIONS_COUNT_LIMIT_MAX = 1000;
    int ASSET_ISSUE_COUNT_LIMIT_MAX = 1000;
  }


}
