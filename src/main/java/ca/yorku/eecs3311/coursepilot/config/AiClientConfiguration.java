package ca.yorku.eecs3311.coursepilot.config;

import ca.yorku.eecs3311.coursepilot.application.ai.LlmClient;
import ca.yorku.eecs3311.coursepilot.infrastructure.ai.GeminiLlmAdapter;
import ca.yorku.eecs3311.coursepilot.infrastructure.ai.OllamaLlmAdapter;
import ca.yorku.eecs3311.coursepilot.infrastructure.ai.RoutingLlmClient;
import ca.yorku.eecs3311.coursepilot.infrastructure.ai.UnavailableLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiClientConfiguration {
    @Bean
    LlmClient llmClient(AiProperties properties, ObjectMapper objectMapper) {
        return new RoutingLlmClient(properties,
                new OllamaLlmAdapter(properties, objectMapper),
                new GeminiLlmAdapter(properties, objectMapper),
                new UnavailableLlmClient(properties.getProvider()));
    }
}
