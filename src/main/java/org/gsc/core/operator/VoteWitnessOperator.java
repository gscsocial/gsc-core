package org.gsc.core.operator;

import com.google.common.math.LongMath;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.StringUtil;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.core.Wallet;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.VotesWrapper;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.db.VotesStore;
import org.gsc.db.WitnessStore;
import org.gsc.protos.Contract.VoteWitnessContract;
import org.gsc.protos.Contract.VoteWitnessContract.Vote;
import org.gsc.protos.Protocol.Transaction.Result.code;

import java.util.Iterator;

@Slf4j
public class VoteWitnessOperator extends AbstractOperator {

  VoteWitnessOperator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      VoteWitnessContract voteContract = contract.unpack(VoteWitnessContract.class);
      countVoteAccount(voteContract);
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
    if (!this.contract.is(VoteWitnessContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [VoteWitnessContract],real type[" + contract
              .getClass() + "]");
    }
    final VoteWitnessContract contract;
    try {
      contract = this.contract.unpack(VoteWitnessContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    if (!Wallet.addressValid(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Invalid address");
    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    AccountStore accountStore = dbManager.getAccountStore();
    WitnessStore witnessStore = dbManager.getWitnessStore();

    if (contract.getVotesCount() == 0) {
      return true;
      /*throw new ContractValidateException(
          "VoteNumber must more than 0");*/
    }
    int maxVoteNumber = ChainConstant.MAX_VOTE_NUMBER;
    if (contract.getVotesCount() > maxVoteNumber) {
      throw new ContractValidateException(
          "VoteNumber more than maxVoteNumber " + maxVoteNumber);
    }
    try {
      Iterator<Vote> iterator = contract.getVotesList().iterator();
      Long sum = 0L;
      while (iterator.hasNext()) {
        Vote vote = iterator.next();
        byte[] witnessCandidate = vote.getVoteAddress().toByteArray();
        if (!Wallet.addressValid(witnessCandidate)) {
          throw new ContractValidateException("Invalid vote address!");
        }
        long voteCount = vote.getVoteCount();
        if (voteCount <= 0) {
          throw new ContractValidateException("vote count must be greater than 0");
        }
        String readableWitnessAddress = StringUtil.createReadableString(vote.getVoteAddress());
        if (!accountStore.has(witnessCandidate)) {
          throw new ContractValidateException(
              "Account[" + readableWitnessAddress + "] not exists");
        }
        if (!witnessStore.has(witnessCandidate)) {
          throw new ContractValidateException(
              "Witness[" + readableWitnessAddress + "] not exists");
        }
        sum = LongMath.checkedAdd(sum, vote.getVoteCount());
      }

      AccountWrapper accountWrapper = accountStore.get(ownerAddress);
      if (accountWrapper == null) {
        throw new ContractValidateException(
            "Account[" + readableOwnerAddress + "] not exists");
      }

      long gscPower = accountWrapper.getGSCPower();

      sum = LongMath.checkedMultiply(sum, 1000000L); //trx -> drop. The vote count is based on TRX
      if (sum > gscPower) {
        throw new ContractValidateException(
            "The total number of votes[" + sum + "] is greater than the gscPower[" + gscPower
                + "]");
      }
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  /**
   * message VoteWitnessContract {
   *   message Vote {
   *     bytes vote_address = 1;
   *     int64 vote_count = 2;
   *   }
   *   bytes owner_address = 1;
   *   repeated Vote votes = 2;
   *   bool support = 3;
   * }
   */
  private void countVoteAccount(VoteWitnessContract voteContract) {
    byte[] ownerAddress = voteContract.getOwnerAddress().toByteArray();

    VotesWrapper votesWrapper;
    VotesStore votesStore = dbManager.getVotesStore();
    AccountStore accountStore = dbManager.getAccountStore();

    AccountWrapper accountWrapper = accountStore.get(ownerAddress);

    if (!votesStore.has(ownerAddress)) {
      votesWrapper = new VotesWrapper(voteContract.getOwnerAddress(),
          accountWrapper.getVotesList());
    } else {
      votesWrapper = votesStore.get(ownerAddress);
    }

    accountWrapper.clearVotes();
    votesWrapper.clearNewVotes();

    voteContract.getVotesList().forEach(vote -> {
      logger.debug("countVoteAccount,address[{}]",
          ByteArray.toHexString(vote.getVoteAddress().toByteArray()));

      votesWrapper.addNewVotes(vote.getVoteAddress(), vote.getVoteCount());
      accountWrapper.addVotes(vote.getVoteAddress(), vote.getVoteCount());
    });

    accountStore.put(accountWrapper.createDbKey(), accountWrapper);
    votesStore.put(ownerAddress, votesWrapper);
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(VoteWitnessContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
