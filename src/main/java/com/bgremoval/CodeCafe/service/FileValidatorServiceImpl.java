package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.exception.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link FileValidatorService}.
 *
 * <p>Validation rules:
 * <ol>
 *   <li>Rejects files larger than {@value #MAX_FILE_SIZE_BYTES} bytes (25 MB).</li>
 *   <li>Rejects files whose extension is not in the supported set
 *       ({@code jpg}, {@code jpeg}, {@code png}, {@code webp}), case-insensitive.</li>
 *   <li>Rejects files whose declared {@code Content-Type} does not match the
 *       expected MIME type for the detected extension.</li>
 * </ol>
 */
@Component
public class FileValidatorServiceImpl implements FileValidatorService {

    /** 25 MB in bytes. */
    static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    /** Supported extensions (lower-case). */
    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    /**
     * Maps each supported extension to its expected MIME type.
     * Both {@code jpg} and {@code jpeg} map to {@code image/jpeg}.
     */
    private static final Map<String, String> EXTENSION_TO_MIME = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp"
    );

    @Override
    public void validate(MultipartFile file) throws ValidationException {

        // 1. Size check
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException(
                    String.format("File '%s' exceeds the maximum allowed size of 25 MB (actual: %.2f MB).",
                            file.getOriginalFilename(),
                            file.getSize() / (1024.0 * 1024.0)));
        }

        // 2. Extension check
        String filename = file.getOriginalFilename();
        String extension = extractExtension(filename);

        if (extension == null || !SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new ValidationException(
                    String.format("File '%s' has an unsupported extension '%s'. "
                                  + "Accepted formats: jpg, jpeg, png, webp.",
                            filename,
                            extension == null ? "(none)" : extension));
        }

        // 3. MIME type consistency check
        String declaredContentType = file.getContentType();
        String expectedMime = EXTENSION_TO_MIME.get(extension);

        if (declaredContentType == null || !declaredContentType.equalsIgnoreCase(expectedMime)) {
            throw new ValidationException(
                    String.format("File '%s' has a Content-Type of '%s' which does not match "
                                  + "the expected MIME type '%s' for the '.%s' extension.",
                            filename,
                            declaredContentType,
                            expectedMime,
                            extension));
        }
    }

    /**
     * Extracts the lower-cased file extension from the given filename.
     *
     * @param filename the original filename (may be {@code null})
     * @return the lower-cased extension after the last {@code '.'}, or
     *         {@code null} if the filename is {@code null}, empty, or has no extension
     */
    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
