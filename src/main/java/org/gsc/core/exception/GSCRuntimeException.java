package org.gsc.core.exception;

public class GSCRuntimeException extends RuntimeException {

  public GSCRuntimeException() {
    super();
  }

  public GSCRuntimeException(String message) {
    super(message);
  }

  public GSCRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public GSCRuntimeException(Throwable cause) {
    super(cause);
  }

  protected GSCRuntimeException(String message, Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
