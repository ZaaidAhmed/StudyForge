package ca.yorku.eecs3311.coursepilot.application.ai;

public record LlmRequest(String systemPrompt, String userPrompt, Double temperature,
                         boolean structuredOutputExpected, String format) {
    public LlmRequest {
        systemPrompt = systemPrompt == null ? "" : systemPrompt;
        userPrompt = userPrompt == null ? "" : userPrompt;
        temperature = temperature == null ? 0.2d : temperature;
        format = format == null || format.isBlank() ? null : format;
    }
}
