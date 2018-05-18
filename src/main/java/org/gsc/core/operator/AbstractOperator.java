package org.gsc.core.operator;

import com.google.protobuf.Any;
import org.gsc.db.Manager;


public abstract class AbstractOperator implements Operator {

  protected Any contract;
  protected Manager dbManager;

  AbstractOperator(Any contract, Manager dbManager) {
    this.contract = contract;
    this.dbManager = dbManager;
  }
}
