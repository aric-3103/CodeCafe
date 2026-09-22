package com.bgremoval.CodeCafe.domain;

/**
 * Represents the lifecycle status of an image processing record.
 */
public enum ProcessingStatus {
    /** Record created; processing not yet started. */
    IDLE,
    /** Background removal is currently in progress. */
    PROCESSING,
    /** Background removal finished successfully; outputBytes are available. */
    COMPLETED,
    /** Background removal failed; errorMessage is non-null. */
    FAILED
}
