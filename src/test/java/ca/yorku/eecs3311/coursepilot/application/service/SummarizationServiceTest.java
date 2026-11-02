package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.application.dto.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
}
