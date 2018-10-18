package org.gsc.core.operator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Transaction.Contract;

@Slf4j
public class OperatorFactory {

  public static final OperatorFactory INSTANCE = new OperatorFactory();

  private OperatorFactory() {
  }

  public static OperatorFactory getInstance() {
    return INSTANCE;
  }

  /**
   * create actuator.
   */
  public static List<Operator> createActuator(TransactionWrapper transactionWrapper,
                                              Manager manager) {
    List<Operator> operatorList = Lists.newArrayList();
    if (null == transactionWrapper || null == transactionWrapper.getInstance()) {
      logger.info("transactionCapsule or Transaction is null");
      return operatorList;
    }

    Preconditions.checkNotNull(manager, "manager is null");
    Protocol.Transaction.raw rawData = transactionWrapper.getInstance().getRawData();
    rawData.getContractList()
            .forEach(contract -> operatorList.add(getActuatorByContract(contract, manager)));
    return operatorList;
  }

  private static Operator getActuatorByContract(Contract contract, Manager manager) {
    switch (contract.getType()) {
      case AccountUpdateContract:
        return new UpdateAccountOperator(contract.getParameter(), manager);
      case TransferContract:
        return new TransferOperator(contract.getParameter(), manager);
      case TransferAssetContract:
        return new TransferAssetOperator(contract.getParameter(), manager);
      case VoteAssetContract:
        break;
      case VoteWitnessContract:
        return new VoteWitnessOperator(contract.getParameter(), manager);
      case WitnessCreateContract:
        return new WitnessCreateOperator(contract.getParameter(), manager);
      case AccountCreateContract:
        return new CreateAccountOperator(contract.getParameter(), manager);
      case AssetIssueContract:
        return new AssetIssueOperator(contract.getParameter(), manager);
      case UnfreezeAssetContract:
        return new UnfreezeAssetOperator(contract.getParameter(), manager);
      case WitnessUpdateContract:
        return new WitnessUpdateOperator(contract.getParameter(), manager);
      case ParticipateAssetIssueContract:
        return new ParticipateAssetIssueOperator(contract.getParameter(), manager);
      case FreezeBalanceContract:
        return new FreezeBalanceOperator(contract.getParameter(), manager);
      case UnfreezeBalanceContract:
        return new UnfreezeBalanceOperator(contract.getParameter(), manager);
      case WithdrawBalanceContract:
        return new WithdrawBalanceOperator(contract.getParameter(), manager);
      case UpdateAssetContract:
        return new UpdateAssetOperator(contract.getParameter(), manager);
      case ProposalCreateContract:
        return new ProposalCreateOperator(contract.getParameter(), manager);
      case ProposalApproveContract:
        return new ProposalApproveOperator(contract.getParameter(), manager);
      case ProposalDeleteContract:
        return new ProposalDeleteOperator(contract.getParameter(), manager);
      case SetAccountIdContract:
        return new SetAccountIdOperator(contract.getParameter(), manager);
//      case BuyStorageContract:
//        return new BuyStorageOperator(contract.getParameter(), manager);
//      case BuyStorageBytesContract:
//        return new BuyStorageBytesOperator(contract.getParameter(), manager);
//      case SellStorageContract:
//        return new SellStorageOperator(contract.getParameter(), manager);
      case UpdateSettingContract:
        return new UpdateSettingContractOperator(contract.getParameter(), manager);
      case ExchangeCreateContract:
        return new ExchangeCreateOperator(contract.getParameter(), manager);
      case ExchangeInjectContract:
        return new ExchangeInjectOperator(contract.getParameter(), manager);
      case ExchangeWithdrawContract:
        return new ExchangeWithdrawOperator(contract.getParameter(), manager);
      case ExchangeTransactionContract:
        return new ExchangeTransactionOperator(contract.getParameter(), manager);
      default:
        break;

    }
    return null;
  }

}
