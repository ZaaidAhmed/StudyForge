package ca.yorku.eecs3311.coursepilot.infrastructure.ai;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailabilityStatus;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmClientException;
import ca.yorku.eecs3311.coursepilot.application.ai.LlmRequest;
import ca.yorku.eecs3311.coursepilot.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiLlmAdapterTest {
    @Test
    void translatesStructuredRequestsWithoutLeakingKey() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var requestBody = new StringBuilder();
        var requestKey = new StringBuilder();
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            requestBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestKey.append(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            byte[] response = "{\"model\":\"gemini-test\",\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"answer\\\":\\\"hello\\\"}\"}]}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            GeminiLlmAdapter adapter = new GeminiLlmAdapter(properties("http://127.0.0.1:" + server.getAddress().getPort()), new ObjectMapper());
            var response = adapter.generate(new LlmRequest("system", "prompt", 0.1, true, "json"));
            assertThat(response.content()).contains("hello");
            assertThat(response.model()).isEqualTo("gemini-test");
            assertThat(requestBody).contains("system", "prompt", "responseMimeType", "application/json");
            assertThat(requestKey.toString()).isEqualTo("test-secret");
            assertThat(requestBody.toString()).doesNotContain("test-secret");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void checksModelAvailabilityUsingMetadataEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/gemini-test", exchange -> {
            byte[] response = "{\"name\":\"models/gemini-test\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            GeminiLlmAdapter adapter = new GeminiLlmAdapter(properties("http://127.0.0.1:" + server.getAddress().getPort()), new ObjectMapper());
            assertThat(adapter.checkAvailability().status()).isEqualTo(LlmAvailabilityStatus.AVAILABLE);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsMissingKeyWithoutNetworkAccess() {
        AiProperties properties = properties("http://127.0.0.1:1");
        properties.setApiKey("");
        GeminiLlmAdapter adapter = new GeminiLlmAdapter(properties, new ObjectMapper());
        assertThat(adapter.checkAvailability().status()).isEqualTo(LlmAvailabilityStatus.MISCONFIGURED);
        assertThatThrownBy(() -> adapter.generate(new LlmRequest("", "", null, false, null)))
                .hasMessage("Gemini provider configuration is incomplete.");
    }

    @Test
    void rejectsBlockedResponses() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            byte[] response = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            GeminiLlmAdapter adapter = new GeminiLlmAdapter(properties("http://127.0.0.1:" + server.getAddress().getPort()), new ObjectMapper());
            assertThatThrownBy(() -> adapter.generate(new LlmRequest("", "prompt", null, false, null)))
                    .isInstanceOf(LlmClientException.class)
                    .hasMessageContaining("blocked");
        } finally {
            server.stop(0);
        }
    }

    private AiProperties properties(String baseUrl) {
        AiProperties properties = new AiProperties();
        properties.setProvider("gemini");
        properties.setBaseUrl(baseUrl);
        properties.setModel("gemini-test");
        properties.setApiKey("test-secret");
        properties.setConnectTimeout(Duration.ofMillis(100));
        properties.setRequestTimeout(Duration.ofMillis(500));
        return properties;
    }
}
