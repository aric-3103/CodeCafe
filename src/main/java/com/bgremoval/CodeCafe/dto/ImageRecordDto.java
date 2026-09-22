package com.bgremoval.CodeCafe.dto;

import com.bgremoval.CodeCafe.domain.ProcessingStatus;

import java.util.UUID;

/**
 * DTO representing an image record returned to the client.
 * The errorMessage is null unless the record has FAILED status.
 */
public record ImageRecordDto(
        UUID id,
        String originalFilename,
        ProcessingStatus status,
        String errorMessage   // null unless FAILED
) {}
