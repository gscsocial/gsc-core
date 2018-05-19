package org.gsc.core.operator;

import com.google.protobuf.Any;
import org.gsc.db.Manager;
import org.springframework.beans.factory.annotation.Autowired;


public abstract class AbstractOperator implements Operator {

  protected Any contract;

  @Autowired
  protected Manager dbManager;

  AbstractOperator(Any contract) {
    this.contract = contract;
  }
}
