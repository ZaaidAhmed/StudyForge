package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server-side state for one in-progress quiz attempt. */
public final class QuizTakingState {
    private final Map<Long, String> answers = new LinkedHashMap<>();
    private int currentQuestionIndex;
    private QuizAttemptResult submittedAttempt;

    public Map<Long, String> answers() {
        return answers;
    }

    public int currentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void currentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public QuizAttemptResult submittedAttempt() {
        return submittedAttempt;
    }

    public void submittedAttempt(QuizAttemptResult submittedAttempt) {
        this.submittedAttempt = submittedAttempt;
    }

    public void reset() {
        answers.clear();
        currentQuestionIndex = 0;
        submittedAttempt = null;
    }
}
