package ca.yorku.eecs3311.coursepilot.infrastructure.ai;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailabilityStatus;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClientException;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmResponse;
import ca.yorku.eecs3311.coursepilot.config.AiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class OllamaLlmAdapter implements LlmClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaLlmAdapter(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout(properties.getConnectTimeout())).build();
    }

    @Override
    public LlmAvailability checkAvailability() {
        if (!properties.isEnabled()) return availability(LlmAvailabilityStatus.DISABLED, "AI integration is disabled.");
        if (!isConfigured()) return availability(LlmAvailabilityStatus.MISCONFIGURED, "AI provider configuration is incomplete.");
        try {
            HttpResponse<String> response = httpClient.send(request("/api/tags", "GET", null), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return availability(LlmAvailabilityStatus.AVAILABLE, "Ollama is reachable.");
            }
            return availability(LlmAvailabilityStatus.UNAVAILABLE, "Ollama returned HTTP " + response.statusCode() + ".");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return availability(LlmAvailabilityStatus.UNAVAILABLE, "Ollama availability check was interrupted.");
        } catch (IOException | RuntimeException exception) {
            return availability(LlmAvailabilityStatus.UNAVAILABLE, "Ollama is unavailable.");
        }
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (!properties.isEnabled()) throw new LlmClientException("AI integration is disabled.");
        if (!isConfigured()) throw new LlmClientException("AI provider configuration is incomplete.");
        long started = System.nanoTime();
        Map<String, Object> options = Map.of("temperature", request.temperature());
        OllamaGenerateRequest payload = new OllamaGenerateRequest(properties.getModel(), request.userPrompt(),
                request.systemPrompt(), false, options, request.structuredOutputExpected() ?
                (request.format() == null ? "json" : request.format()) : null);
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpResponse<String> response = httpClient.send(request("/api/generate", "POST", json),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmClientException("AI service returned HTTP " + response.statusCode() + ".");
            }
            OllamaGenerateResponse generated = objectMapper.readValue(response.body(), OllamaGenerateResponse.class);
            if (generated.response() == null || generated.response().isBlank()) {
                throw new LlmClientException("AI service returned an empty response.");
            }
            String model = generated.model() == null || generated.model().isBlank() ? properties.getModel() : generated.model();
            return new LlmResponse(generated.response(), model, Duration.ofNanos(System.nanoTime() - started).toMillis());
        } catch (LlmClientException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("AI request was interrupted.", exception);
        } catch (IOException | RuntimeException exception) {
            throw new LlmClientException("AI service is currently unavailable.", exception);
        }
    }

    private HttpRequest request(String path, String method, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).timeout(timeout(properties.getRequestTimeout()));
        if ("POST".equals(method)) builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        else builder.GET();
        return builder.build();
    }

    private URI uri(String path) {
        try { return new URI(properties.getBaseUrl().replaceAll("/+$", "") + path); }
        catch (URISyntaxException exception) { throw new LlmClientException("AI provider base URL is invalid.", exception); }
    }

    private boolean isConfigured() {
        return "ollama".equalsIgnoreCase(properties.getProvider()) && properties.getBaseUrl() != null
                && !properties.getBaseUrl().isBlank() && properties.getModel() != null && !properties.getModel().isBlank();
    }

    private LlmAvailability availability(LlmAvailabilityStatus status, String message) {
        return new LlmAvailability(status, properties.getProvider(), properties.getModel(), message);
    }

    private static Duration timeout(Duration duration) { return duration == null || duration.isNegative() || duration.isZero() ? Duration.ofSeconds(2) : duration; }

    private record OllamaGenerateRequest(String model, String prompt, String system, boolean stream,
                                         Map<String, Object> options, String format) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaGenerateResponse(String response, String model) { }
}
