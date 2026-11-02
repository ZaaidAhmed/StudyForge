package ca.yorku.eecs3311.coursepilot.application.ai;

public class LlmClientException extends RuntimeException {
    private final AiFailureCategory category;

    public LlmClientException(String message) { this(message, null, AiFailureCategory.AI_PROVIDER_ERROR); }
    public LlmClientException(String message, Throwable cause) { this(message, cause, AiFailureCategory.AI_PROVIDER_ERROR); }
    public LlmClientException(String message, AiFailureCategory category) { this(message, null, category); }
    public LlmClientException(String message, Throwable cause, AiFailureCategory category) {
        super(message, cause);
        this.category = category == null ? AiFailureCategory.AI_PROVIDER_ERROR : category;
    }
    public AiFailureCategory category() { return category; }
}
