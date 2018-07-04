package org.gsc.core.operator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OperatorFactory {

  @Autowired
  public OperatorFactory(ApplicationContext ctx) {}

  /**
   * create actuator.
   */
  public static Operator createActuator(TransactionWrapper transactionWrapper,
      Manager manager) {
    List<Operator> actuatorList = Lists.newArrayList();
    if (null == transactionWrapper || null == transactionWrapper.getInstance()) {
      logger.info("transactionWrapper or Transaction is null");
      return null;
    }

    Preconditions.checkNotNull(manager, "manager is null");
    Protocol.Transaction.raw rawData = transactionWrapper.getInstance().getRawData();
    return getActuatorByContract(rawData.getContract(), manager);
  }

  //TODO implemetion op
  private static Operator getActuatorByContract(Contract contract, Manager manager) {
    return null;
  }

}
