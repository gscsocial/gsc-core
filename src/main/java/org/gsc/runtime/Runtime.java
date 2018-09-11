package org.gsc.runtime;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.gsc.runtime.utils.MUtil.convertTogscAddress;
import static org.gsc.runtime.utils.MUtil.transfer;
import static org.gsc.runtime.vm.VMUtils.saveProgramTraceFile;
import static org.gsc.runtime.vm.VMUtils.zipAndEncode;
import static org.gsc.runtime.vm.program.InternalTransaction.ExecutorType.ET_NORMAL_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.ExecutorType.ET_PRE_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.ExecutorType.ET_UNKNOWN_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_UNKNOWN_TYPE;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.operator.Operator;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.joda.time.DateTime;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.config.SystemProperties;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.PrecompiledContracts;
import org.gsc.runtime.vm.VM;
import org.gsc.runtime.vm.program.InternalTransaction;
import org.gsc.runtime.vm.program.InternalTransaction.ExecutorType;
import org.gsc.runtime.vm.program.Program;
import org.gsc.runtime.vm.program.Program.JVMStackOverFlowException;
import org.gsc.runtime.vm.program.ProgramPrecompile;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.runtime.vm.program.invoke.ProgramInvoke;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactory;
import org.gsc.common.storage.Deposit;
import org.gsc.common.storage.DepositImpl;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.operator.OperatorFactory;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.EnergyProcessor;
import org.gsc.db.StorageMarket;
import org.gsc.db.TransactionTrace;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.SmartContract.ABI;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;

@Slf4j(topic = "Runtime")
public class Runtime {


  private SystemProperties config = SystemProperties.getInstance();

  private Transaction trx;
  private BlockWrapper blockCap = null;
  private Deposit deposit;
  private ProgramInvokeFactory programInvokeFactory = null;
  private String runtimeError;

  private EnergyProcessor energyProcessor = null;
  private StorageMarket storageMarket = null;
  PrecompiledContracts.PrecompiledContract precompiledContract = null;
  private ProgramResult result = new ProgramResult();


  private VM vm = null;
  private Program program = null;

  private InternalTransaction.TrxType trxType = TRX_UNKNOWN_TYPE;
  private ExecutorType executorType = ET_UNKNOWN_TYPE;

  //tx trace
  private TransactionTrace trace;


  /**
   * For blockCap's trx run
   */
  public Runtime(TransactionTrace trace, BlockWrapper block, Deposit deposit,
                 ProgramInvokeFactory programInvokeFactory) {
    this.trace = trace;
    this.trx = trace.getTrx().getInstance();

    if (Objects.nonNull(block)) {
      this.blockCap = block;
      this.executorType = ET_NORMAL_TYPE;
    } else {
      this.blockCap = new BlockWrapper(Block.newBuilder().build());
      this.executorType = ET_PRE_TYPE;
    }
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    this.energyProcessor = new EnergyProcessor(deposit.getDbManager());
    this.storageMarket = new StorageMarket(deposit.getDbManager());

    Transaction.Contract.ContractType contractType = this.trx.getRawData().getContract(0).getType();
    switch (contractType.getNumber()) {
      case ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TRX_PRECOMPILED_TYPE;
    }
  }


  /**
   * For constant trx with latest blockCap.
   */
  public Runtime(Transaction tx, BlockWrapper block, DepositImpl deposit,
                 ProgramInvokeFactory programInvokeFactory) {
    this.trx = tx;
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    this.executorType = ET_PRE_TYPE;
    this.blockCap = block;
    this.energyProcessor = new EnergyProcessor(deposit.getDbManager());
    this.storageMarket = new StorageMarket(deposit.getDbManager());
    Transaction.Contract.ContractType contractType = tx.getRawData().getContract(0).getType();
    switch (contractType.getNumber()) {
      case Transaction.Contract.ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case Transaction.Contract.ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TRX_PRECOMPILED_TYPE;
    }
  }


  public void precompiled() throws ContractValidateException, ContractExeException {
    TransactionWrapper trxCap = new TransactionWrapper(trx);
    final List<Operator> operatorList = OperatorFactory
        .createActuator(trxCap, deposit.getDbManager());

    for (Operator act : operatorList) {
      act.validate();
      act.execute(result.getRet());
    }
  }


  public BigInteger getBlockCPULeftInUs() {

    // insure blockCap is not null
    BigInteger curBlockHaveElapsedCPUInUs =
        BigInteger.valueOf(
            1000 * (DateTime.now().getMillis() - blockCap.getInstance().getBlockHeader()
                .getRawData()
                .getTimestamp())); // us
    BigInteger curBlockCPULimitInUs = BigInteger.valueOf((long)
        (1000 * ChainConstant.BLOCK_PRODUCED_INTERVAL * 0.5
            * ChainConstant.BLOCK_PRODUCED_TIME_OUT
            / 100)); // us

    return curBlockCPULimitInUs.subtract(curBlockHaveElapsedCPUInUs);

  }

  public void execute() throws ContractValidateException, ContractExeException {
    switch (trxType) {
      case TRX_PRECOMPILED_TYPE:
        precompiled();
        break;
      case TRX_CONTRACT_CREATION_TYPE:
        create();
        break;
      case TRX_CONTRACT_CALL_TYPE:
        call();
        break;
      default:
        throw new ContractValidateException("Unknown contract type");
    }
  }

  private long getEnergyLimit(AccountWrapper account, long feeLimit, long callValue) {

    long SUN_PER_ENERGY = deposit.getDbManager().getDynamicPropertiesStore().getEnergyFee() == 0
        ? Constant.SUN_PER_ENERGY :
        deposit.getDbManager().getDynamicPropertiesStore().getEnergyFee();
    // can change the calc way
    long leftEnergyFromFreeze = energyProcessor.getAccountLeftEnergyFromFreeze(account);
    callValue = max(callValue, 0);
    long energyFromBalance = Math
        .floorDiv(max(account.getBalance() - callValue, 0), SUN_PER_ENERGY);

    long energyFromFeeLimit;
    long totalBalanceForEnergyFreeze = account.getAccountResource().getFrozenBalanceForEnergy()
        .getFrozenBalance();
    if (0 == totalBalanceForEnergyFreeze) {
      energyFromFeeLimit =
          feeLimit / SUN_PER_ENERGY;
    } else {
      long totalEnergyFromFreeze = energyProcessor
          .calculateGlobalEnergyLimit(totalBalanceForEnergyFreeze);
      long leftBalanceForEnergyFreeze = getEnergyFee(totalBalanceForEnergyFreeze,
          leftEnergyFromFreeze,
          totalEnergyFromFreeze);

      if (leftBalanceForEnergyFreeze >= feeLimit) {
        energyFromFeeLimit = BigInteger.valueOf(totalEnergyFromFreeze)
            .multiply(BigInteger.valueOf(feeLimit))
            .divide(BigInteger.valueOf(totalBalanceForEnergyFreeze)).longValueExact();
      } else {
        energyFromFeeLimit = Math
            .addExact(leftEnergyFromFreeze,
                (feeLimit - leftBalanceForEnergyFreeze) / SUN_PER_ENERGY);
      }
    }

    return min(Math.addExact(leftEnergyFromFreeze, energyFromBalance), energyFromFeeLimit);
  }

  private long getEnergyLimit(AccountWrapper creator, AccountWrapper caller,
                              TriggerSmartContract contract, long feeLimit, long callValue) {

    long callerEnergyLimit = getEnergyLimit(caller, feeLimit, callValue);
    if (Arrays.equals(creator.getAddress().toByteArray(), caller.getAddress().toByteArray())) {
      return callerEnergyLimit;
    }

    // creatorEnergyFromFreeze
    long creatorEnergyLimit = energyProcessor.getAccountLeftEnergyFromFreeze(creator);

    SmartContract smartContract = this.deposit
        .getContract(contract.getContractAddress().toByteArray()).getInstance();
    long consumeUserResourcePercent = smartContract.getConsumeUserResourcePercent();

    consumeUserResourcePercent = max(0, min(consumeUserResourcePercent, 100));

    if (consumeUserResourcePercent <= 0) {
      return creatorEnergyLimit;
    }

    if (creatorEnergyLimit * consumeUserResourcePercent
        >= (100 - consumeUserResourcePercent) * callerEnergyLimit) {
      return Math.floorDiv(callerEnergyLimit * 100, consumeUserResourcePercent);
    } else {
      return Math.addExact(callerEnergyLimit, creatorEnergyLimit);
    }
  }

  private double getThisTxCPULimitInUsRatio() {

    double thisTxCPULimitInUsRatio;

    if (ET_NORMAL_TYPE == executorType) {
      // self witness 2
      if (this.blockCap != null && blockCap.generatedByMyself &&
          this.blockCap.getInstance().getBlockHeader().getWitnessSignature().isEmpty()) {
        thisTxCPULimitInUsRatio = 1.0;
      } else
      // self witness 3, other witness 3, fullnode 2
      {
        if (trx.getRet(0).getContractRet() == contractResult.OUT_OF_TIME) {
          thisTxCPULimitInUsRatio = Args.getInstance().getMinTimeRatio();
        } else {
          thisTxCPULimitInUsRatio = Args.getInstance().getMaxTimeRatio();
        }
      }
    } else {
      // self witness 1, other witness 1, fullnode 1
      thisTxCPULimitInUsRatio = 1.0;
    }

    return thisTxCPULimitInUsRatio;
  }

  /*
   **/
  private void create()
      throws  ContractValidateException {
    if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
      throw new ContractValidateException("vm work is off, need to be opened by the committee");
    }

    CreateSmartContract contract = ContractWrapper.getSmartContractFromTransaction(trx);
    SmartContract newSmartContract = contract.getNewContract();

    byte[] code = newSmartContract.getBytecode().toByteArray();
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();

    long percent = contract.getNewContract().getConsumeUserResourcePercent();
    if (percent < 0 || percent > 100) {
      throw new ContractValidateException("percent must be >= 0 and <= 100");
    }

    // insure the new contract address haven't exist
    if (deposit.getAccount(contractAddress) != null) {
      throw new ContractValidateException(
          "Trying to create a contract with existing contract address: " + Wallet
              .encode58Check(contractAddress));
    }

    newSmartContract = newSmartContract.toBuilder()
        .setContractAddress(ByteString.copyFrom(contractAddress)).build();
    long callValue = newSmartContract.getCallValue();
    // create vm to constructor smart contract
    try {

      AccountWrapper creator = this.deposit
          .getAccount(newSmartContract.getOriginAddress().toByteArray());
      // if (executorType == ET_NORMAL_TYPE) {
      //   long blockENERGYLeftInUs = getBlockENERGYLeftInUs().longValueExact();
      //   thisTxENERGYLimitInUs = min(blockENERGYLeftInUs,
      //       Constant.ENERGY_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);
      // } else {
      //   thisTxENERGYLimitInUs = Constant.ENERGY_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT;
      // }

      long MAX_CPU_TIME_OF_ONE_TX = deposit.getDbManager().getDynamicPropertiesStore()
          .getMaxCpuTimeOfOneTX() * 1000;

      long thisTxCPULimitInUs = (long) (MAX_CPU_TIME_OF_ONE_TX * getThisTxCPULimitInUsRatio());

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      long feeLimit = trx.getRawData().getFeeLimit();
      long energyLimit = getEnergyLimit(creator, feeLimit, callValue);
      byte[] ops = newSmartContract.getBytecode().toByteArray();
      InternalTransaction internalTransaction = new InternalTransaction(trx);

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CREATION_TYPE, executorType, trx,
              blockCap.getInstance(), deposit, vmStartInUs, vmShouldEndInUs, energyLimit);
      this.vm = new VM(config);
      this.program = new Program(ops, programInvoke, internalTransaction, config, this.blockCap);
      Program.setRootTransactionId(new TransactionWrapper(trx).getTransactionId().getBytes());
      Program.resetNonce();
      Program.setRootCallConstant(isCallConstant());
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new ContractValidateException(e.getMessage());
    }

    program.getResult().setContractAddress(contractAddress);

    deposit.createAccount(contractAddress, newSmartContract.getName(),
        Protocol.AccountType.Contract);

    deposit.createContract(contractAddress, new ContractWrapper(newSmartContract));
    deposit.saveCode(contractAddress, ProgramPrecompile.getCode(code));
    // deposit.createContractByNormalAccountIndex(ownerAddress, new BytesWrapper(contractAddress));

    // transfer from callerAddress to contractAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    if (callValue > 0) {
      transfer(this.deposit, callerAddress, contractAddress, callValue);
    }

  }

  /**
   * **
   */

  private void call()
      throws  ContractValidateException {

    if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
      throw new ContractValidateException("VM work is off, need to be opened by the committee");
    }

    Contract.TriggerSmartContract contract = ContractWrapper.getTriggerContractFromTransaction(trx);
    if (contract == null) {
      return;
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    byte[] code = this.deposit.getCode(contractAddress);
    long callValue = contract.getCallValue();
    if (isEmpty(code)) {

    } else {

      AccountWrapper caller = this.deposit.getAccount(contract.getOwnerAddress().toByteArray());
      AccountWrapper creator = this.deposit.getAccount(
          this.deposit.getContract(contractAddress).getInstance()
              .getOriginAddress().toByteArray());

      long MAX_CPU_TIME_OF_ONE_TX = deposit.getDbManager().getDynamicPropertiesStore()
          .getMaxCpuTimeOfOneTX() * 1000;
      long thisTxCPULimitInUs =
          (long) (MAX_CPU_TIME_OF_ONE_TX * getThisTxCPULimitInUsRatio());

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      long feeLimit = trx.getRawData().getFeeLimit();
      long energyLimit;
      if (isCallConstant(contractAddress)) {
        energyLimit = Constant.MAX_ENERGY_IN_TX;
      } else {
        energyLimit = getEnergyLimit(creator, caller, contract, feeLimit, callValue);
      }

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CALL_TYPE, executorType, trx,
              blockCap.getInstance(), deposit, vmStartInUs, vmShouldEndInUs, energyLimit);
      this.vm = new VM(config);
      InternalTransaction internalTransaction = new InternalTransaction(trx);
      this.program = new Program(null, code, programInvoke, internalTransaction, config,
          this.blockCap);
      Program.setRootTransactionId(new TransactionWrapper(trx).getTransactionId().getBytes());
      Program.resetNonce();
      Program.setRootCallConstant(isCallConstant());
    }

    program.getResult().setContractAddress(contractAddress);
    //transfer from callerAddress to targetAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    if (callValue > 0) {
      transfer(this.deposit, callerAddress, contractAddress, callValue);
    }

  }

  public void go() {
    try {
      if (vm != null) {
        vm.play(program);

        program.getResult().setRet(result.getRet());
        result = program.getResult();

        if (isCallConstant()) {
          long callValue = TransactionWrapper.getCallValue(trx.getRawData().getContract(0));
          if (callValue > 0) {
            runtimeError = "constant cannot set call value.";
          }
          return;
        }

        if (result.getException() != null || result.isRevert()) {
          result.getDeleteAccounts().clear();
          result.getLogInfoList().clear();
          result.resetFutureRefund();

          if (result.getException() != null) {
            program.spendAllEnergy();
            runtimeError = result.getException().getMessage();
            throw result.getException();
          } else {
            runtimeError = "REVERT opcode executed";
          }
        } else {
          deposit.commit();
        }
      } else {
        deposit.commit();
      }
    } catch (JVMStackOverFlowException e) {
      result.setException(e);
      runtimeError = result.getException().getMessage();
      logger.error("runtime error is :{}", result.getException().getMessage());
    } catch (Throwable e) {
      if (Objects.isNull(result.getException())) {
        logger.error(e.getMessage(), e);
        result.setException(new RuntimeException("Unknown Throwable"));
      }
      if (StringUtils.isEmpty(runtimeError)) {
        runtimeError = result.getException().getMessage();
      }
      logger.error("runtime error is :{}", result.getException().getMessage());
    }
    trace.setBill(result.getEnergyUsed());
  }

  private long getEnergyFee(long callerEnergyUsage, long callerEnergyFrozen,
      long callerEnergyTotal) {
    if (callerEnergyTotal <= 0) {
      return 0;
    }
    return BigInteger.valueOf(callerEnergyFrozen).multiply(BigInteger.valueOf(callerEnergyUsage))
        .divide(BigInteger.valueOf(callerEnergyTotal)).longValueExact();
  }

  public boolean isCallConstant() throws ContractValidateException {

    TriggerSmartContract triggerContractFromTransaction = ContractWrapper
        .getTriggerContractFromTransaction(trx);
    if (TRX_CONTRACT_CALL_TYPE.equals(trxType)) {
      ABI abi = deposit
          .getContract(triggerContractFromTransaction.getContractAddress().toByteArray())
          .getInstance().getAbi();
      if (Wallet.isConstant(abi, triggerContractFromTransaction)) {
        return true;
      }
    }
    return false;
  }

  private boolean isCallConstant(byte[] address) throws ContractValidateException {

    if (TRX_CONTRACT_CALL_TYPE.equals(trxType)) {
      ABI abi = deposit.getContract(address).getInstance().getAbi();
      if (Wallet.isConstant(abi, ContractWrapper.getTriggerContractFromTransaction(trx))) {
        return true;
      }
    }
    return false;
  }

  public void finalization() {
    for (DataWord contract : result.getDeleteAccounts()) {
      deposit.deleteContract(convertTogscAddress((contract.getLast20Bytes())));
    }

    if (config.vmTrace() && program != null && result != null) {
      String trace = program.getTrace()
          .result(result.getHReturn())
          .error(result.getException())
          .toString();

      if (config.vmTraceCompressed()) {
        trace = zipAndEncode(trace);
      }

      String txHash = Hex.toHexString(new InternalTransaction(trx).getHash());
      saveProgramTraceFile(config, txHash, trace);
    }

  }

  public ProgramResult getResult() {
    return result;
  }

  public String getRuntimeError() {
    return runtimeError;
  }
}
