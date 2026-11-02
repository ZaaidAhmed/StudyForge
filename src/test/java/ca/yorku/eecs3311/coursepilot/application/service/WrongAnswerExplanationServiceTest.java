package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WrongAnswerExplanationServiceTest {
    private final QuizRepository quizzes = mock(QuizRepository.class);
    private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
    private final WrongAnswerExplanationService service = new WrongAnswerExplanationService(quizzes, llm,
            new PromptBuilder(), new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), chunks);
    private final WrongAnswerExplanationService attemptScopedService = new WrongAnswerExplanationService(quizzes, attempts, llm,
            new PromptBuilder(), new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), chunks);

    @Test
    void unavailableAiProducesExplicitUntrustedState() {
        Quiz quiz = quizWithQuestion(7L);
        when(quizzes.findOwned(4L, 2L)).thenReturn(Optional.of(quiz));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.UNAVAILABLE, "ollama", "model", "offline"));
        var result = service.explain(2L, 4L, 7L, "wrong");
        assertThat(result.unavailable()).isTrue();
        assertThat(result.sources()).isEmpty();
        assertThat(result.explanation()).contains("unavailable");
        verify(llm, never()).generate(any());
    }

    @Test
    void malformedGroundedResponseIsNotPresentedAsEvidence() {
        Quiz quiz = quizWithQuestion(8L);
        when(quizzes.findOwned(4L, 2L)).thenReturn(Optional.of(quiz));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "model", "ok"));
        when(llm.generate(any())).thenReturn(new LlmResponse("{\"explanation\":\"unsupported\",\"sourceIds\":[\"S99\"]}", "model", 1));
        var result = service.explain(2L, 4L, 8L, "wrong");
        assertThat(result.unavailable()).isTrue();
        assertThat(result.sources()).isEmpty();
        assertThat(result.explanation()).contains("could not be validated");
    }

    @Test
    void successfulExplanationReturnsValidatedEvidence() {
        Quiz quiz = quizWithQuestion(9L);
        QuizQuestion question = quiz.getQuestions().get(0);
        Course course = new Course("EECS3311", "Software Design", null, null);
        CourseDocument document = new CourseDocument(course, "notes.pdf", "stored.pdf", "application/pdf", "pdf", "hash", 30, 1, 1);
        org.springframework.test.util.ReflectionTestUtils.setField(document, "id", 10L);
        DocumentChunk chunk = new DocumentChunk(document, 0, "Strategy selects an algorithm at runtime.", 5, 5);
        org.springframework.test.util.ReflectionTestUtils.setField(chunk, "id", 22L);
        QuizSource source = mock(QuizSource.class);
        when(source.getChunkId()).thenReturn(22L);
        when(question.getSources()).thenReturn(List.of(source));
        when(quizzes.findOwned(4L, 2L)).thenReturn(Optional.of(quiz));
        when(chunks.findById(22L)).thenReturn(Optional.of(chunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "model", "ok"));
        when(llm.generate(any())).thenReturn(new LlmResponse("{\"explanation\":\"The answer reverses the role of Strategy: it selects an algorithm rather than storing data.\",\"sourceIds\":[\"S1\"]}", "model", 1));

        var result = service.explain(2L, 4L, 9L, "A database");

        assertThat(result.unavailable()).isFalse();
        assertThat(result.explanation()).contains("selects an algorithm");
        assertThat(result.sources()).singleElement().satisfies(s -> {
            assertThat(s.documentName()).isEqualTo("notes.pdf");
            assertThat(s.chunkId()).isEqualTo(22L);
        });
    }

    @Test
    void attemptScopedExplanationUsesPersistedAnswerAndValidatesOwnership() {
        Quiz quiz = quizWithQuestion(10L);
        QuizQuestion question = quiz.getQuestions().getFirst();
        QuizAttempt attempt = mock(QuizAttempt.class);
        QuizResponse response = mock(QuizResponse.class);
        when(response.getQuestion()).thenReturn(question);
        when(response.getStudentAnswer()).thenReturn("persisted wrong answer");
        when(response.isCorrect()).thenReturn(false);
        when(attempt.getResponses()).thenReturn(List.of(response));
        when(quizzes.findOwned(4L, 2L)).thenReturn(Optional.of(quiz));
        when(attempts.findOwned(55L, 4L, 2L)).thenReturn(Optional.of(attempt));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.UNAVAILABLE, "ollama", "model", "offline"));

        var result = attemptScopedService.explain(2L, 4L, 55L, 10L);

        assertThat(result.unavailable()).isTrue();
        assertThat(result.explanation()).contains("unavailable");
        verify(attempts).findOwned(55L, 4L, 2L);
        verify(llm, never()).generate(any());
    }

    private Quiz quizWithQuestion(Long id) {
        Quiz quiz = mock(Quiz.class);
        QuizQuestion question = mock(QuizQuestion.class);
        when(question.getId()).thenReturn(id);
        when(question.getPrompt()).thenReturn("Explain the concept");
        when(question.getReferenceAnswer()).thenReturn("reference");
        when(question.getSources()).thenReturn(List.of());
        when(quiz.getQuestions()).thenReturn(List.of(question));
        return quiz;
    }
}
