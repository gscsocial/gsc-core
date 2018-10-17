package org.gsc.core.exception;

public class GSCException extends Exception {

  public GSCException() {
    super();
  }

  public GSCException(String message) {
    super(message);
  }

  public GSCException(String message, Throwable cause) {
    super(message, cause);
  }

}
