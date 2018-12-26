package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.StringUtil;
import org.gsc.config.Parameter;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Protocol.Account.AccountResource;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class FreezeBalanceOperator extends AbstractOperator {

  FreezeBalanceOperator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    final FreezeBalanceContract freezeBalanceContract;
    try {
      freezeBalanceContract = contract.unpack(FreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(freezeBalanceContract.getOwnerAddress().toByteArray());

    long newBalance = accountWrapper.getBalance() - freezeBalanceContract.getFrozenBalance();

    switch (freezeBalanceContract.getResource()) {
      case BANDWIDTH:
        Frozen newFrozen = Frozen.newBuilder()
            .setFrozenBalance(freezeBalanceContract.getFrozenBalance())
            .setExpireTime(0L) // set expireTime to 0 as inactive frozen balance
            .build();
        accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                .addFrozen(newFrozen)
                .setBalance(newBalance)
                .build());

        dbManager.getDynamicPropertiesStore()
            .addTotalNetWeight(freezeBalanceContract.getFrozenBalance() / 1000_000L);
        break;
      case ENERGY:
        long currentFrozenBalanceForEnergy = accountWrapper.getAccountResource()
            .getFrozenBalanceForEnergy()
            .getFrozenBalance();
        long newFrozenBalanceForEnergy =
            freezeBalanceContract.getFrozenBalance() + currentFrozenBalanceForEnergy;

        Frozen newFrozenForEnergy = Frozen.newBuilder()
            .setFrozenBalance(newFrozenBalanceForEnergy)
            //.setExpireTime(now + duration)
            .setExpireTime(0) // set to 0 as mark of inactive frozen balance
            .build();

        AccountResource newAccountResource = accountWrapper.getAccountResource().toBuilder()
            .setFrozenBalanceForEnergy(newFrozenForEnergy).build();

        accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
            .setAccountResource(newAccountResource)
            .setBalance(newBalance)
            .build());
        dbManager.getDynamicPropertiesStore()
            .addTotalEnergyWeight(freezeBalanceContract.getFrozenBalance() / 1000_000L);
        break;
    }

    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

    ret.setStatus(fee, code.SUCESS);

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
    if (!contract.is(FreezeBalanceContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [FreezeBalanceContract],real type[" + contract
              .getClass() + "]");
    }

    final FreezeBalanceContract freezeBalanceContract;
    try {
      freezeBalanceContract = this.contract.unpack(FreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = freezeBalanceContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    if (accountWrapper == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    long frozenBalance = freezeBalanceContract.getFrozenBalance();
    if (frozenBalance <= 0) {
      throw new ContractValidateException("frozenBalance must be positive");
    }
    if (frozenBalance < 1_000_000L) {
      throw new ContractValidateException("frozenBalance must be more than 1GSC");
    }

//    int frozenCount = accountWrapper.getFrozenCount();
//    if (!(frozenCount == 0 || frozenCount == 1)) {
//      throw new ContractValidateException("frozenCount must be 0 or 1");
//    }
    if (frozenBalance > accountWrapper.getBalance()) {
      throw new ContractValidateException("frozenBalance must be less than accountBalance");
    }

//    long maxFrozenNumber = dbManager.getDynamicPropertiesStore().getMaxFrozenNumber();
//    if (accountWrapper.getFrozenCount() >= maxFrozenNumber) {
//      throw new ContractValidateException("max frozen number is: " + maxFrozenNumber);
//    }

    long frozenDuration = freezeBalanceContract.getFrozenDuration();
    long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
    long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();

    if (!(frozenDuration >= minFrozenTime && frozenDuration <= maxFrozenTime)) {
      throw new ContractValidateException(
          "frozenDuration must be less than " + maxFrozenTime + " days "
              + "and more than " + minFrozenTime + " days");
    }

    switch (freezeBalanceContract.getResource()) {
      case BANDWIDTH:
        break;
      case ENERGY:
        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error,valid ResourceCode[BANDWIDTH、ENERGY]");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(FreezeBalanceContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
