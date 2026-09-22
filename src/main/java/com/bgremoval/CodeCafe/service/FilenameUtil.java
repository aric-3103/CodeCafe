package com.bgremoval.CodeCafe.service;

/**
 * Utility class for deriving download filenames from original upload filenames.
 * Non-instantiable; all methods are static.
 */
public final class FilenameUtil {

    private FilenameUtil() {
        throw new UnsupportedOperationException("FilenameUtil is a utility class");
    }

    /**
     * Derives the download filename from the original filename.
     * Strips from the last '.' onward; appends '-bg-removed.png'.
     * If no '.' is present, uses the full original filename as the base.
     *
     * <p>Examples:
     * <pre>
     *   "photo.jpg"       → "photo-bg-removed.png"
     *   "image"           → "image-bg-removed.png"
     *   "archive.tar.gz"  → "archive.tar-bg-removed.png"
     *   "my.image.png"    → "my.image-bg-removed.png"
     * </pre>
     *
     * @param originalFilename the original filename as uploaded; must not be {@code null}
     * @return the derived download filename ending with {@code -bg-removed.png}
     * @throws IllegalArgumentException if {@code originalFilename} is {@code null}
     */
    public static String toDownloadFilename(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("originalFilename must not be null");
        }

        int lastDot = originalFilename.lastIndexOf('.');
        String base = (lastDot >= 0) ? originalFilename.substring(0, lastDot) : originalFilename;

        return base + "-bg-removed.png";
    }
}
