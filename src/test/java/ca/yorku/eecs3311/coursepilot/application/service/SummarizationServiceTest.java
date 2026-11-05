package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SummarizationServiceTest {
    private final CourseMaterialService materials = mock(CourseMaterialService.class);
    private final CourseSearchService search = mock(CourseSearchService.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final SummarizationService service = new SummarizationService(materials, search, llm,
            new PromptBuilder(), new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), 12000, 8);

    @Test
    void emptyTopicContextReturnsInsufficientEvidenceWithoutCallingAi() {
        when(search.search(1L, "patterns", 8)).thenReturn(List.of());
        var result = service.summarizeTopic(1L, " patterns ");
        assertThat(result.label()).isEqualTo("patterns");
        assertThat(result.insufficientContext()).isTrue();
        verifyNoInteractions(llm);
    }

    @Test
    void rejectsBlankAndOverlongTopics() {
        assertThatThrownBy(() -> service.summarizeTopic(1L, "  "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("required");
        assertThatThrownBy(() -> service.summarizeTopic(1L, "x".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("200");
        verifyNoInteractions(search, llm);
    }

    @Test
    void unavailableAiIsReportedAfterRelevantEvidenceIsFound() {
        when(search.search(1L, "patterns", 8)).thenReturn(List.of(new SearchResult(10L, 20L, "notes.md", 1,
                "Strategy patterns", 4.0, null, null, List.of("patterns"))));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.UNAVAILABLE, "ollama", "model", "offline"));
        assertThatThrownBy(() -> service.summarizeTopic(1L, "patterns"))
                .isInstanceOf(InsightGenerationException.class).hasMessageContaining("unavailable");
        verify(llm, never()).generate(any());
    }

    @Test
    void successfulTopicSummaryReturnsGroundedSourceMetadata() {
        when(search.search(1L, "patterns", 8)).thenReturn(List.of(new SearchResult(10L, 20L, "notes.md", 1,
                "Strategy patterns", 4.0, 2, 2, List.of("patterns"))));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "ollama", "test", "ready"));
        when(llm.generate(any())).thenReturn(new LlmResponse(
                "{\"summary\":\"The Strategy pattern selects an algorithm at runtime.\",\"keyPoints\":[\"Encapsulates algorithms\"],\"sourceIds\":[\"S1\"],\"insufficientContext\":false}", "test", 12));

        var result = service.summarizeTopic(1L, "patterns");

        assertThat(result.summary()).contains("Strategy pattern");
        assertThat(result.keyPoints()).containsExactly("Encapsulates algorithms");
        assertThat(result.sources()).singleElement().satisfies(source -> {
            assertThat(source.sourceId()).isEqualTo("S1");
            assertThat(source.documentName()).isEqualTo("notes.md");
            assertThat(source.pageStart()).isEqualTo(2);
        });
        assertThat(result.grounded()).isTrue();
    }

    @Test
    void courseSummaryCombinesEvidenceFromEveryReadyDocumentAndSkipsProcessingDocuments() {
        var first = document(1L, "Threads.pdf", "READY");
        var second = document(2L, "Scheduling.pdf", "READY");
        var processing = document(3L, "Unfinished.pdf", "PROCESSING");
        DocumentChunk threadChunk = chunk(101L, "Threads.pdf", 0,
                "A thread contains a program counter, registers, and stack.", 3);
        DocumentChunk schedulingChunk = chunk(201L, "Scheduling.pdf", 0,
                "CPU scheduling selects a process from the ready queue.", 8);
        when(materials.listDocuments(36L)).thenReturn(List.of(first, processing, second));
        when(materials.listChunks(36L, 1L)).thenReturn(List.of(threadChunk));
        when(materials.listChunks(36L, 2L)).thenReturn(List.of(schedulingChunk));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "gemini", "test", "ready"));
        when(llm.generate(any())).thenReturn(new LlmResponse(
                "{\"summary\":\"Threads provide concurrent execution while scheduling allocates CPU time.\",\"keyPoints\":[\"Threads\",\"Scheduling\"],\"sourceIds\":[\"S1\",\"S2\"],\"insufficientContext\":false}",
                "test", 12));

        var result = service.summarizeCourse(36L);

        assertThat(result.summaryType()).isEqualTo("course");
        assertThat(result.label()).contains("2 documents");
        assertThat(result.summary()).contains("concurrent execution");
        assertThat(result.sources()).extracting("documentName").containsExactly("Threads.pdf", "Scheduling.pdf");
        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm).generate(request.capture());
        assertThat(request.getValue().userPrompt()).contains("Threads.pdf", "Scheduling.pdf", "Synthesize evidence from all supplied ready course materials")
                .doesNotContain("Unfinished.pdf");
        verify(materials, never()).listChunks(36L, 3L);
    }

    @Test
    void courseSummaryUsesOneDocumentWhenOnlyOneIsReady() {
        DocumentChunk source = chunk(101L, "Only.pdf", 0, "One source document.", 1);
        when(materials.listDocuments(36L)).thenReturn(List.of(document(1L, "Only.pdf", "READY")));
        when(materials.listChunks(36L, 1L)).thenReturn(List.of(source));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.AVAILABLE, "gemini", "test", "ready"));
        when(llm.generate(any())).thenReturn(new LlmResponse(
                "{\"summary\":\"Summary of the source.\",\"keyPoints\":[],\"sourceIds\":[\"S1\"],\"insufficientContext\":false}", "test", 1));

        var result = service.summarizeCourse(36L);

        assertThat(result.label()).contains("1 document");
        assertThat(result.sources()).singleElement().extracting("documentName").isEqualTo("Only.pdf");
    }

    @Test
    void courseSummaryWithNoReadyMaterialsReturnsActionableEmptyStateWithoutAiCall() {
        when(materials.listDocuments(36L)).thenReturn(List.of(document(1L, "Processing.pdf", "PROCESSING"),
                document(2L, "Failed.pdf", "FAILED")));

        var result = service.summarizeCourse(36L);

        assertThat(result.insufficientContext()).isTrue();
        assertThat(result.summary()).contains("No ready course materials");
        verifyNoInteractions(llm);
        verify(materials, never()).listChunks(eq(36L), anyLong());
    }

    @Test
    void courseSummaryWithReadyButUnreadableMaterialsDoesNotCallAi() {
        when(materials.listDocuments(36L)).thenReturn(List.of(document(1L, "Empty.pdf", "READY")));
        when(materials.listChunks(36L, 1L)).thenReturn(new ArrayList<>());

        var result = service.summarizeCourse(36L);

        assertThat(result.insufficientContext()).isTrue();
        assertThat(result.summary()).contains("readable text");
        verifyNoInteractions(llm);
    }

    @Test
    void courseSummaryDoesNotPretendToCoverDocumentsThatDoNotFitTheContextBudget() {
        SummarizationService constrained = new SummarizationService(materials, search, llm,
                new PromptBuilder(), new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), 200, 8);
        DocumentChunk first = chunk(101L, "First.pdf", 0, "First document content. ".repeat(20), 1);
        DocumentChunk second = chunk(201L, "Second.pdf", 0, "Second document content. ".repeat(20), 1);
        when(materials.listDocuments(36L)).thenReturn(List.of(document(1L, "First.pdf", "READY"),
                document(2L, "Second.pdf", "READY")));
        when(materials.listChunks(36L, 1L)).thenReturn(List.of(first));
        when(materials.listChunks(36L, 2L)).thenReturn(List.of(second));

        var result = constrained.summarizeCourse(36L);

        assertThat(result.insufficientContext()).isTrue();
        assertThat(result.summary()).contains("too many ready course materials");
        verifyNoInteractions(llm);
    }

    @Test
    void courseSummaryBoundsLongEvidenceAndReportsProviderUnavailable() {
        DocumentChunk source = chunk(101L, "Long.pdf", 0, "topic ".repeat(800), 1);
        when(materials.listDocuments(36L)).thenReturn(List.of(document(1L, "Long.pdf", "READY")));
        when(materials.listChunks(36L, 1L)).thenReturn(List.of(source));
        when(llm.checkAvailability()).thenReturn(new LlmAvailability(LlmAvailabilityStatus.UNAVAILABLE, "gemini", "test", "offline"));

        assertThatThrownBy(() -> service.summarizeCourse(36L))
                .isInstanceOf(InsightGenerationException.class).hasMessageContaining("unavailable");
        verify(llm, never()).generate(any());
    }

    private DocumentResponse document(Long id, String filename, String status) {
        return new DocumentResponse(id, filename, "pdf", "application/pdf", "hash", status,
                java.time.Instant.now(), 100, 1, 1);
    }

    private DocumentChunk chunk(Long id, String filename, int index, String content, int page) {
        CourseDocument document = mock(CourseDocument.class);
        when(document.getId()).thenReturn(id);
        when(document.getOriginalFilename()).thenReturn(filename);
        DocumentChunk chunk = mock(DocumentChunk.class);
        when(chunk.getId()).thenReturn(id);
        when(chunk.getDocument()).thenReturn(document);
        when(chunk.getChunkIndex()).thenReturn(index);
        when(chunk.getContent()).thenReturn(content);
        when(chunk.getStartPage()).thenReturn(page);
        when(chunk.getEndPage()).thenReturn(page);
        return chunk;
    }
}
