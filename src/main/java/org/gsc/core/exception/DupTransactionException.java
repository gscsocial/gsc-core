package org.gsc.core.exception;

public class DupTransactionException extends GSCException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }
}
