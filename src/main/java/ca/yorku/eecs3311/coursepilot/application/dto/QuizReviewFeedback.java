package ca.yorku.eecs3311.coursepilot.application.dto;

/** Safe, presentation-only feedback for quiz review. It intentionally contains no sources or retrieved text. */
public record QuizReviewFeedback(boolean isCorrect, String correctAnswer, String explanation) {
}
