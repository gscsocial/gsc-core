package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ExchangeWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.ExchangeCreateContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class ExchangeCreateOperator extends AbstractOperator {

  ExchangeCreateOperator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final ExchangeCreateContract exchangeCreateContract = this.contract
          .unpack(ExchangeCreateContract.class);
      AccountWrapper accountWrapper = dbManager.getAccountStore()
          .get(exchangeCreateContract.getOwnerAddress().toByteArray());

      byte[] firstTokenID = exchangeCreateContract.getFirstTokenId().toByteArray();
      byte[] secondTokenID = exchangeCreateContract.getSecondTokenId().toByteArray();
      long firstTokenBalance = exchangeCreateContract.getFirstTokenBalance();
      long secondTokenBalance = exchangeCreateContract.getSecondTokenBalance();

      long newBalance = accountWrapper.getBalance() - calcFee();

      accountWrapper.setBalance(newBalance);

      if (Arrays.equals(firstTokenID, "_".getBytes())) {
        accountWrapper.setBalance(newBalance - firstTokenBalance);
      } else {
        accountWrapper.reduceAssetAmount(firstTokenID, firstTokenBalance);
      }

      if (Arrays.equals(secondTokenID, "_".getBytes())) {
        accountWrapper.setBalance(newBalance - secondTokenBalance);
      } else {
        accountWrapper.reduceAssetAmount(secondTokenID, secondTokenBalance);
      }

      long id = dbManager.getDynamicPropertiesStore().getLatestExchangeNum() + 1;
      long now = dbManager.getHeadBlockTimeStamp();
      ExchangeWrapper exchangeWrapper =
          new ExchangeWrapper(
              exchangeCreateContract.getOwnerAddress(),
              id,
              now,
              firstTokenID,
              secondTokenID
          );

      exchangeWrapper.setBalance(firstTokenBalance, secondTokenBalance);

      dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
      dbManager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);
      dbManager.getDynamicPropertiesStore().saveLatestExchangeNum(id);

      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(ExchangeCreateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ExchangeCreateContract],real type[" + contract
              .getClass() + "]");
    }
    final ExchangeCreateContract contract;
    try {
      contract = this.contract.unpack(ExchangeCreateContract.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!this.dbManager.getAccountStore().has(ownerAddress)) {
      throw new ContractValidateException("account[" + readableOwnerAddress + "] not exists");
    }

    AccountWrapper accountWrapper = this.dbManager.getAccountStore().get(ownerAddress);

    if (accountWrapper.getBalance() < calcFee()) {
      throw new ContractValidateException("No enough balance for exchange create fee!");
    }

    byte[] firstTokenID = contract.getFirstTokenId().toByteArray();
    byte[] secondTokenID = contract.getSecondTokenId().toByteArray();
    long firstTokenBalance = contract.getFirstTokenBalance();
    long secondTokenBalance = contract.getSecondTokenBalance();

    if (Arrays.equals(firstTokenID, secondTokenID)) {
      throw new ContractValidateException("cannot exchange same tokens");
    }

    if (firstTokenBalance <= 0 || secondTokenBalance <= 0) {
      throw new ContractValidateException("token balance must greater than zero");
    }

    long balanceLimit = dbManager.getDynamicPropertiesStore().getExchangeBalanceLimit();
    if (firstTokenBalance > balanceLimit || secondTokenBalance > balanceLimit) {
      throw new ContractValidateException("token balance must less than " + balanceLimit);
    }

    if (Arrays.equals(firstTokenID, "_".getBytes())) {
      if (accountWrapper.getBalance() < (firstTokenBalance + calcFee())) {
        throw new ContractValidateException("balance is not enough");
      }
    } else {
      if (!accountWrapper.assetBalanceEnough(firstTokenID, firstTokenBalance)) {
        throw new ContractValidateException("first token balance is not enough");
      }
    }

    if (Arrays.equals(secondTokenID, "_".getBytes())) {
      if (accountWrapper.getBalance() < (secondTokenBalance + calcFee())) {
        throw new ContractValidateException("balance is not enough");
      }
    } else {
      if (!accountWrapper.assetBalanceEnough(secondTokenID, secondTokenBalance)) {
        throw new ContractValidateException("second token balance is not enough");
      }
    }

    return true;
  }


  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ExchangeCreateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return dbManager.getDynamicPropertiesStore().getExchangeCreateFee();
  }

}
