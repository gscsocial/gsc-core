package org.gsc.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.config.Parameter.WalletConstant;

@Slf4j
public class AddressUtil {

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != WalletConstant.ADDRESS_SIZE / 2) {
      logger.warn(
          "Warning: Address length need " + WalletConstant.ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }
    if (address[0] != WalletConstant.ADD_PRE_FIX_BYTE_MAINNET) {
      logger.warn("Warning: Address need prefix with " + WalletConstant.ADD_PRE_FIX_BYTE_MAINNET + " but "
          + address[0] + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

}
