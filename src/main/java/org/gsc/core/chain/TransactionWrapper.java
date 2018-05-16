package org.gsc.core.chain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.exception.ValidateSignatureException;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.ECKey.ECDSASignature;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;


@Slf4j
public class TransactionWrapper {

  private Transaction transaction;
  @Setter
  private boolean isVerified = false;

  /**
   * constructor TransactionWrapper.
   */
  public TransactionWrapper(Transaction trx) {
    this.transaction = trx;
  }

  /**
   * get account from bytes data.
   */
  public TransactionWrapper(byte[] data) {
    try {
      this.transaction = Transaction.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public void setResult(TransactionWrapper ret) {
   // this.getInstance().toBuilder().setRet((tx.getInstance()).build();)
  }

  public void setReference(long blockNum, byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = this.transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockHash, 8, 16)))
        .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  /**
   * @param expiration must be in milliseconds format
   */
  public void setExpiration(long expiration) {
    Transaction.raw rawData = this.transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public long getExpiration() {
    return transaction.getRawData().getExpiration();
  }

  public TransactionWrapper(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  @Deprecated
  public void createTransaction(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public Sha256Hash getHash() {
    byte[] transBytes = this.transaction.toByteArray();
    return Sha256Hash.of(transBytes);
  }

  public Sha256Hash getRawHash() {
    return Sha256Hash.of(this.transaction.getRawData().toByteArray());
  }

  /**
   * cheack balance of the address.
   */
  public boolean checkBalance(byte[] address, byte[] to, long amount, long balance) {
//    if (!Wallet.addressValid(address)) {
//      logger.error("address invalid");
//      return false;
//    }
//
//    if (!Wallet.addressValid(to)) {
//      logger.error("address invalid");
//      return false;
//    }
//
//    if (amount <= 0) {
//      logger.error("amount required a positive number");
//      return false;
//    }
//
//    if (amount > balance) {
//      logger.error("don't have enough money");
//      return false;
//    }

    return true;
  }

//  @Deprecated
//  public void sign(byte[] privateKey) {
//    ECKey ecKey = ECKey.fromPrivate(privateKey);
//    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
//    ByteString sig = ByteString.copyFrom(signature.toBase64().getBytes());
//    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
//  }

  public static byte[] getOwner(Transaction.Contract contract) {
    return null;
  }

  public static byte[] getToAddress(Transaction.Contract contract) {
    return null;
  }

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }

  /**
   * validate signature
   */
  public boolean validateSignature() throws ValidateSignatureException {
    if (isVerified == true) {
      return true;
    }

    try {
      Transaction.Contract contract = this.transaction.getRawData().getContract();
      byte[] owner = getOwner(contract);
      byte[] address = ECKey.signatureToAddress(getRawHash().getBytes(),
          getBase64FromByteString(this.transaction.getSignature()));
      if (!Arrays.equals(owner, address)) {
        isVerified = false;
        throw new ValidateSignatureException("sig error");
      }
    } catch (SignatureException e) {
      isVerified = false;
      throw new ValidateSignatureException(e.getMessage());
    }

    isVerified = true;
    return true;
  }

  public Sha256Hash getTransactionId() {
    return Sha256Hash.of(this.transaction.getRawData().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.transaction.toByteArray();
  }

  public long getSerializedSize() {
    return this.transaction.getSerializedSize();
  }

  @Override
  public Transaction getInstance() {
    return this.transaction;
  }

  private StringBuffer toStringBuff = new StringBuffer();

  @Override
  public String toString() {
      return transaction.toString();
  }

}
