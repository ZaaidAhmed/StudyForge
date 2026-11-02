package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.domain.repository.QuizRepository;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class QuizGenerationServiceTest {
    private final CourseMaterialService materials = mock(CourseMaterialService.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final QuizRepository quizzes = mock(QuizRepository.class);
    private final QuizGenerationService service = new QuizGenerationService(materials, llm, new PromptBuilder(),
            new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), quizzes, 12000, 6000, 20);

    @Test
    void rejectsInvalidCountAndQuestionTypeBeforeLoadingMaterial() {
        assertThatThrownBy(() -> service.generate(1L, 2L, 0, Set.of("MULTIPLE_CHOICE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("between 1 and 20");
        assertThatThrownBy(() -> service.generate(1L, 2L, 21, Set.of("MULTIPLE_CHOICE")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.generate(1L, 2L, 5, Set.of("ESSAY")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsupported");
        verifyNoInteractions(materials, llm, quizzes);
    }

    @Test
    void rejectsFailedMaterialBeforeCallingAi() {
        when(materials.getDocument(1L, 2L)).thenReturn(new DocumentResponse(2L, "notes.pdf", "pdf", "application/pdf", "hash", "FAILED", null, 100, 1, 1));
        assertThatThrownBy(() -> service.generate(1L, 2L, 5, Set.of("MULTIPLE_CHOICE")))
                .isInstanceOf(InsightGenerationException.class).hasMessageContaining("not ready");
        verifyNoInteractions(llm, quizzes);
    }

    @Test
    void rejectsReadyDocumentWithoutChunksWithControlledMessage() {
        when(materials.getDocument(1L, 2L)).thenReturn(new DocumentResponse(2L, "notes.pdf", "pdf", "application/pdf", "hash", "READY", null, 0, 0, 1));
        when(materials.listChunks(1L, 2L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.generate(1L, 2L, 5, Set.of("MULTIPLE_CHOICE")))
                .isInstanceOf(InsightGenerationException.class).hasMessageContaining("No usable material");
        verifyNoInteractions(llm, quizzes);
    }

    @Test
    void successfulGenerationPersistsGroundedMultipleChoiceQuestion() {
        var course = new Course("EECS3311", "Software Design", null, null);
        var document = new CourseDocument(course, "notes.pdf", "stored.pdf", "application/pdf", "pdf", "hash", 40, 1, 1);
        ReflectionTestUtils.setField(document, "id", 2L);
        var chunk = new DocumentChunk(document, 0, "The Strategy pattern selects an algorithm.", 6, 6);
        ReflectionTestUtils.setField(chunk, "id", 9L);
        when(materials.getDocument(1L, 2L)).thenReturn(new DocumentResponse(2L, "notes.pdf", "pdf", "application/pdf", "hash", "READY", null, 40, 1, 1));
        when(materials.listChunks(1L, 2L)).thenReturn(List.of(chunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "test", "ready"));
        when(llm.generate(any())).thenReturn(
                new LlmResponse("{\"concepts\":[{\"name\":\"Strategy pattern\",\"definition\":\"Selects an algorithm at runtime.\",\"example\":\"A sorting strategy can be replaced without changing the client.\",\"sourceIds\":[\"S1\"]}]}", "test", 10),
                new LlmResponse("{\"questions\":[{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"What does the Strategy pattern select at runtime?\",\"options\":[\"An algorithm\",\"A database schema\",\"A network socket\",\"A user credential\"],\"correct_answer\":\"An algorithm\",\"source_ids\":[\"S1\"]}]}", "test", 10));

        var result = service.generate(1L, 2L, 1, Set.of("MULTIPLE_CHOICE"));

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.quiz().questions()).singleElement().satisfies(question -> {
            assertThat(question.prompt()).contains("Strategy");
            assertThat(question.sources()).singleElement().satisfies(source -> assertThat(source.chunkId()).isEqualTo(9L));
        });
        verify(quizzes).save(any(Quiz.class));
    }

    @Test
    void fillsRequestedCountAcrossSmallBatchesInsteadOfSavingPartialQuiz() {
        var course = new Course("EECS3311", "Software Design", null, null);
        var document = new CourseDocument(course, "notes.pdf", "stored.pdf", "application/pdf", "pdf", "hash", 40, 1, 1);
        ReflectionTestUtils.setField(document, "id", 2L);
        var chunk = new DocumentChunk(document, 0, "Strategy selects an algorithm. SOLID improves maintainability.", 6, 6);
        ReflectionTestUtils.setField(chunk, "id", 9L);
        when(materials.getDocument(1L, 2L)).thenReturn(new DocumentResponse(2L, "notes.pdf", "pdf", "application/pdf", "hash", "READY", null, 40, 1, 1));
        when(materials.listChunks(1L, 2L)).thenReturn(List.of(chunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "test", "ready"));
        when(llm.generate(any())).thenReturn(
                new LlmResponse("{\"concepts\":[{\"name\":\"Strategy pattern\",\"definition\":\"Selects an algorithm at runtime.\",\"example\":\"A sorting strategy can be replaced without changing the client.\",\"sourceIds\":[\"S1\"]}]}", "test", 10),
                new LlmResponse("{\"questions\":[{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"What does the Strategy pattern select at runtime?\",\"options\":[\"An algorithm\",\"A database schema\",\"A network socket\",\"A user credential\"],\"correct_answer\":\"An algorithm\",\"source_ids\":[\"S1\"]}]}", "test", 10),
                new LlmResponse("{\"questions\":[{\"type\":\"TRUE_FALSE\",\"prompt\":\"SOLID principles are intended to improve software maintainability.\",\"options\":[],\"correct_answer\":\"true\",\"source_ids\":[\"S1\"]}]}", "test", 10));

        var result = service.generate(1L, 2L, 2, Set.of("MULTIPLE_CHOICE", "TRUE_FALSE"));

        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.quiz().questions()).hasSize(2);
        verify(llm, times(3)).generate(any());
        verify(quizzes).save(any(Quiz.class));
    }

    @Test
    void refusesToPersistAnIncompleteQuizWhenTheModelCannotFillTarget() {
        var course = new Course("EECS3311", "Software Design", null, null);
        var document = new CourseDocument(course, "notes.pdf", "stored.pdf", "application/pdf", "pdf", "hash", 40, 1, 1);
        ReflectionTestUtils.setField(document, "id", 2L);
        var chunk = new DocumentChunk(document, 0, "Strategy selects an algorithm.", 6, 6);
        ReflectionTestUtils.setField(chunk, "id", 9L);
        when(materials.getDocument(1L, 2L)).thenReturn(new DocumentResponse(2L, "notes.pdf", "pdf", "application/pdf", "hash", "READY", null, 40, 1, 1));
        when(materials.listChunks(1L, 2L)).thenReturn(List.of(chunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "test", "ready"));
        when(llm.generate(any())).thenReturn(
                new LlmResponse("{\"concepts\":[{\"name\":\"Strategy pattern\",\"definition\":\"Selects an algorithm at runtime.\",\"example\":\"A sorting strategy can be replaced without changing the client.\",\"sourceIds\":[\"S1\"]}]}", "test", 10),
                new LlmResponse("{\"questions\":[{\"type\":\"MULTIPLE_CHOICE\",\"prompt\":\"Which design pattern selects an algorithm at runtime?\",\"options\":[\"Strategy\",\"Observer\",\"Factory\",\"Singleton\"],\"correct_answer\":\"Strategy\",\"source_ids\":[\"S1\"]}]}", "test", 10));

        assertThatThrownBy(() -> service.generate(1L, 2L, 2, Set.of("MULTIPLE_CHOICE")))
                .isInstanceOf(InsightGenerationException.class)
                .hasMessageContaining("generated 1 valid questions out of 2");
        verify(quizzes, never()).save(any(Quiz.class));
    }
}
