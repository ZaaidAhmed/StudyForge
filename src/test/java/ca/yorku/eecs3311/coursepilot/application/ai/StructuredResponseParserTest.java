package ca.yorku.eecs3311.coursepilot.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredResponseParserTest {
    private final StructuredResponseParser parser = new StructuredResponseParser(new ObjectMapper());

    @Test
    void parsesPlainAndMarkdownFencedJson() {
        assertThat(parser.parse("{\"answer\":\"ok\"}", Answer.class).answer()).isEqualTo("ok");
        assertThat(parser.parse("```json\n{\"answer\":\"fenced\"}\n```", Answer.class).answer()).isEqualTo("fenced");
    }

    @Test
    void reportsControlledErrorForInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not json", Answer.class))
                .isInstanceOf(StructuredOutputParseException.class)
                .hasMessage("The AI response was not valid JSON.");
    }

    @Test
    void parsesQuizSourceIdAliasesWithoutLosingGroundingMetadata() {
        String json = "{\"questions\":[{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"Q\",\"options\":[\"A\",\"B\"],\"correct_answer\":\"A\",\"source_ids\":[\"S1\"]}]}";
        QuizGenerationLlmResponse response = parser.parse(json, QuizGenerationLlmResponse.class);
        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().getFirst().sourceIds()).containsExactly("S1");
        assertThat(response.questions().getFirst().correctAnswer()).isEqualTo("A");
    }

    private record Answer(String answer) { }
}
