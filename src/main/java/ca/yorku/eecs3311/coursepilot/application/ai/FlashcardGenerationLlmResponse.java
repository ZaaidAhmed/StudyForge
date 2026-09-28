package ca.yorku.eecs3311.coursepilot.application.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlashcardGenerationLlmResponse(List<GeneratedFlashcardDto> flashcards) {
    public FlashcardGenerationLlmResponse { flashcards = flashcards == null ? List.of() : flashcards; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedFlashcardDto(String question, String answer, @JsonAlias("source_ids") List<String> sourceIds) {
        public GeneratedFlashcardDto { sourceIds = sourceIds == null ? List.of() : sourceIds; }
    }
}
