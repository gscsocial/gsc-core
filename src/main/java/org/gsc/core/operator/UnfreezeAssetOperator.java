package org.gsc.core.operator;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.Wallet;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.UnfreezeAssetContract;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UnfreezeAssetOperator extends AbstractOperator {

  UnfreezeAssetOperator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final UnfreezeAssetContract unfreezeAssetContract = contract
          .unpack(UnfreezeAssetContract.class);
      byte[] ownerAddress = unfreezeAssetContract.getOwnerAddress().toByteArray();

      AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      long unfreezeAsset = 0L;
      List<Frozen> frozenList = Lists.newArrayList();
      frozenList.addAll(accountWrapper.getFrozenSupplyList());
      Iterator<Frozen> iterator = frozenList.iterator();
      long now = dbManager.getHeadBlockTimeStamp();
      while (iterator.hasNext()) {
        Frozen next = iterator.next();
        if (next.getExpireTime() <= now) {
          unfreezeAsset += next.getFrozenBalance();
          iterator.remove();
        }
      }

      accountWrapper.addAssetAmount(accountWrapper.getAssetIssuedName(), unfreezeAsset);
      accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
          .clearFrozenSupply().addAllFrozenSupply(frozenList).build());
      dbManager.getAccountStore().put(ownerAddress, accountWrapper);
      ret.setStatus(fee, code.SUCCESS);
    } catch (InvalidProtocolBufferException e) {
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
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(UnfreezeAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [UnfreezeAssetContract],real type[" + contract
              .getClass() + "]");
    }
    final UnfreezeAssetContract unfreezeAssetContract;
    try {
      unfreezeAssetContract = this.contract.unpack(UnfreezeAssetContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = unfreezeAssetContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    if (accountWrapper == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    if (accountWrapper.getFrozenSupplyCount() <= 0) {
      throw new ContractValidateException("no frozen supply balance");
    }

    if (accountWrapper.getAssetIssuedName().isEmpty()) {
      throw new ContractValidateException("this account did not issue any asset");
    }

    long now = dbManager.getHeadBlockTimeStamp();
    long allowedUnfreezeCount = accountWrapper.getFrozenSupplyList().stream()
        .filter(frozen -> frozen.getExpireTime() <= now).count();
    if (allowedUnfreezeCount <= 0) {
      throw new ContractValidateException("It's not time to unfreeze asset supply");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(UnfreezeAssetContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
