package ca.yorku.eecs3311.coursepilot.infrastructure.ai;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailabilityStatus;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClientException;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmResponse;

/** Keeps the application usable when an unsupported provider is configured. */
public class UnavailableLlmClient implements LlmClient {
    private final String provider;

    public UnavailableLlmClient(String provider) { this.provider = provider; }

    @Override
    public LlmResponse generate(LlmRequest request) {
        throw new LlmClientException("Unsupported AI provider: " + provider + ".");
    }

    @Override
    public LlmAvailability checkAvailability() {
        return new LlmAvailability(LlmAvailabilityStatus.MISCONFIGURED, provider, "", "Unsupported AI provider.");
    }
}
