package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record QuizAttemptResult(Long id, Long quizId, int correctCount, int totalCount, List<AnswerResult> answers) {
    public record AnswerResult(Long questionId, String answer, boolean correct, String feedback) {}
}
