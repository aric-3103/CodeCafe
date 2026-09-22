package com.bgremoval.CodeCafe.exception;

/**
 * Thrown when building a ZIP archive fails — either because no COMPLETED
 * records exist in the session or because an I/O error occurs mid-stream.
 * Maps to HTTP 500 in the global exception handler.
 */
public class ZipPackagingException extends RuntimeException {

    public ZipPackagingException(String message) {
        super(message);
    }

    public ZipPackagingException(String message, Throwable cause) {
        super(message, cause);
    }
}
