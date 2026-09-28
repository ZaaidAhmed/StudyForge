package ca.yorku.eecs3311.coursepilot.application.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {
    @Test
    void keepsPromptSectionsExplicitAndSeparate() {
        LlmRequest request = new PromptBuilder().build("Be concise.", "Explain the term.", "A definition.", "Return one paragraph.");
        assertThat(request.systemPrompt()).isEqualTo("Be concise.");
        assertThat(request.userPrompt()).contains("TASK:\nExplain the term.", "CONTEXT:\nA definition.", "OUTPUT REQUIREMENTS:\nReturn one paragraph.");
    }
}
