package org.gsc.runtime;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.ReceiptWrapper;

@Slf4j
public class TVMTestResult {

  private Runtime runtime;
  private ReceiptWrapper receipt;
  private byte[] contractAddress;

  public byte[] getContractAddress() {
    return contractAddress;
  }

  public TVMTestResult setContractAddress(byte[] contractAddress) {
    this.contractAddress = contractAddress;
    return this;
  }

  public Runtime getRuntime() {
    return runtime;
  }

  public TVMTestResult setRuntime(Runtime runtime) {
    this.runtime = runtime;
    return this;
  }

  public ReceiptWrapper getReceipt() {
    return receipt;
  }

  public TVMTestResult setReceipt(ReceiptWrapper receipt) {
    this.receipt = receipt;
    return this;
  }

  public TVMTestResult(Runtime runtime, ReceiptWrapper receipt, byte[] contractAddress) {
    this.runtime = runtime;
    this.receipt = receipt;
    this.contractAddress = contractAddress;
  }

}
