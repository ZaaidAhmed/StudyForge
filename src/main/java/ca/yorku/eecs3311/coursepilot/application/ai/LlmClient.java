package ca.yorku.eecs3311.coursepilot.application.ai;

public interface LlmClient {
    LlmResponse generate(LlmRequest request);

    LlmAvailability checkAvailability();
}
