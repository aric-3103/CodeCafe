package com.bgremoval.CodeCafe.exception;

/**
 * Thrown when an uploaded file fails validation (unsupported format, size
 * exceeded, or extension/MIME mismatch). Maps to HTTP 400 in the global
 * exception handler.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
