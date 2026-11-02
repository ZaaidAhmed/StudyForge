package ca.yorku.eecs3311.coursepilot.infrastructure.ai;

import ca.yorku.eecs3311.coursepilot.application.ai.AiTimeoutException;
import ca.yorku.eecs3311.coursepilot.application.ai.AiFailureCategory;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailability;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailabilityStatus;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClientException;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmResponse;
import ca.yorku.eecs3311.coursepilot.config.AiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Gemini REST implementation behind the provider-neutral LlmClient boundary. */
public class GeminiLlmAdapter implements LlmClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiLlmAdapter(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout(properties.getConnectTimeout()))
                .build();
    }

    @Override
    public LlmAvailability checkAvailability() {
        if (!properties.isEnabled()) return availability(LlmAvailabilityStatus.DISABLED, "AI integration is disabled.");
        if (!isConfigured()) return availability(LlmAvailabilityStatus.MISCONFIGURED, "Gemini provider configuration is incomplete.");
        try {
            HttpResponse<String> response = httpClient.send(availabilityRequest(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode metadata = objectMapper.readTree(response.body());
                JsonNode methods = metadata.get("supportedGenerationMethods");
                if (methods != null && methods.isArray()
                        && !java.util.stream.StreamSupport.stream(methods.spliterator(), false)
                        .anyMatch(node -> "generateContent".equals(node.asText()))) {
                    return availability(LlmAvailabilityStatus.MISCONFIGURED,
                            "The configured Gemini model does not support text generation.");
                }
                return availability(LlmAvailabilityStatus.AVAILABLE, "Gemini is reachable.");
            }
            return availability(statusFor(response.statusCode()), messageFor(response.statusCode()));
        } catch (HttpTimeoutException exception) {
            return availability(LlmAvailabilityStatus.UNAVAILABLE, "Gemini availability check timed out.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return availability(LlmAvailabilityStatus.UNAVAILABLE, "Gemini availability check was interrupted.");
        } catch (IOException | RuntimeException exception) {
            return availability(LlmAvailabilityStatus.UNAVAILABLE, "Gemini is unavailable.");
        }
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (!properties.isEnabled()) throw new LlmClientException("AI integration is disabled.");
        if (!isConfigured()) throw new LlmClientException("Gemini provider configuration is incomplete.");

        long started = System.nanoTime();
        try {
            Map<String, Object> payload = payload(request);
            String json = objectMapper.writeValueAsString(payload);
            HttpResponse<String> response = httpClient.send(
                    request(json), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerException(response.statusCode());
            }
            GeminiResponse generated = objectMapper.readValue(response.body(), GeminiResponse.class);
            String content = extractText(generated);
            String model = generated.model() == null || generated.model().isBlank()
                    ? properties.getModel() : generated.model();
            return new LlmResponse(content, model,
                    Duration.ofNanos(System.nanoTime() - started).toMillis());
        } catch (LlmClientException exception) {
            throw exception;
        } catch (HttpTimeoutException exception) {
            throw new AiTimeoutException("AI request timed out. Try again or use material search.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("AI request was interrupted.", exception);
        } catch (IOException | RuntimeException exception) {
            throw new LlmClientException("Gemini is currently unavailable.", exception, AiFailureCategory.AI_PROVIDER_ERROR);
        }
    }

    private Map<String, Object> payload(LlmRequest request) {
        List<Map<String, String>> userParts = List.of(Map.of("text", request.userPrompt()));
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (!request.systemPrompt().isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.systemPrompt()))));
        }
        body.put("contents", List.of(Map.of("role", "user", "parts", userParts)));

        Map<String, Object> generationConfig = new java.util.LinkedHashMap<>();
        generationConfig.put("temperature", request.temperature());
        if (request.structuredOutputExpected()) {
            generationConfig.put("responseMimeType", "application/json");
        }
        body.put("generationConfig", generationConfig);
        return body;
    }

    private String extractText(GeminiResponse response) {
        if (response.candidates() == null || response.candidates().isEmpty()) {
            boolean blocked = response.promptFeedback() != null && response.promptFeedback().blockReason() != null;
            String reason = blocked ? "Gemini blocked the request: " + response.promptFeedback().blockReason() + "." : "Gemini returned no answer.";
            throw new LlmClientException(reason, blocked ? AiFailureCategory.AI_BLOCKED_RESPONSE : AiFailureCategory.AI_EMPTY_RESPONSE);
        }
        List<String> parts = new ArrayList<>();
        for (Candidate candidate : response.candidates()) {
            if (candidate.content() == null || candidate.content().parts() == null) continue;
            for (Part part : candidate.content().parts()) {
                if (part.text() != null && !part.text().isBlank()) parts.add(part.text());
            }
        }
        if (parts.isEmpty()) throw new LlmClientException("Gemini returned an empty response.", AiFailureCategory.AI_EMPTY_RESPONSE);
        return String.join("\n", parts);
    }

    private HttpRequest request(String body) {
        return HttpRequest.newBuilder(endpoint())
                .timeout(timeout(properties.getRequestTimeout()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest availabilityRequest() {
        return HttpRequest.newBuilder(modelEndpoint())
                .timeout(timeout(properties.getRequestTimeout()))
                .header("x-goog-api-key", properties.getApiKey())
                .GET()
                .build();
    }

    private URI endpoint() {
        return URI.create(modelEndpoint().toString() + ":generateContent");
    }

    private URI modelEndpoint() {
        String base = properties.getProviderBaseUrl().replaceAll("/+$", "");
        String model = URLEncoder.encode(properties.getModel(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return URI.create(base + "/v1beta/models/" + model);
    }

    private LlmClientException providerException(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return new LlmClientException("Gemini rejected the API key or access to this model.", AiFailureCategory.AI_PROVIDER_ERROR);
        }
        if (statusCode == 429 || statusCode >= 500) {
            return new LlmClientException("Gemini is busy or rate-limited. Try again shortly.", AiFailureCategory.AI_PROVIDER_ERROR);
        }
        return new LlmClientException("Gemini returned HTTP " + statusCode + ".", AiFailureCategory.AI_PROVIDER_ERROR);
    }

    private LlmAvailabilityStatus statusFor(int statusCode) {
        if (statusCode == 400 || statusCode == 401 || statusCode == 403 || statusCode == 404) return LlmAvailabilityStatus.MISCONFIGURED;
        return LlmAvailabilityStatus.UNAVAILABLE;
    }

    private String messageFor(int statusCode) {
        if (statusCode == 401 || statusCode == 403) return "Gemini rejected the API key or model access.";
        if (statusCode == 404) return "The configured Gemini model does not support this API endpoint or is unavailable.";
        if (statusCode == 429) return "Gemini rate limit reached.";
        return "Gemini returned HTTP " + statusCode + ".";
    }

    private boolean isConfigured() {
        return "gemini".equalsIgnoreCase(properties.getProvider())
                && properties.getProviderBaseUrl() != null && !properties.getProviderBaseUrl().isBlank()
                && properties.getModel() != null && !properties.getModel().isBlank()
                && properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private LlmAvailability availability(LlmAvailabilityStatus status, String message) {
        return new LlmAvailability(status, properties.getProvider(), properties.getModel(), message);
    }

    private static Duration timeout(Duration duration) {
        return duration == null || duration.isNegative() || duration.isZero()
                ? Duration.ofSeconds(2) : duration;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(String model, List<Candidate> candidates, PromptFeedback promptFeedback) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidate(Content content) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Content(List<Part> parts) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Part(String text) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PromptFeedback(String blockReason) { }
}
