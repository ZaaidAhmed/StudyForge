package ca.yorku.eecs3311.coursepilot.application.error;

/** Stable categories used by web and CLI adapters when reporting expected failures. */
public enum ApplicationErrorCode {
    VALIDATION_ERROR,
    COURSE_NOT_FOUND,
    DOCUMENT_NOT_FOUND,
    NO_RELEVANT_MATERIAL,
    AI_UNAVAILABLE,
    AI_TIMEOUT,
    INVALID_AI_RESPONSE,
    UPLOAD_FAILED,
    UNSUPPORTED_OPERATION
}
