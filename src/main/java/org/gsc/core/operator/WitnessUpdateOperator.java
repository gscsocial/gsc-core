package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.TransactionResultCapsule;
import org.gsc.core.wrapper.WitnessCapsule;
import org.gsc.core.wrapper.utils.TransactionUtil;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.WitnessUpdateContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class WitnessUpdateOperator extends AbstractOperator {

  WitnessUpdateOperator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  private void updateWitness(final WitnessUpdateContract contract) {
    WitnessCapsule witnessCapsule = this.dbManager.getWitnessStore()
        .get(contract.getOwnerAddress().toByteArray());
    witnessCapsule.setUrl(contract.getUpdateUrl().toStringUtf8());
    this.dbManager.getWitnessStore().put(witnessCapsule.createDbKey(), witnessCapsule);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final WitnessUpdateContract witnessUpdateContract = this.contract
          .unpack(WitnessUpdateContract.class);
      this.updateWitness(witnessUpdateContract);
      ret.setStatus(fee, code.SUCESS);
    } catch (final InvalidProtocolBufferException e) {
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
    if (!this.contract.is(WitnessUpdateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [WitnessUpdateContract],real type[" + contract
              .getClass() + "]");
    }
    final WitnessUpdateContract contract;
    try {
      contract = this.contract.unpack(WitnessUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!this.dbManager.getAccountStore().has(ownerAddress)) {
      throw new ContractValidateException("account does not exist");
    }

    if (!TransactionUtil.validUrl(contract.getUpdateUrl().toByteArray())) {
      throw new ContractValidateException("Invalid url");
    }

    if (!this.dbManager.getWitnessStore().has(ownerAddress)) {
      throw new ContractValidateException("Witness does not exist");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(WitnessUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
