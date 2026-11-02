package ca.yorku.eecs3311.coursepilot.application.dto;

/** A quiz question paired with its submitted answer and review data. */
public record QuizReviewRow(
        int questionNumber,
        QuizQuestionResponse question,
        QuizAttemptResult.AnswerResult result,
        String correctAnswer,
        String referenceAnswer,
        boolean unanswered,
        QuizReviewFeedback feedback) {
}
