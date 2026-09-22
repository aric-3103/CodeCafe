package com.bgremoval.CodeCafe.dto;

import java.util.List;

/**
 * Response returned after a batch image upload.
 * rejectedFiles contains filenames that failed validation and were not queued for processing.
 */
public record UploadResponse(
        String sessionId,
        List<ImageRecordDto> records,
        List<String> rejectedFiles   // filenames that failed validation
) {}
