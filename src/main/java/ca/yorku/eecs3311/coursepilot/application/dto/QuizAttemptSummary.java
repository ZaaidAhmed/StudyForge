package ca.yorku.eecs3311.coursepilot.application.dto;

import java.time.Instant;

public record QuizAttemptSummary(Long id, int correctCount, int totalCount, Instant completedAt) {
}
