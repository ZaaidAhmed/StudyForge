package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QuizAttemptServiceTest {
    private final QuizRepository quizzes = mock(QuizRepository.class);
    private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
    private final QuizAttemptService service = new QuizAttemptService(quizzes, attempts, llm, new PromptBuilder(),
            new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), chunks);

    @Test
    void gradesObjectiveAnswersAndRecordsUnansweredQuestions() {
        Quiz quiz = mock(Quiz.class);
        QuizQuestion correct = mock(QuizQuestion.class);
        QuizQuestion unanswered = mock(QuizQuestion.class);
        when(correct.getId()).thenReturn(11L); when(correct.getType()).thenReturn(QuizQuestionType.MULTIPLE_CHOICE); when(correct.getCorrectAnswer()).thenReturn("A");
        when(unanswered.getId()).thenReturn(12L); when(unanswered.getType()).thenReturn(QuizQuestionType.TRUE_FALSE); when(unanswered.getCorrectAnswer()).thenReturn("true");
        when(quiz.getQuestions()).thenReturn(List.of(correct, unanswered));
        when(quizzes.findOwned(5L, 2L)).thenReturn(Optional.of(quiz));
        when(attempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.submit(2L, 5L, Map.of(11L, "a"));
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.answers()).extracting("feedback").contains("No answer was submitted.");
        verify(attempts).save(any(QuizAttempt.class));
        verifyNoInteractions(llm);
    }

    @Test
    void fallsBackSafelyWhenShortAnswerAiIsUnavailable() {
        Quiz quiz = mock(Quiz.class);
        QuizQuestion question = mock(QuizQuestion.class);
        when(question.getId()).thenReturn(21L); when(question.getType()).thenReturn(QuizQuestionType.SHORT_ANSWER);
        when(question.getReferenceAnswer()).thenReturn("dependency inversion uses abstractions");
        when(question.getPrompt()).thenReturn("Explain dependency inversion");
        when(quiz.getQuestions()).thenReturn(List.of(question));
        when(quizzes.findOwned(8L, 3L)).thenReturn(Optional.of(quiz));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.UNAVAILABLE, "ollama", "model", "offline"));
        when(attempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.submit(3L, 8L, Map.of(21L, "depend on abstractions"));
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.answers().getFirst().feedback()).contains("unavailable");
        verify(attempts).save(any(QuizAttempt.class));
    }

    @Test
    void rejectsAnswersForQuestionsOutsideTheSelectedQuiz() {
        Quiz quiz = mock(Quiz.class);
        QuizQuestion question = mock(QuizQuestion.class);
        when(question.getId()).thenReturn(11L);
        when(question.getType()).thenReturn(QuizQuestionType.MULTIPLE_CHOICE);
        when(question.getCorrectAnswer()).thenReturn("A");
        when(quiz.getQuestions()).thenReturn(List.of(question));
        when(quizzes.findOwned(5L, 2L)).thenReturn(Optional.of(quiz));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.submit(2L, 5L, Map.of(999L, "A")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to this quiz");
        verify(attempts, never()).save(any());
    }
}
