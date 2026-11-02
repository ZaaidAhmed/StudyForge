package ca.yorku.eecs3311.coursepilot.application.ai;

/** Safe, non-sensitive categories used for provider and structured-output diagnostics. */
public enum AiFailureCategory {
    AI_PROVIDER_ERROR,
    AI_TIMEOUT,
    AI_BLOCKED_RESPONSE,
    AI_EMPTY_RESPONSE,
    AI_INVALID_JSON,
    AI_INVALID_SCHEMA,
    AI_EMPTY_FLASHCARD_BATCH
}
