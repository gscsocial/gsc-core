/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.core.exception;

public class RevokingStoreIllegalStateException extends GSCRuntimeException {

    /**
     * Constructs an RevokingStoreIllegalStateException with no detail message. A detail message is a
     * String that describes this particular exception.
     */
    public RevokingStoreIllegalStateException() {
        super();
    }

    /**
     * Constructs an RevokingStoreIllegalStateException with the specified detail message.  A detail
     * message is a String that describes this particular exception.
     *
     * @param s the String that contains a detailed message
     */
    public RevokingStoreIllegalStateException(String s) {
        super(s);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with <code>cause</code> is <i>not</i> automatically
     * incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link
     *                Throwable#getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link Throwable#getCause()}
     *                method).  (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    public RevokingStoreIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause and a detail message of <tt>(cause==null ?
     * null : cause.toString())</tt> (which typically contains the class and detail message of
     * <tt>cause</tt>). This constructor is useful for exceptions that are little more than wrappers
     * for other throwables (for example, {@link java.security.PrivilegedActionException}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link Throwable#getCause()}
     *              method).  (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
     *              unknown.)
     */
    public RevokingStoreIllegalStateException(Throwable cause) {
        super("", cause);
    }

    public static final long serialVersionUID = -1848914673093119416L;
}
