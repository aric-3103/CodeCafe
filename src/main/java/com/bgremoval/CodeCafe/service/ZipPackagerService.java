package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.exception.ZipPackagingException;

import java.io.OutputStream;

/**
 * Bundles all COMPLETED images in a session into a ZIP archive and writes it
 * to the provided {@link OutputStream}.
 *
 * <p>Requirements: 7.2, 7.3, 7.4, 7.5
 */
public interface ZipPackagerService {

    /**
     * Writes all COMPLETED images from the given session to {@code out} as a
     * ZIP archive named {@code processed-images.zip}.
     *
     * <p>Entry names follow the download-filename convention (see
     * {@link FilenameUtil#toDownloadFilename}). If two source files share the
     * same base name the second and subsequent entries receive numeric suffixes:
     * {@code photo-bg-removed-2.png}, {@code photo-bg-removed-3.png}, etc.
     *
     * @param sessionId session whose COMPLETED records should be packaged
     * @param out       target output stream (caller manages its lifecycle)
     * @throws ZipPackagingException if the session has no COMPLETED records or
     *                               an I/O error occurs while streaming
     */
    void packageSession(String sessionId, OutputStream out) throws ZipPackagingException;
}
