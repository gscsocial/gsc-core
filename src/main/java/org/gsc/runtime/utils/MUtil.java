package org.gsc.runtime.utils;

import java.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.gsc.crypto.Hash;
import org.gsc.common.storage.Deposit;
import org.gsc.core.Wallet;
import org.gsc.core.operator.TransferOperator;
import org.gsc.core.exception.ContractValidateException;

public class MUtil {

  public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount)
      throws ContractValidateException {
    if (0 == amount) {
      return;
    }
    TransferOperator.validateForSmartContract(deposit, fromAddress, toAddress, amount);
    if (deposit.getBalance(fromAddress) < amount) {
      throw new RuntimeException(
          Hex.toHexString(fromAddress).toUpperCase() + " not enough balance!");
    }
    if (deposit.getBalance(toAddress) + amount < amount) {
      throw new RuntimeException("Long integer overflow!");
    }
    deposit.addBalance(toAddress, amount);
    deposit.addBalance(fromAddress, -amount);
  }

  public static void burn(Deposit deposit, byte[] address, long amount) {
    if (deposit.getBalance(address) < amount) {
      throw new RuntimeException("Not enough balance!");
    }
    deposit.addBalance(address, -amount);
  }

  public static byte[] convertTogscAddress(byte[] address) {
    if (address.length == 20) {
      byte[] newAddress = new byte[21];
      byte[] temp = new byte[]{Wallet.getAddressPreFixByte()};
      System.arraycopy(temp, 0, newAddress, 0, temp.length);
      System.arraycopy(address, 0, newAddress, temp.length, address.length);
      address = newAddress;
    }
    return address;
  }

  public static String get4BytesSha3HexString(String data) {
    return Hex.toHexString(Arrays.copyOf(Hash.sha3(data.getBytes()), 4));
  }

  public static byte[] generateByteArray(byte[] ...parameters){
    int length =0;
    for(int i=0;i<parameters.length;i++){
      length+=parameters[i].length;
    }
    
    byte[] result = new byte[length];
    
    int pos =0;
    for (int i=0;i<parameters.length;i++){
      System.arraycopy(parameters[i],0,result,pos,parameters[i].length);
      pos += parameters[i].length;
    }
    return result;
  }
}
