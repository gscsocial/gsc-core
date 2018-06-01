package org.gsc.common.exception;

public class TooBigTransactionException extends GscException {

    public TooBigTransactionException() { super(); }

    public TooBigTransactionException(String message) { super(message); }
}
