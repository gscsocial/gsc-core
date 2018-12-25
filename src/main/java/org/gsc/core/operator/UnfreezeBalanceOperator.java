package org.gsc.core.operator;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.VotesWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Protocol.Account.AccountResource;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.Transaction.Result.code;

import java.util.Iterator;
import java.util.List;

@Slf4j
public class UnfreezeBalanceOperator extends AbstractOperator {

  private static final Long DURATION = 3 * 86400000L; // 3 DAYS

  UnfreezeBalanceOperator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    final UnfreezeBalanceContract unfreezeBalanceContract;
    try {
      unfreezeBalanceContract = contract.unpack(UnfreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    byte[] ownerAddress = unfreezeBalanceContract.getOwnerAddress().toByteArray();

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    long oldBalance = accountWrapper.getBalance();
    long unfreezeBalance = 0L;
    switch (unfreezeBalanceContract.getType()){
      case ACTIVATE:
        switch (unfreezeBalanceContract.getResource()) {
          case BANDWIDTH:
            List<Frozen> frozenList = Lists.newArrayList();
            //frozenList.addAll(accountWrapper.getFrozenList());
            Iterator<Frozen> iterator = accountWrapper.getFrozenList().iterator();
            long now = dbManager.getHeadBlockTimeStamp();
            while (iterator.hasNext()) {
              Frozen next = iterator.next();
              if (next.getExpireTime() == 0L) {
                unfreezeBalance += next.getFrozenBalance();
                next.toBuilder().setExpireTime(now + DURATION).build();
              }
              frozenList.add(next);
            }

            accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                    .setBalance(oldBalance)
                    .clearFrozen().addAllFrozen(frozenList).build());

            dbManager.getDynamicPropertiesStore().addTotalNetWeight(-unfreezeBalance / 1000_000L);
            break;
          case ENERGY:
            unfreezeBalance = accountWrapper.getAccountResource().getFrozenBalanceForEnergy()
                    .getFrozenBalance();

            AccountResource newAccountResource = accountWrapper.getAccountResource().toBuilder()
                    .clearFrozenBalanceForEnergy().build();
            accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                    .setBalance(oldBalance + unfreezeBalance)
                    .setAccountResource(newAccountResource).build());

            dbManager.getDynamicPropertiesStore().addTotalEnergyWeight(-unfreezeBalance / 1000_000L);
            break;
        }

        VotesWrapper votesCapsule;
        if (!dbManager.getVotesStore().has(ownerAddress)) {
          votesCapsule = new VotesWrapper(unfreezeBalanceContract.getOwnerAddress(),
                  accountWrapper.getVotesList());
        } else {
          votesCapsule = dbManager.getVotesStore().get(ownerAddress);
        }
        accountWrapper.clearVotes();
        votesCapsule.clearNewVotes();

        dbManager.getAccountStore().put(ownerAddress, accountWrapper);
        dbManager.getVotesStore().put(ownerAddress, votesCapsule);
        break;
      case RECOVER: // withdraw the active unfreezeBalance
        switch (unfreezeBalanceContract.getResource()) {
          case BANDWIDTH:

            List<Frozen> frozenList = Lists.newArrayList();
            frozenList.addAll(accountWrapper.getFrozenList());
            Iterator<Frozen> iterator = frozenList.iterator();
            long now = dbManager.getHeadBlockTimeStamp();
            while (iterator.hasNext()) {
              Frozen next = iterator.next();
              if (next.getExpireTime() <= now && next.getExpireTime() > 0) { //已经提交过解冻 并且已经到期
                unfreezeBalance += next.getFrozenBalance();
                iterator.remove();
              }
            }

            accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                    .setBalance(oldBalance + unfreezeBalance)
                    .clearFrozen().addAllFrozen(frozenList).build());

            dbManager.getDynamicPropertiesStore().addTotalNetWeight(-unfreezeBalance / 1000_000L);
            break;
          case ENERGY:
            unfreezeBalance = accountWrapper.getAccountResource().getFrozenBalanceForEnergy()
                    .getFrozenBalance();

            AccountResource newAccountResource = accountWrapper.getAccountResource().toBuilder()
                    .clearFrozenBalanceForEnergy().build();
            accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                    .setBalance(oldBalance + unfreezeBalance)
                    .setAccountResource(newAccountResource).build());

            dbManager.getDynamicPropertiesStore().addTotalEnergyWeight(-unfreezeBalance / 1000_000L);
            break;
        }
        dbManager.getAccountStore().put(ownerAddress, accountWrapper);
        break;
    }
    ret.setUnfreezeAmount(unfreezeBalance);
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
    if (!this.contract.is(UnfreezeBalanceContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [UnfreezeBalanceContract],real type[" + contract
              .getClass() + "]");
    }
    final UnfreezeBalanceContract unfreezeBalanceContract;
    try {
      unfreezeBalanceContract = this.contract.unpack(UnfreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = unfreezeBalanceContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    if (accountWrapper == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    long now = dbManager.getHeadBlockTimeStamp();

    switch (unfreezeBalanceContract.getResource()) {
      case BANDWIDTH:
        if (accountWrapper.getFrozenCount() <= 0) {
          throw new ContractValidateException("no frozenBalance");
        }
        switch (unfreezeBalanceContract.getType()){
          case ACTIVATE:
            long allowedUnfreezeCount = accountWrapper.getFrozenList().stream()
                    .filter(frozen -> frozen.getExpireTime() == 0).count();
            if(allowedUnfreezeCount <= 0)
              throw new ContractValidateException("no frozenBalance");
            break;
          case RECOVER:
            long allowedWithdrawCount = accountWrapper.getFrozenList().stream()
                    .filter(frozen -> frozen.getExpireTime() <= now && frozen.getExpireTime() > 0).count();
            long toWithdrawCount = accountWrapper.getFrozenList().stream()
                    .filter(frozen -> frozen.getExpireTime() >  0).count();
            if(toWithdrawCount <= 0){
              throw new ContractValidateException("no active frozenBalance");
            }
            if (allowedWithdrawCount <= 0) {
              throw new ContractValidateException("It's not time to withdraw.");
            }
            break;
        }
        break;
      case ENERGY:
        Frozen frozenBalanceForEnergy = accountWrapper.getAccountResource()
            .getFrozenBalanceForEnergy();
        if (frozenBalanceForEnergy.getFrozenBalance() <= 0) {
          throw new ContractValidateException("no frozenBalance");
        }
        if (frozenBalanceForEnergy.getExpireTime() > now) {
          throw new ContractValidateException("It's not time to unfreeze.");
        }

        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error.valid ResourceCode[BANDWIDTH、ENERGY]");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(UnfreezeBalanceContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
