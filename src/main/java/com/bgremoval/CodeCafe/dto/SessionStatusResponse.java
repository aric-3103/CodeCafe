package com.bgremoval.CodeCafe.dto;

import java.util.List;

/**
 * Response for session-level status polling.
 * Satisfies the count invariant: completed + failed + skipped == total (Requirement 9.2).
 *
 * allTerminal is true when every record has reached a terminal status
 * (i.e., no record is IDLE or PROCESSING), indicating the browser can stop polling.
 */
public record SessionStatusResponse(
        String sessionId,
        List<ImageRecordDto> records,
        int total,
        int completed,
        int failed,
        int skipped,        // validator-rejected files counted here
        boolean allTerminal // true when no record is IDLE or PROCESSING
) {}
