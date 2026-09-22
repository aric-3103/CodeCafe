package com.bgremoval.CodeCafe.exception;

/**
 * Thrown when an {@code ImageRecord} cannot be found by the given ID or when
 * a session has no records matching the requested criteria. Maps to HTTP 404
 * in the global exception handler.
 */
public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(String message) {
        super(message);
    }

    public RecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
