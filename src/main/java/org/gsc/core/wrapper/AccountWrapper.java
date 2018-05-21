package org.gsc.core.wrapper;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.protos.Protocol.Account;

@Slf4j
public class AccountWrapper implements ProtoWrapper<Account>, Comparable<AccountWrapper> {

  private Account account;

  @Override
  public int compareTo(AccountWrapper o) {
    return 0;
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public Account getInstance() {
    return null;
  }

  public AccountWrapper(byte[] data) {
    try {
      this.account = Account.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public long getBalance() {
    return this.account.getBalance();
  }
}
