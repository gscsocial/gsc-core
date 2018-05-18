package org.gsc.core.operator;

import static org.gsc.config.Parameter.ChainConstant.TRANSFER_FEE;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.exception.BalanceInsufficientException;
import org.gsc.common.exception.ContractExeException;
import org.gsc.common.exception.ContractValidateException;
import org.gsc.common.utils.AddressUtil;
import org.gsc.core.chain.TransactionResultWrapper;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class TransferOperator extends AbstractOperator {

  private TransferContract transferContract;
  private byte[] ownerAddress;
  private byte[] toAddress;
  private long amount;

  TransferOperator(Any contract, Manager dbManager) {
    super(contract, dbManager);
    try {
      transferContract = contract.unpack(TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.error(e.getMessage(), e);
    }
    amount = transferContract.getAmount();
    toAddress = transferContract.getToAddress().toByteArray();
    ownerAddress = transferContract.getOwnerAddress().toByteArray();
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      // if account with to_address does not exist, create it first.
      AccountWrapper toAccount = dbManager.getAccountStore()
          .get(transferContract.getToAddress().toByteArray());
      if (toAccount == null) {
        toAccount = new AccountWrapper(ByteString.copyFrom(toAddress), AccountType.Normal,
            dbManager.getHeadBlockTimeStamp());
        dbManager.getAccountStore().put(toAddress, toAccount);
      }
      dbManager.adjustBalance(transferContract.getOwnerAddress().toByteArray(), -fee);
      ret.setStatus(fee, code.SUCCESS);
      dbManager.adjustBalance(transferContract.getOwnerAddress().toByteArray(), -amount);
      dbManager.adjustBalance(transferContract.getToAddress().toByteArray(), amount);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!this.contract.is(TransferContract.class)) {
        throw new ContractValidateException();
      }
      if (this.dbManager == null) {
        throw new ContractValidateException();
      }
      if (transferContract == null) {
        throw new ContractValidateException(
            "contract type error,expected type [TransferContract],real type[" + contract
                .getClass() + "]");
      }
      if (!AddressUtil.addressValid(ownerAddress)) {
        throw new ContractValidateException("Invalidate ownerAddress");
      }
      if (!AddressUtil.addressValid(toAddress)) {
        throw new ContractValidateException("Invalidate toAddress");
      }

      Preconditions.checkNotNull(transferContract.getAmount(), "Amount is null");

      if (Arrays.equals(toAddress, ownerAddress)) {
        throw new ContractValidateException("Cannot transfer trx to yourself.");
      }

      AccountWrapper ownerAccount = new AccountWrapper();
//      AccountWrapper ownerAccount; = dbManager.getAccountStore()
//          .get(transferContract.getOwnerAddress().toByteArray());

      if (ownerAccount == null) {
        throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
      }

      long balance = ownerAccount.getBalance();

      if (ownerAccount.getBalance() < calcFee()) {
        throw new ContractValidateException("Validate TransferContract error, insufficient fee.");
      }

      if (amount <= 0) {
        throw new ContractValidateException("Amount must greater than 0.");
      }

      if (balance < Math.addExact(amount, calcFee())) {
        throw new ContractValidateException("balance is not sufficient.");
      }

      // if account with to_address is not existed, the minimum amount is 1 TRX
//      AccountWrapper toAccount = dbManager.getAccountStore()
//          .get(transferContract.getToAddress().toByteArray());
//      if (toAccount == null) {
//        long min = dbManager.getDynamicPropertiesStore().getNonExistentAccountTransferMin();
//        if (amount < min) {
//          throw new ContractValidateException(
//              "For a non-existent account transfer, the minimum amount is 1 TRX");
//        }
//      } else {
//        //check to account balance if overflow
//        long toAddressBalance = Math.addExact(toAccount.getBalance(), amount);
//      }
    } catch (Exception ex) {
      throw new ContractValidateException(ex.getMessage());
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(TransferContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return TRANSFER_FEE;
  }
}