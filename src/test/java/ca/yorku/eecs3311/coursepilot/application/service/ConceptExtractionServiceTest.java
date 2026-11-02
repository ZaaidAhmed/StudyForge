package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.ai.*;
import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.domain.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ConceptExtractionServiceTest {
    private final CourseMaterialService materials = mock(CourseMaterialService.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final ConceptExtractionService service = new ConceptExtractionService(materials, llm,
            new PromptBuilder(), new StructuredResponseParser(new ObjectMapper()), new GroundingContextBuilder(), 12000);

    @Test
    void doesNotCallAiForProcessingDocument() {
        when(materials.getDocument(1L, 4L)).thenReturn(new DocumentResponse(4L, "notes.md", "md", "text/markdown", "hash", "PROCESSING", null, 10, 1, null));
        assertThatThrownBy(() -> service.extract(1L, 4L))
                .isInstanceOf(InsightGenerationException.class).hasMessageContaining("not ready");
        verifyNoInteractions(llm);
    }

    @Test
    void returnsEmptyConceptsWhenReadyDocumentHasNoUsableChunks() {
        when(materials.getDocument(1L, 4L)).thenReturn(new DocumentResponse(4L, "notes.md", "md", "text/markdown", "hash", "READY", null, 0, 0, null));
        when(materials.listChunks(1L, 4L)).thenReturn(List.of());
        assertThat(service.extract(1L, 4L)).isEmpty();
        verifyNoInteractions(llm);
    }
}
