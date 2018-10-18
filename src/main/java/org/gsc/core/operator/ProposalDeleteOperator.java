package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.ProposalWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract.ProposalDeleteContract;
import org.gsc.protos.Protocol.Proposal.State;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class ProposalDeleteOperator extends AbstractOperator {

  ProposalDeleteOperator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final ProposalDeleteContract proposalDeleteContract = this.contract
              .unpack(ProposalDeleteContract.class);
      ProposalWrapper proposalWrapper = dbManager.getProposalStore().
              get(ByteArray.fromLong(proposalDeleteContract.getProposalId()));

      proposalWrapper.setState(State.CANCELED);
      dbManager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (ItemNotFoundException e) {
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
    if (!this.contract.is(ProposalDeleteContract.class)) {
      throw new ContractValidateException(
              "contract type error,expected type [ProposalDeleteContract],real type[" + contract
                      .getClass() + "]");
    }
    final ProposalDeleteContract contract;
    try {
      contract = this.contract.unpack(ProposalDeleteContract.class);
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

    if (contract.getProposalId() > dbManager.getDynamicPropertiesStore().getLatestProposalNum()) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] not exists");
    }

    ProposalWrapper proposalWrapper = null;
    try {
      proposalWrapper = dbManager.getProposalStore().
              get(ByteArray.fromLong(contract.getProposalId()));
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] not exists");
    }

    long now = dbManager.getHeadBlockTimeStamp();
    if (!proposalWrapper.getProposalAddress().equals(contract.getOwnerAddress())) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] "
              + "is not proposed by " + readableOwnerAddress);
    }
    if (now >= proposalWrapper.getExpirationTime()) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] expired");
    }
    if (proposalWrapper.getState() == State.CANCELED) {
      throw new ContractValidateException("Proposal[" + contract.getProposalId() + "] canceled");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ProposalDeleteContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
