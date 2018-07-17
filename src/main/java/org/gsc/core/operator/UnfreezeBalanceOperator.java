package org.gsc.core.operator;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.wrapper.AccountCapsule;
import org.gsc.core.wrapper.TransactionResultCapsule;
import org.gsc.core.wrapper.VotesCapsule;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.Wallet;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UnfreezeBalanceOperator extends AbstractOperator {

  UnfreezeBalanceOperator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
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

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    long oldBalance = accountCapsule.getBalance();
    long unfreezeBalance = 0L;
    List<Frozen> frozenList = Lists.newArrayList();
    frozenList.addAll(accountCapsule.getFrozenList());
    Iterator<Frozen> iterator = frozenList.iterator();
    long now = dbManager.getHeadBlockTimeStamp();
    while (iterator.hasNext()) {
      Frozen next = iterator.next();
      if (next.getExpireTime() <= now) {
        unfreezeBalance += next.getFrozenBalance();
        iterator.remove();
      }
    }

    accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
        .setBalance(oldBalance + unfreezeBalance)
        .clearFrozen().addAllFrozen(frozenList).build());

    VotesCapsule votesCapsule;
    if (!dbManager.getVotesStore().has(ownerAddress)) {
      votesCapsule = new VotesCapsule(unfreezeBalanceContract.getOwnerAddress(),
          accountCapsule.getVotesList());
    } else {
      votesCapsule = dbManager.getVotesStore().get(ownerAddress);
    }
    accountCapsule.clearVotes();
    votesCapsule.clearNewVotes();

    dbManager.getAccountStore().put(ownerAddress, accountCapsule);
    dbManager.getVotesStore().put(ownerAddress, votesCapsule);
    dbManager.getDynamicPropertiesStore().addTotalNetWeight(-unfreezeBalance / 1000_000L);

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

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }
    if (accountCapsule.getFrozenCount() <= 0) {
      throw new ContractValidateException("no frozenBalance");
    }

    long now = dbManager.getHeadBlockTimeStamp();
    long allowedUnfreezeCount = accountCapsule.getFrozenList().stream()
        .filter(frozen -> frozen.getExpireTime() <= now).count();
    if (allowedUnfreezeCount <= 0) {
      throw new ContractValidateException("It's not time to unfreeze.");
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
