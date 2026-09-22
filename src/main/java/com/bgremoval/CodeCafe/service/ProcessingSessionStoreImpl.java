package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.domain.ImageRecord;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory, thread-safe implementation of {@link ProcessingSessionStore}.
 *
 * <p>Records are stored in a {@link ConcurrentHashMap} keyed by {@code sessionId}.
 * Each session maps to a {@link CopyOnWriteArrayList}, so concurrent reads are
 * lock-free and writes are protected by an internal copy-on-write mechanism.
 * This makes the store safe for simultaneous async processing threads (writers)
 * and REST polling threads (readers) without explicit synchronisation.
 */
@Component
public class ProcessingSessionStoreImpl implements ProcessingSessionStore {

    /**
     * Top-level map: sessionId → list of ImageRecords belonging to that session.
     * ConcurrentHashMap provides thread-safe map operations; per-session lists
     * are CopyOnWriteArrayLists for thread-safe list operations.
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ImageRecord>> store =
            new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@link ConcurrentHashMap#computeIfAbsent} to atomically create the
     * per-session list on first use, then appends the record to it.
     */
    @Override
    public void addRecord(String sessionId, ImageRecord record) {
        store.computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<>())
             .add(record);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Streams all session lists and returns the first record whose
     * {@code id} matches {@code recordId}.
     */
    @Override
    public Optional<ImageRecord> findById(UUID recordId) {
        return store.values()
                    .stream()
                    .flatMap(List::stream)
                    .filter(r -> recordId.equals(r.getId()))
                    .findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an unmodifiable view of the session list, or an empty list
     * when the session does not exist.
     */
    @Override
    public List<ImageRecord> findBySession(String sessionId) {
        CopyOnWriteArrayList<ImageRecord> sessionList = store.get(sessionId);
        if (sessionList == null) {
            return Collections.emptyList();
        }
        // Return an unmodifiable snapshot so callers cannot mutate the internal list.
        return Collections.unmodifiableList(sessionList);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Locates the existing record by ID and replaces it in the owning
     * session's list using an atomic {@code set} on the
     * {@link CopyOnWriteArrayList}.  If the record's session list is not found,
     * or the record ID is not present in that list, the method is a no-op.
     */
    @Override
    public void updateRecord(ImageRecord record) {
        String sessionId = record.getSessionId();
        CopyOnWriteArrayList<ImageRecord> sessionList = store.get(sessionId);
        if (sessionList == null) {
            return;
        }
        for (int i = 0; i < sessionList.size(); i++) {
            if (record.getId().equals(sessionList.get(i).getId())) {
                // CopyOnWriteArrayList.set() is thread-safe
                sessionList.set(i, record);
                return;
            }
        }
    }
}
