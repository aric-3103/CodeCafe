package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.domain.ImageRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe in-memory store for {@link ImageRecord} objects, keyed by session ID.
 *
 * <p>Implementations must be safe for concurrent access from multiple threads
 * (e.g., async background-removal workers writing alongside polling REST requests).
 */
public interface ProcessingSessionStore {

    /**
     * Adds a new {@link ImageRecord} to the specified session's list.
     * Creates the session list if one does not already exist.
     *
     * @param sessionId the session identifier to add the record to
     * @param record    the record to add (must not be {@code null})
     */
    void addRecord(String sessionId, ImageRecord record);

    /**
     * Finds a single {@link ImageRecord} by its unique identifier, searching
     * across all sessions.
     *
     * @param recordId the UUID of the record to locate
     * @return an {@link Optional} containing the matching record, or
     *         {@link Optional#empty()} if no record with that ID exists
     */
    Optional<ImageRecord> findById(UUID recordId);

    /**
     * Returns all {@link ImageRecord} objects belonging to the given session.
     *
     * @param sessionId the session identifier
     * @return an unmodifiable snapshot of the session's record list, or an
     *         empty list if the session does not exist
     */
    List<ImageRecord> findBySession(String sessionId);

    /**
     * Replaces an existing {@link ImageRecord} in the store with the provided
     * updated record, matched by {@link ImageRecord#getId()}.
     *
     * <p>If no record with the same ID is found the operation is a no-op.
     *
     * @param record the updated record (must not be {@code null}; must have
     *               the same {@code id} as the record it replaces)
     */
    void updateRecord(ImageRecord record);
}
