package org.gsc.config;

public interface GscConstants {

  interface ChainConstant {
    int BLOCK_PRODUCED_INTERVAL = 5000; //ms,produce block period, must be divisible by 60. millisecond
    long CLOCK_MAX_DELAY = 3600 * 1000; //ms
  }

  interface NetConstants {
  }


}
