package org.gsc.runtime;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.ReceiptWrapper;

@Slf4j
public class GVMTestResult {

  private Runtime runtime;
  private ReceiptWrapper receipt;
  private byte[] contractAddress;

  public byte[] getContractAddress() {
    return contractAddress;
  }

  public GVMTestResult setContractAddress(byte[] contractAddress) {
    this.contractAddress = contractAddress;
    return this;
  }

  public Runtime getRuntime() {
    return runtime;
  }

  public GVMTestResult setRuntime(Runtime runtime) {
    this.runtime = runtime;
    return this;
  }

  public ReceiptWrapper getReceipt() {
    return receipt;
  }

  public GVMTestResult setReceipt(ReceiptWrapper receipt) {
    this.receipt = receipt;
    return this;
  }

  public GVMTestResult(Runtime runtime, ReceiptWrapper receipt, byte[] contractAddress) {
    this.runtime = runtime;
    this.receipt = receipt;
    this.contractAddress = contractAddress;
  }

}
