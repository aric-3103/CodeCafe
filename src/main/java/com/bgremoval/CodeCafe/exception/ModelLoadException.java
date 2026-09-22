package com.bgremoval.CodeCafe.exception;

/**
 * Thrown when the ONNX model fails to load at application startup or cannot
 * be initialised for inference. Extends {@link ModelException} so it can be
 * caught either specifically or as a general model error. Maps to HTTP 503 in
 * the global exception handler.
 */
public class ModelLoadException extends ModelException {

    public ModelLoadException(String message) {
        super(message);
    }

    public ModelLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
