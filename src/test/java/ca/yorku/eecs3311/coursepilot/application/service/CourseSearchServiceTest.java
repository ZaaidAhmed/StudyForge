package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.search.LexicalRetrievalScorer;
import ca.yorku.eecs3311.coursepilot.application.search.QueryNormalizer;
import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import ca.yorku.eecs3311.coursepilot.domain.model.ProcessingStatus;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseSearchServiceTest {
    private final CourseRepository courses = mock(CourseRepository.class);
    private final DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
    private final CourseSearchService service = new CourseSearchService(courses, chunks, new QueryNormalizer(),
            new LexicalRetrievalScorer(new QueryNormalizer(), 6.0, 4.0, 1.5, 0.25, 2.0), 5, 20, 1.0);

    @Test
    void searchesOnlyTheRequestedCourseAndPreservesSourceMetadata() {
        when(courses.existsById(1L)).thenReturn(true);
        DocumentChunk matching = chunk(1L, 10L, "Lecture 6.pdf", 3, "Strategy pattern selects an algorithm at runtime.");
        when(chunks.findSearchableByCourseId(1L, ProcessingStatus.READY)).thenReturn(List.of(matching));

        var results = service.search(1L, "strategy pattern", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo(10L);
        assertThat(results.get(0).chunkId()).isEqualTo(1L);
        assertThat(results.get(0).documentName()).isEqualTo("Lecture 6.pdf");
        assertThat(results.get(0).pageStart()).isEqualTo(3);
        assertThat(results.get(0).matchedTerms()).containsExactly("strategy", "pattern");
        verify(chunks).findSearchableByCourseId(1L, ProcessingStatus.READY);
    }

    @Test
    void appliesTopKAndStableDocumentThenChunkOrdering() {
        when(courses.existsById(1L)).thenReturn(true);
        var first = chunk(1L, 1L, "notes.txt", 2, "strategy pattern");
        var second = chunk(2L, 2L, "notes.txt", 1, "strategy pattern");
        var third = chunk(3L, 3L, "notes.txt", 0, "strategy pattern");
        when(chunks.findSearchableByCourseId(1L, ProcessingStatus.READY)).thenReturn(List.of(third, second, first));

        var results = service.search(1L, "strategy pattern", 2);

        assertThat(results).extracting(result -> result.chunkId()).containsExactly(1L, 2L);
    }

    @Test
    void rejectsMissingOrBlankQueryAndInvalidLimit() {
        when(courses.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.search(1L, "   !!! ", 5)).hasMessage("Search query is required.");
        assertThatThrownBy(() -> service.search(1L, "strategy", 21)).hasMessageContaining("between 1 and 20");
        when(courses.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.search(99L, "strategy", 5)).isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void returnsNoResultsForIrrelevantQueriesAndNoMaterials() {
        when(courses.existsById(1L)).thenReturn(true);
        when(chunks.findSearchableByCourseId(1L, ProcessingStatus.READY)).thenReturn(List.of());
        assertThat(service.search(1L, "quantum chromodynamics", 5)).isEmpty();
    }

    private DocumentChunk chunk(Long chunkId, Long documentId, String name, int index, String content) {
        Course course = new Course("EECS3311", "Software Design", null, null);
        CourseDocument document = new CourseDocument(course, name, "stored-" + documentId, "text/plain", "txt", "hash" + documentId, content.length(), 1, null);
        ReflectionTestUtils.setField(document, "id", documentId);
        DocumentChunk chunk = new DocumentChunk(document, index, content, 3, 3);
        ReflectionTestUtils.setField(chunk, "id", chunkId);
        return chunk;
    }
}
