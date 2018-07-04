package org.gsc.core.exception;

public class GscRuntimeException extends RuntimeException {

  public GscRuntimeException() {
    super();
  }

  public GscRuntimeException(String message) {
    super(message);
  }

  public GscRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public GscRuntimeException(Throwable cause) {
    super(cause);
  }

  protected GscRuntimeException(String message, Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
