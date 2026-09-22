package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates an uploaded {@link MultipartFile} before processing.
 *
 * <p>Checks performed (in order):
 * <ol>
 *   <li>File size must not exceed 25 MB.</li>
 *   <li>File extension must be one of {@code jpg}, {@code jpeg}, {@code png}, {@code webp}
 *       (case-insensitive).</li>
 *   <li>Declared MIME type must match the expected MIME type for the extension.</li>
 * </ol>
 */
public interface FileValidatorService {

    /**
     * Validates the uploaded file's extension, MIME type, size, and
     * extension/MIME consistency.
     *
     * @param file the multipart file to validate
     * @throws ValidationException with a human-readable message on any failure
     */
    void validate(MultipartFile file) throws ValidationException;
}
