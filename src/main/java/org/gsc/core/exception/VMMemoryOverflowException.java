package org.gsc.core.exception;

public class VMMemoryOverflowException extends GSCException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
