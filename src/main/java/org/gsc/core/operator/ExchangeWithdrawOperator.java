package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
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
import org.gsc.protos.Contract.ExchangeWithdrawContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class ExchangeWithdrawOperator extends AbstractOperator {

  ExchangeWithdrawOperator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final ExchangeWithdrawContract exchangeWithdrawContract = this.contract
              .unpack(ExchangeWithdrawContract.class);
      AccountWrapper accountWrapper = dbManager.getAccountStore()
              .get(exchangeWithdrawContract.getOwnerAddress().toByteArray());

      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore().
              get(ByteArray.fromLong(exchangeWithdrawContract.getExchangeId()));

      byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
      byte[] secondTokenID = exchangeWrapper.getSecondTokenId();
      long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
      long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

      byte[] tokenID = exchangeWithdrawContract.getTokenId().toByteArray();
      long tokenQuant = exchangeWithdrawContract.getQuant();

      byte[] anotherTokenID;
      long anotherTokenQuant;

      BigInteger bigFirstTokenBalance = new BigInteger(String.valueOf(firstTokenBalance));
      BigInteger bigSecondTokenBalance = new BigInteger(String.valueOf(secondTokenBalance));
      BigInteger bigTokenQuant = new BigInteger(String.valueOf(tokenQuant));
      if (Arrays.equals(tokenID, firstTokenID)) {
        anotherTokenID = secondTokenID;
//        anotherTokenQuant = Math
//            .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
        anotherTokenQuant = bigSecondTokenBalance.multiply(bigTokenQuant)
                .divide(bigFirstTokenBalance).longValueExact();
        exchangeWrapper.setBalance(firstTokenBalance - tokenQuant,
                secondTokenBalance - anotherTokenQuant);
      } else {
        anotherTokenID = firstTokenID;
//        anotherTokenQuant = Math
//            .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
        anotherTokenQuant = bigFirstTokenBalance.multiply(bigTokenQuant)
                .divide(bigSecondTokenBalance).longValueExact();
        exchangeWrapper.setBalance(firstTokenBalance - anotherTokenQuant,
                secondTokenBalance - tokenQuant);
      }

      long newBalance = accountWrapper.getBalance() - calcFee();

      if (Arrays.equals(tokenID, "_".getBytes())) {
        accountWrapper.setBalance(newBalance + tokenQuant);
      } else {
        accountWrapper.addAssetAmount(tokenID, tokenQuant);
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
    if (!this.contract.is(ExchangeWithdrawContract.class)) {
      throw new ContractValidateException(
              "contract type error,expected type [ExchangeWithdrawContract],real type[" + contract
                      .getClass() + "]");
    }
    final ExchangeWithdrawContract contract;
    try {
      contract = this.contract.unpack(ExchangeWithdrawContract.class);
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
      throw new ContractValidateException("No enough balance for exchange withdraw fee!");
    }

    ExchangeWrapper exchangeWrapper;
    try {
      exchangeWrapper = dbManager.getExchangeStore().
              get(ByteArray.fromLong(contract.getExchangeId()));
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException("Exchange[" + contract.getExchangeId() + "] not exists");
    }

    if (!accountWrapper.getAddress().equals(exchangeWrapper.getCreatorAddress())) {
      throw new ContractValidateException("account[" + readableOwnerAddress + "] is not creator");
    }

    byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
    byte[] secondTokenID = exchangeWrapper.getSecondTokenId();
    long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
    long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

    byte[] tokenID = contract.getTokenId().toByteArray();
    long tokenQuant = contract.getQuant();

    long anotherTokenQuant;

    if (!Arrays.equals(tokenID, firstTokenID) && !Arrays.equals(tokenID, secondTokenID)) {
      throw new ContractValidateException("token is not in exchange");
    }

    if (tokenQuant <= 0) {
      throw new ContractValidateException("withdraw token quant must greater than zero");
    }

    if (firstTokenBalance == 0 || secondTokenBalance == 0) {
      throw new ContractValidateException("Token balance in exchange is equal with 0,"
              + "the exchange has been closed");
    }

    BigInteger bigFirstTokenBalance = new BigInteger(String.valueOf(firstTokenBalance));
    BigInteger bigSecondTokenBalance = new BigInteger(String.valueOf(secondTokenBalance));
    BigInteger bigTokenQuant = new BigInteger(String.valueOf(tokenQuant));
    if (Arrays.equals(tokenID, firstTokenID)) {
//      anotherTokenQuant = Math
//          .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
      anotherTokenQuant = bigSecondTokenBalance.multiply(bigTokenQuant)
              .divide(bigFirstTokenBalance).longValueExact();
      if (firstTokenBalance < tokenQuant || secondTokenBalance < anotherTokenQuant) {
        throw new ContractValidateException("exchange balance is not enough");
      }
    } else {
//      anotherTokenQuant = Math
//          .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
      anotherTokenQuant = bigFirstTokenBalance.multiply(bigTokenQuant)
              .divide(bigSecondTokenBalance).longValueExact();
      if (secondTokenBalance < tokenQuant || firstTokenBalance < anotherTokenQuant) {
        throw new ContractValidateException("exchange balance is not enough");
      }
    }

    return true;
  }


  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ExchangeWithdrawContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
