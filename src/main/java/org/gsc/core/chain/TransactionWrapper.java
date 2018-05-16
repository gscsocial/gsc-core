package org.gsc.core.chain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.exception.ValidateSignatureException;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.crypto.ECKey;
import org.gsc.protos.Protocol.Transaction;

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


}
