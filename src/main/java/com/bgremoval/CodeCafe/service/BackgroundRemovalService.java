package com.bgremoval.CodeCafe.service;

import java.util.UUID;

/**
 * Asynchronously removes the background from an already-stored image record.
 *
 * <p>Implementations must be safe to call from a Spring {@code @Async} thread pool.
 * The method transitions the record through PROCESSING → COMPLETED or FAILED and
 * must never propagate an exception out of the call — all errors are captured in
 * the record's {@code errorMessage}.
 *
 * <p>Requirements: 1.2, 1.3, 1.4, 8.1, 8.2, 8.3
 */
public interface BackgroundRemovalService {

    /**
     * Removes the background from the image identified by {@code recordId}.
     *
     * <p>Status transitions:
     * <ul>
     *   <li>Sets status to {@code PROCESSING} immediately on entry.</li>
     *   <li>Sets status to {@code COMPLETED} (with {@code outputBytes} and
     *       {@code completedAt}) on success.</li>
     *   <li>Sets status to {@code FAILED} (with a non-empty {@code errorMessage}
     *       and {@code completedAt}) on any exception — the exception is never
     *       re-thrown.</li>
     * </ul>
     *
     * @param recordId the UUID of the {@code ImageRecord} to process
     */
    void process(UUID recordId);
}
