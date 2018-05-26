package org.gsc.config.args;

import com.google.protobuf.ByteString;
import java.io.Serializable;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.gsc.common.utils.AddressUtil;
import org.gsc.common.utils.ByteArray;
import org.gsc.protos.Protocol.AccountType;

public class Account implements Serializable {

  private static final long serialVersionUID = 2674206490063656846L;

  private static final String ACCOUNT_TYPE_NORMAL = "NORMAL";
  private static final String ACCOUNT_TYPE_ASSETISSUE = "ASSETISSUE";
  private static final String ACCOUNT_TYPE_CONTRACT = "CONTRACT";

  private String accountName;
  private String accountType;

  @Getter
  private byte[] address;

  private String balance;

//  public byte[] getAddressBytes() {
//    return ByteArray.fromHexString(this.address);
//  }

  /**
   * Account address is a 21-bits hex string.
   */
  public void setAddress(final byte[] address) {
    if (!AddressUtil.addressValid(address)){
      throw new IllegalArgumentException(
          "The address(" + address + ") must be a 21 bytes.");
    }
    this.address = address;
  }

  public long getBalance() {
    return Long.parseLong(this.balance);
  }

  /**
   * Account balance is a long type.
   */
  public void setBalance(final String balance) {
    try {
      Long.parseLong(balance);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Balance(" + balance + ") must be Long type.");
    }

    this.balance = balance;
  }

  /**
   * get account from configuration.
   */
  public ByteString getAccountName() {
    if (StringUtils.isBlank(this.accountName)) {
      return ByteString.EMPTY;
    }

    return ByteString.copyFrom(ByteArray.fromString(this.accountName));
  }

  /**
   * Account name is a no-empty string.
   */
  public void setAccountName(String accountName) {
    if (StringUtils.isBlank(accountName)) {
      throw new IllegalArgumentException("Account name must be non-empty.");
    }

    this.accountName = accountName;
  }

  /**
   * switch account type.
   */
  public AccountType getAccountType() {
    return getAccountTypeByString(this.accountType);
  }

  /**
   * Account type: Normal/AssetIssue/Contract.
   */
  public void setAccountType(final String accountType) {
    if (!this.isAccountType(accountType)) {
      throw new IllegalArgumentException("Account type error: Not Normal/AssetIssue/Contract");
    }

    this.accountType = accountType;
  }

  /**
   * judge account type.
   */
  public boolean isAccountType(final String accountType) {
    if (accountType == null) {
      return false;
    }

    switch (accountType.toUpperCase()) {
      case ACCOUNT_TYPE_NORMAL:
      case ACCOUNT_TYPE_ASSETISSUE:
      case ACCOUNT_TYPE_CONTRACT:
        return true;
      default:
        return false;
    }
  }

  /**
   * Normal/AssetIssue/Contract.
   */
  public AccountType getAccountTypeByString(final String accountType) {
    if (accountType == null) {
      throw new IllegalArgumentException("Account type error: Not Normal/AssetIssue/Contract");
    }

    switch (accountType.toUpperCase()) {
      case ACCOUNT_TYPE_NORMAL:
        return AccountType.Normal;
      case ACCOUNT_TYPE_ASSETISSUE:
        return AccountType.AssetIssue;
      case ACCOUNT_TYPE_CONTRACT:
        return AccountType.Contract;
      default:
        throw new IllegalArgumentException("Account type error: Not Normal/AssetIssue/Contract");
    }
  }
}
