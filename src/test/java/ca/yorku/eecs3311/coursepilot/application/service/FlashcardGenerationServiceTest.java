package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardRepository;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FlashcardGenerationServiceTest {
    private final CourseMaterialService materials = mock(CourseMaterialService.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final FlashcardRepository cards = mock(FlashcardRepository.class);
    private final FlashcardGenerationService service = new FlashcardGenerationService(materials, llm,
            new PromptBuilder(), new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), cards, 12000, 10, 20);

    @Test
    void rejectsCountsOutsideConfiguredBoundaryBeforeLoadingDocument() {
        assertThatThrownBy(() -> service.generate(1L, 3L, 0)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("between 1 and 20");
        assertThatThrownBy(() -> service.generate(1L, 3L, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.generate(1L, 3L, 21)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(materials, llm, cards);
    }

    @Test
    void rejectsProcessingDocumentBeforeCallingAi() {
        when(materials.getDocument(1L, 3L)).thenReturn(new DocumentResponse(3L, "notes.md", "md", "text/markdown", "hash", "PROCESSING", null, 10, 1, null));
        assertThatThrownBy(() -> service.generate(1L, 3L, 10))
                .isInstanceOf(InsightGenerationException.class).hasMessageContaining("not ready");
        verifyNoInteractions(llm, cards);
    }

    @Test
    void rejectsReadyDocumentWithoutChunksWithControlledMessage() {
        when(materials.getDocument(1L, 3L)).thenReturn(new DocumentResponse(3L, "notes.md", "md", "text/markdown", "hash", "READY", null, 0, 0, null));
        when(materials.listChunks(1L, 3L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.generate(1L, 3L, 10))
                .isInstanceOf(InsightGenerationException.class).hasMessageContaining("No usable material");
        verifyNoInteractions(llm, cards);
    }

    @Test
    void successfulGenerationPersistsGroundedCard() {
        var course = new Course("EECS3311", "Software Design", null, null);
        var document = new CourseDocument(course, "notes.md", "stored.md", "text/markdown", "md", "hash", 40, 1, null);
        ReflectionTestUtils.setField(document, "id", 3L);
        var chunk = new DocumentChunk(document, 0, "The Strategy pattern selects an algorithm.", 4, 4);
        ReflectionTestUtils.setField(chunk, "id", 8L);
        when(materials.getDocument(1L, 3L)).thenReturn(new DocumentResponse(3L, "notes.md", "md", "text/markdown", "hash", "READY", null, 40, 1, null));
        when(materials.listChunks(1L, 3L)).thenReturn(List.of(chunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "test", "ready"));
        when(llm.generate(any()))
                .thenReturn(new LlmResponse("{\"concepts\":[{\"name\":\"Strategy pattern\",\"definition\":\"A design approach that selects an algorithm at runtime.\",\"example\":\"A context can switch algorithms without changing its own logic.\",\"sourceIds\":[\"S1\"]}]}", "test", 10))
                .thenReturn(new LlmResponse("{\"flashcards\":[{\"question\":\"What does the Strategy pattern do?\",\"answer\":\"The Strategy pattern selects an algorithm at runtime, allowing the context to change strategies independently.\",\"type\":\"definition\",\"source_ids\":[\"S1\"]}]}", "test", 10));
        var saved = new Flashcard(course, "What does Strategy do?", "The Strategy pattern selects an algorithm at runtime, allowing the context to change strategies independently.");
        ReflectionTestUtils.setField(saved, "id", 20L);
        when(cards.save(any(Flashcard.class))).thenReturn(saved);
        when(cards.countByCourseIdAndSourceDocumentId(1L, 3L)).thenReturn(1L);

        var result = service.generate(1L, 3L, 1);

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.flashcards()).singleElement().satisfies(card -> assertThat(card.question()).contains("Strategy"));
        verify(cards).save(any(Flashcard.class));
    }

    @Test
    void rejectsGenericCardsThatDoNotTestAConcreteIdea() {
        var course = new Course("EECS3311", "Software Design", null, null);
        var document = new CourseDocument(course, "notes.md", "stored.md", "text/markdown", "md", "hash", 80, 1, null);
        ReflectionTestUtils.setField(document, "id", 3L);
        var chunk = new DocumentChunk(document, 0, "The Strategy pattern selects an algorithm at runtime and lets the context vary the algorithm independently.", 4, 4);
        ReflectionTestUtils.setField(chunk, "id", 8L);
        when(materials.getDocument(1L, 3L)).thenReturn(new DocumentResponse(3L, "notes.md", "md", "text/markdown", "hash", "READY", null, 80, 1, null));
        when(materials.listChunks(1L, 3L)).thenReturn(List.of(chunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "test", "ready"));
        when(llm.generate(any()))
                .thenReturn(new LlmResponse("{\"concepts\":[{\"name\":\"Strategy pattern\",\"definition\":\"A design approach that selects an algorithm at runtime and separates it from its context.\",\"example\":\"The context can use different algorithm implementations.\",\"sourceIds\":[\"S1\"]}]}", "test", 10))
                .thenReturn(new LlmResponse("{\"flashcards\":["
                        + "{\"question\":\"What should you remember about this passage?\",\"answer\":\"The passage explains an important idea that students should understand and remember for later study.\",\"type\":\"concept\",\"source_ids\":[\"S1\"]},"
                        + "{\"question\":\"Why does the Strategy pattern let the context vary the algorithm independently?\",\"answer\":\"It separates the algorithm from the context, so the context can use different algorithm implementations without changing its own logic.\",\"type\":\"comparison\",\"source_ids\":[\"S1\"]}]}" , "test", 10));
        var saved = new Flashcard(course, "Why does the Strategy pattern let the context vary the algorithm independently?", "It separates the algorithm from the context, so the context can use different algorithm implementations without changing its own logic.");
        ReflectionTestUtils.setField(saved, "id", 20L);
        when(cards.save(any(Flashcard.class))).thenReturn(saved);
        when(cards.countByCourseIdAndSourceDocumentId(1L, 3L)).thenReturn(2L);

        var result = service.generate(1L, 3L, 2);

        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.flashcards()).hasSize(2);
        assertThat(result.flashcards()).anySatisfy(card ->
                assertThat(card.question()).startsWith("Why does the Strategy pattern"));
    }

    @Test
    void reportsInvalidFlashcardFormatAfterRecoveryRetry() {
        var course = new Course("EECS3311", "Software Design", null, null);
        var document = new CourseDocument(course, "notes.md", "stored.md", "text/markdown", "md", "hash", 80, 1, null);
        ReflectionTestUtils.setField(document, "id", 3L);
        var chunk = new DocumentChunk(document, 0, "The Strategy pattern selects an algorithm at runtime.", 4, 4);
        ReflectionTestUtils.setField(chunk, "id", 8L);
        when(materials.getDocument(1L, 3L)).thenReturn(new DocumentResponse(3L, "notes.md", "md", "text/markdown", "hash", "READY", null, 80, 1, null));
        when(materials.listChunks(1L, 3L)).thenReturn(List.of(chunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "gemini", "test", "ready"));
        when(llm.generate(any()))
                .thenReturn(new LlmResponse("{\"concepts\":[{\"name\":\"Strategy pattern\",\"definition\":\"A design approach that selects an algorithm at runtime.\",\"example\":\"A context can switch algorithms.\",\"sourceIds\":[\"S1\"]}]}", "test", 10))
                .thenReturn(new LlmResponse("not json", "test", 10))
                .thenReturn(new LlmResponse("still not json", "test", 10));

        assertThatThrownBy(() -> service.generate(1L, 3L, 1))
                .isInstanceOf(InsightGenerationException.class)
                .hasMessageContaining("invalid flashcard format");
        verify(cards, never()).save(any(Flashcard.class));
    }
}
