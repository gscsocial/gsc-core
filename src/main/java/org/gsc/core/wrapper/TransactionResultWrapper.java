package org.gsc.core.wrapper;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Result;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;

@Slf4j
public class TransactionResultWrapper implements ProtoWrapper<Result> {

  private Transaction.Result transactionResult;

  /**
   * constructor TransactionWrapper.
   */
  public TransactionResultWrapper(Transaction.Result trxRet) {
    this.transactionResult = trxRet;
  }

  public TransactionResultWrapper(byte[] data) throws BadItemException {
    try {
      this.transactionResult = Transaction.Result.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionResult proto data parse exception");
    }
  }

  public TransactionResultWrapper() {
    this.transactionResult = Transaction.Result.newBuilder().build();
  }

  public TransactionResultWrapper(contractResult code) {
    this.transactionResult = Transaction.Result.newBuilder().setContractRet(code).build();
  }

  public TransactionResultWrapper(Transaction.Result.code code, long fee) {
    this.transactionResult = Transaction.Result.newBuilder().setRet(code).setFee(fee).build();
  }

  public void setStatus(long fee, Transaction.Result.code code) {
    long oldValue = transactionResult.getFee();
    this.transactionResult = this.transactionResult.toBuilder()
        .setFee(oldValue + fee)
        .setRet(code).build();
  }

  public long getFee() {
    return transactionResult.getFee();
  }

  public void setUnfreezeAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder().setUnfreezeAmount(amount).build();
  }

  public long getUnfreezeAmount() {
    return transactionResult.getUnfreezeAmount();
  }

  public void setWithdrawAmount(long amount) {
    this.transactionResult = this.transactionResult.toBuilder().setWithdrawAmount(amount).build();
  }

  public long getWithdrawAmount() {
    return transactionResult.getWithdrawAmount();
  }

  public void addFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder()
        .setFee(this.transactionResult.getFee() + fee).build();
  }
  
  public void setFee(long fee) {
    this.transactionResult = this.transactionResult.toBuilder().setFee(fee).build();
  }

  public void setErrorCode(Transaction.Result.code code) {
    this.transactionResult = this.transactionResult.toBuilder().setRet(code).build();
  }

  @Override
  public byte[] getData() {
    return this.transactionResult.toByteArray();
  }

  @Override
  public Result getInstance() {
    return this.transactionResult;
  }
}
