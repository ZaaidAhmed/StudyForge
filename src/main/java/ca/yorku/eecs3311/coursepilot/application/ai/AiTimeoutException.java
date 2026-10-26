package ca.yorku.eecs3311.coursepilot.application.ai;

/** Signals that the provider did not answer within the configured request timeout. */
public class AiTimeoutException extends LlmClientException {
    public AiTimeoutException(String message, Throwable cause) { super(message, cause); }
}
