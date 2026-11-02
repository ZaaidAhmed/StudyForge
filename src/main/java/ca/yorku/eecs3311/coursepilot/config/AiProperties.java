package ca.yorku.eecs3311.coursepilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "coursepilot.ai")
public class AiProperties {
    private boolean enabled = true;
    private String provider = "ollama";
    private String baseUrl = "http://localhost:11434";
    private String ollamaBaseUrl;
    private String geminiBaseUrl;
    private String model = "qwen2.5-coder:14b";
    private String apiKey = "";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration requestTimeout = Duration.ofSeconds(30);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
    public String getGeminiBaseUrl() { return geminiBaseUrl; }
    public void setGeminiBaseUrl(String geminiBaseUrl) { this.geminiBaseUrl = geminiBaseUrl; }
    public String getProviderBaseUrl() {
        String providerEndpoint = "gemini".equalsIgnoreCase(provider) ? geminiBaseUrl : ollamaBaseUrl;
        return providerEndpoint == null || providerEndpoint.isBlank() ? baseUrl : providerEndpoint;
    }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
}
