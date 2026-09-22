package com.bgremoval.CodeCafe.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Central domain object representing one uploaded image and its processing state.
 * Stored in memory (never persisted to a database).
 */
public class ImageRecord {

    /** Stable, unique identifier for this record. */
    private UUID id;

    /** Groups multiple records belonging to the same browser session. */
    private String sessionId;

    /** Original filename as supplied by the uploader (e.g. "photo.jpg"). */
    private String originalFilename;

    /** Validated MIME type of the uploaded file (e.g. "image/jpeg"). */
    private String contentType;

    /** Raw bytes of the uploaded file. */
    private byte[] inputBytes;

    /** Result PNG bytes after background removal; {@code null} until status is COMPLETED. */
    private byte[] outputBytes;

    /** Current processing lifecycle status. */
    private ProcessingStatus status;

    /** Human-readable error description; non-null only when status is FAILED. */
    private String errorMessage;

    /** Timestamp when the file was uploaded and the record was created. */
    private Instant uploadedAt;

    /** Timestamp when processing reached a terminal status (COMPLETED or FAILED); {@code null} until then. */
    private Instant completedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** No-args constructor required for frameworks and serialisation. */
    public ImageRecord() {
    }

    /**
     * All-args constructor for creating a fully initialised record.
     *
     * @param id               stable unique identifier
     * @param sessionId        browser session grouping key
     * @param originalFilename filename as uploaded
     * @param contentType      validated MIME type
     * @param inputBytes       raw uploaded bytes
     * @param outputBytes      result PNG bytes (may be {@code null})
     * @param status           current {@link ProcessingStatus}
     * @param errorMessage     error detail when status is FAILED (otherwise {@code null})
     * @param uploadedAt       time the record was created
     * @param completedAt      time processing finished (may be {@code null})
     */
    public ImageRecord(UUID id,
                       String sessionId,
                       String originalFilename,
                       String contentType,
                       byte[] inputBytes,
                       byte[] outputBytes,
                       ProcessingStatus status,
                       String errorMessage,
                       Instant uploadedAt,
                       Instant completedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.inputBytes = inputBytes;
        this.outputBytes = outputBytes;
        this.status = status;
        this.errorMessage = errorMessage;
        this.uploadedAt = uploadedAt;
        this.completedAt = completedAt;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getInputBytes() {
        return inputBytes;
    }

    public void setInputBytes(byte[] inputBytes) {
        this.inputBytes = inputBytes;
    }

    public byte[] getOutputBytes() {
        return outputBytes;
    }

    public void setOutputBytes(byte[] outputBytes) {
        this.outputBytes = outputBytes;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ImageRecord{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", status=" + status +
                ", errorMessage='" + errorMessage + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
