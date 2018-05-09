package org.gsc.common.exception;

public class DupTransactionException extends GscException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }
}
