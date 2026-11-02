package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.Map;

public record QuizGenerationResult(int requestedCount, int createdCount, QuizResponseDto quiz,
                                   int generatedCount, int validatedCount, int rejectedCount,
                                   int attemptCount, int providerCallCount, int responseCount,
                                   int parsedBatchCount, int retryCount, String failureCategory,
                                   Map<String, Integer> rejectionReasons, String reason) {
    public QuizGenerationResult {
        rejectionReasons = rejectionReasons == null ? Map.of() : Map.copyOf(rejectionReasons);
        failureCategory = failureCategory == null ? "" : failureCategory;
        reason = reason == null ? "" : reason;
    }

    public QuizGenerationResult(int requestedCount, int createdCount, QuizResponseDto quiz) {
        this(requestedCount, createdCount, quiz, createdCount, createdCount, 0,
                0, 0, 0, 0, 0, "", Map.of(), "");
    }

    /** Compatibility constructor for callers that only need generation/rejection totals. */
    public QuizGenerationResult(int requestedCount, int createdCount, QuizResponseDto quiz,
                                int generatedCount, int rejectedCount,
                                Map<String, Integer> rejectionReasons) {
        this(requestedCount, createdCount, quiz, generatedCount, createdCount, rejectedCount,
                0, 0, 0, 0, 0, "", rejectionReasons, "");
    }
}
