package ca.yorku.eecs3311.coursepilot.config;

import java.util.List;

/** Explicitly supported model identifiers exposed by the model switcher. */
public final class AiModelCatalog {
    private AiModelCatalog() { }

    public static List<String> modelsFor(String provider) {
        if ("gemini".equalsIgnoreCase(provider)) {
            return List.of("gemini-3.5-flash-lite", "gemini-3.8-flash", "gemini-2.5-flash");
        }
        if ("ollama".equalsIgnoreCase(provider)) {
            return List.of("qwen3:4b", "qwen-claude-14b:latest");
        }
        return List.of();
    }

    public static boolean supports(String provider, String model) {
        return model != null && modelsFor(provider).contains(model);
    }

    public static List<String> providers() {
        return List.of("gemini", "ollama");
    }

    public static String defaultModelFor(String provider) {
        return "ollama".equalsIgnoreCase(provider) ? "qwen3:4b" : "gemini-3.5-flash-lite";
    }
}

