package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ExchangeWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract.ExchangeTransactionContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class ExchangeTransactionOperator extends AbstractOperator {

  ExchangeTransactionOperator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final ExchangeTransactionContract exchangeTransactionContract = this.contract
          .unpack(ExchangeTransactionContract.class);
      AccountWrapper accountWrapper = dbManager.getAccountStore()
          .get(exchangeTransactionContract.getOwnerAddress().toByteArray());

      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore().
          get(ByteArray.fromLong(exchangeTransactionContract.getExchangeId()));

      byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
      byte[] secondTokenID = exchangeWrapper.getSecondTokenId();

      byte[] tokenID = exchangeTransactionContract.getTokenId().toByteArray();
      long tokenQuant = exchangeTransactionContract.getQuant();

      byte[] anotherTokenID;
      long anotherTokenQuant = exchangeWrapper.transaction(tokenID, tokenQuant);

      if (Arrays.equals(tokenID, firstTokenID)) {
        anotherTokenID = secondTokenID;
      } else {
        anotherTokenID = firstTokenID;
      }

      long newBalance = accountWrapper.getBalance() - calcFee();

      if (Arrays.equals(tokenID, "_".getBytes())) {
        accountWrapper.setBalance(newBalance - tokenQuant);
      } else {
        accountWrapper.reduceAssetAmount(tokenID, tokenQuant);
      }

      if (Arrays.equals(anotherTokenID, "_".getBytes())) {
        accountWrapper.setBalance(newBalance + anotherTokenQuant);
      } else {
        accountWrapper.addAssetAmount(anotherTokenID, anotherTokenQuant);
      }

      dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
      dbManager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);

      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
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
    if (!this.contract.is(ExchangeTransactionContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ExchangeTransactionContract],real type[" + contract
              .getClass() + "]");
    }
    final ExchangeTransactionContract contract;
    try {
      contract = this.contract.unpack(ExchangeTransactionContract.class);
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
      throw new ContractValidateException("No enough balance for exchange transaction fee!");
    }

    ExchangeWrapper exchangeWrapper;
    try {
      exchangeWrapper = dbManager.getExchangeStore().
          get(ByteArray.fromLong(contract.getExchangeId()));
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException("Exchange[" + contract.getExchangeId() + "] not exists");
    }

    byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
    byte[] secondTokenID = exchangeWrapper.getSecondTokenId();
    long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
    long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

    byte[] tokenID = contract.getTokenId().toByteArray();
    long tokenQuant = contract.getQuant();

    if (!Arrays.equals(tokenID, firstTokenID) && !Arrays.equals(tokenID, secondTokenID)) {
      throw new ContractValidateException("token is not in exchange");
    }

    if (tokenQuant <= 0) {
      throw new ContractValidateException("transaction token balance must greater than zero");
    }

    if (firstTokenBalance == 0 || secondTokenBalance == 0) {
      throw new ContractValidateException("Token balance in exchange is equal with 0,"
          + "the exchange has been closed");
    }

    long balanceLimit = dbManager.getDynamicPropertiesStore().getExchangeBalanceLimit();
    long tokenBalance = (Arrays.equals(tokenID, firstTokenID) ? firstTokenBalance
        : secondTokenBalance);
    tokenBalance += tokenQuant;
    if (tokenBalance > balanceLimit) {
      throw new ContractValidateException("token balance must less than " + balanceLimit);
    }

    if (Arrays.equals(tokenID, "_".getBytes())) {
      if (accountWrapper.getBalance() < (tokenQuant + calcFee())) {
        throw new ContractValidateException("balance is not enough");
      }
    } else {
      if (!accountWrapper.assetBalanceEnough(tokenID, tokenQuant)) {
        throw new ContractValidateException("token balance is not enough");
      }
    }

    long anotherTokenQuant = exchangeWrapper.transaction(tokenID, tokenQuant);
    if (anotherTokenQuant < 1) {
      throw new ContractValidateException("token quant is not enough to buy 1 another token");
    }

    return true;
  }


  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ExchangeTransactionContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
