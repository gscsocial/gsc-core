package org.gsc.core.exception;

public class TooBigTransactionResultException extends GSCException {

    public TooBigTransactionResultException() { super("too big transaction result"); }

    public TooBigTransactionResultException(String message) { super(message); }
}
