package com.bgremoval.CodeCafe.exception;

/**
 * Base exception for AI model-related errors in the background removal tool.
 * Subtypes represent more specific failure modes (e.g., model load failure).
 */
public class ModelException extends RuntimeException {

    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
