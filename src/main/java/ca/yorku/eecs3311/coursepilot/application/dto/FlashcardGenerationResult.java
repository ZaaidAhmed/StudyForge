package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record FlashcardGenerationResult(int requestedCount, int createdCount, List<FlashcardResponse> flashcards) {
    public FlashcardGenerationResult { flashcards = List.copyOf(flashcards); }
}
