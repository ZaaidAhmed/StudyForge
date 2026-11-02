package ca.yorku.eecs3311.coursepilot.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvEnvironmentPostProcessorTest {
    @Test
    void parsesCommentsQuotesAndOnlyValidKeys() {
        var values = DotEnvEnvironmentPostProcessor.parse(List.of(
                "# comment", "GEMINI_API_KEY=abc", "COURSEPILOT_AI_MODEL=\"gemini-test\"",
                "EMPTY=", "not valid=value", "NO_SEPARATOR"));
        assertThat(values).containsEntry("GEMINI_API_KEY", "abc")
                .containsEntry("COURSEPILOT_AI_MODEL", "gemini-test")
                .containsEntry("EMPTY", "")
                .doesNotContainKey("not valid");
    }
}
