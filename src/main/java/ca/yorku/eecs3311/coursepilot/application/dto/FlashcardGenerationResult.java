package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;
import java.util.Map;

public record FlashcardGenerationResult(int requestedCount, int createdCount, List<FlashcardResponse> flashcards,
                                        String reason, int generatedCount, int validatedCount, int deduplicatedCount,
                                        int rejectedCount, int savedCount, int returnedCount,
                                        Map<String, Integer> rejectionReasons, int retryCount,
                                        String failureCategory, int backfillAttempts) {
    public FlashcardGenerationResult {
        flashcards = List.copyOf(flashcards);
        rejectionReasons = rejectionReasons == null ? Map.of() : Map.copyOf(rejectionReasons);
        failureCategory = failureCategory == null ? "" : failureCategory;
    }
    public FlashcardGenerationResult(int requestedCount, int createdCount, List<FlashcardResponse> flashcards, String reason) {
        this(requestedCount, createdCount, flashcards, reason, createdCount, createdCount, 0,
                0, createdCount, flashcards == null ? 0 : flashcards.size(), Map.of(), 0, "", 0);
    }
    public boolean complete() { return createdCount == requestedCount; }
}
