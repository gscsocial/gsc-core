package org.gsc.core.wrapper;

import org.gsc.protos.Protocol.Account;

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

  public long getBalance() {
    return this.account.getBalance();
  }
}
