package org.gsc.core.exception;

public class GscException extends Exception {

  public GscException() {
    super();
  }

  public GscException(String message) {
    super(message);
  }

  public GscException(String message, Throwable cause) {
    super(message, cause);
  }

}
