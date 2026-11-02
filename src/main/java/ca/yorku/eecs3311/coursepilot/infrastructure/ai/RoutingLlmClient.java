package ca.yorku.eecs3311.coursepilot.infrastructure.ai;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmResponse;
import ca.yorku.eecs3311.coursepilot.config.AiProperties;

/** Delegates each operation to the provider currently selected in AiProperties. */
public class RoutingLlmClient implements LlmClient {
    private final AiProperties properties;
    private final LlmClient ollama;
    private final LlmClient gemini;
    private final LlmClient unsupported;

    public RoutingLlmClient(AiProperties properties, LlmClient ollama, LlmClient gemini,
                            LlmClient unsupported) {
        this.properties = properties;
        this.ollama = ollama;
        this.gemini = gemini;
        this.unsupported = unsupported;
    }

    @Override
    public LlmResponse generate(LlmRequest request) { return delegate().generate(request); }

    @Override
    public LlmAvailability checkAvailability() { return delegate().checkAvailability(); }

    private LlmClient delegate() {
        if ("ollama".equalsIgnoreCase(properties.getProvider())) return ollama;
        if ("gemini".equalsIgnoreCase(properties.getProvider())) return gemini;
        return unsupported;
    }
}
