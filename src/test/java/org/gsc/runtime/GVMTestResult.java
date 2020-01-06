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

  public GVMTestResult setReceipt(ReceiptWrapper receipt) {
    this.receipt = receipt;
    return this;
  }

  public ReceiptWrapper getReceipt() {
    return receipt;
  }

  public GVMTestResult(Runtime runtime, ReceiptWrapper receipt, byte[] contractAddress) {
    this.runtime = runtime;
    this.receipt = receipt;
    this.contractAddress = contractAddress;
  }

}
