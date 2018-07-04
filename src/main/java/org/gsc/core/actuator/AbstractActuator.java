package org.gsc.core.actuator;

import com.google.protobuf.Any;
import org.gsc.core.db.Manager;

public abstract class AbstractActuator implements Actuator {

  protected Any contract;
  protected Manager dbManager;

  AbstractActuator(Any contract, Manager dbManager) {
    this.contract = contract;
    this.dbManager = dbManager;
  }
}
