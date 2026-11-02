package ca.yorku.eecs3311.coursepilot.application.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Normalized quiz response. The aliases keep provider/model schema drift at the boundary. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizGenerationLlmResponse(
        @JsonProperty("questions") @JsonAlias({"quiz_questions", "items"}) List<GeneratedQuestion> questions) {
    public QuizGenerationLlmResponse {
        questions = questions == null ? List.of() : List.copyOf(questions);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedQuestion(
            String type,
            String prompt,
            List<String> options,
            @JsonAlias({"correct_answer", "correctAnswer", "answer"}) String correctAnswer,
            @JsonAlias({"reference_answer", "referenceAnswer"}) String referenceAnswer,
            @JsonAlias({"explanation", "rationale", "why"}) String explanation,
            @JsonAlias({"source_ids", "sourceIds", "sourceIDs"}) List<String> sourceIds) {
        public GeneratedQuestion {
            options = options == null ? List.of() : List.copyOf(options);
            sourceIds = sourceIds == null ? List.of() : List.copyOf(sourceIds);
        }
    }
}
