package ca.yorku.eecs3311.coursepilot.infrastructure.ai;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmAvailabilityStatus;
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

class OllamaLlmAdapterTest {
    @Test
    void reportsUnavailableWithoutFailingWhenProviderIsDown() {
        AiProperties properties = properties("http://127.0.0.1:1");
        OllamaLlmAdapter adapter = new OllamaLlmAdapter(properties, new ObjectMapper());
        assertThat(adapter.checkAvailability().status()).isEqualTo(LlmAvailabilityStatus.UNAVAILABLE);
    }

    @Test
    void translatesProviderResponseThroughTheNeutralContract() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var body = new StringBuilder();
        server.createContext("/api/generate", exchange -> {
            body.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"model\":\"test-model\",\"response\":\"hello\",\"done\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AiProperties properties = properties("http://127.0.0.1:" + server.getAddress().getPort());
            OllamaLlmAdapter adapter = new OllamaLlmAdapter(properties, new ObjectMapper());
            var response = adapter.generate(new LlmRequest("system", "prompt", 0.1, true, null));
            assertThat(response.content()).isEqualTo("hello");
            assertThat(response.model()).isEqualTo("test-model");
            assertThat(body).contains("\"model\":\"qwen2.5-coder:14b\"", "\"stream\":false", "\"format\":\"json\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void disabledProviderIsReportedWithoutNetworkAccess() {
        AiProperties properties = properties("http://127.0.0.1:1");
        properties.setEnabled(false);
        OllamaLlmAdapter adapter = new OllamaLlmAdapter(properties, new ObjectMapper());
        assertThat(adapter.checkAvailability().status()).isEqualTo(LlmAvailabilityStatus.DISABLED);
        assertThatThrownBy(() -> adapter.generate(new LlmRequest("", "", null, false, null)))
                .hasMessage("AI integration is disabled.");
    }

    private AiProperties properties(String baseUrl) {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl(baseUrl);
        properties.setConnectTimeout(Duration.ofMillis(100));
        properties.setRequestTimeout(Duration.ofMillis(500));
        return properties;
    }
}
