package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseDocumentRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.DocumentChunkRepository;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentExtractorFactory;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.DocumentValidator;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.FileStorageService;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.HashService;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.TextChunker;
import ca.yorku.eecs3311.coursepilot.infrastructure.document.TextNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseMaterialServiceTest {
    @TempDir Path tempDir;

    @Test
    void rejectsSameHashForTheSameCourseBeforeWritingAnotherFile() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseDocumentRepository documents = mock(CourseDocumentRepository.class);
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        Course course = new Course("EECS3311", "Software Design", null, null);
        when(courses.findById(1L)).thenReturn(Optional.of(course));
        CourseDocument existing = mock(CourseDocument.class);
        when(documents.findByCourseIdAndSha256Hash(1L, new HashService().sha256("notes".getBytes())))
                .thenReturn(Optional.of(existing));

        var service = new CourseMaterialService(courses, documents, chunks, new DocumentValidator(1000), new HashService(),
                new FileStorageService(tempDir.toString()), new DocumentExtractorFactory(java.util.List.of(new ca.yorku.eecs3311.coursepilot.infrastructure.document.TextDocumentExtractor())),
                new TextNormalizer(), new TextChunker(1200, 150));

        assertThatThrownBy(() -> service.importMaterial(1L, "copy.txt", "text/plain", "notes".getBytes()))
                .isInstanceOf(DuplicateDocumentException.class)
                .hasMessageContaining("already been uploaded");
    }

    @Test
    void refusesToExposeADocumentThroughAnotherCourse() throws Exception {
        CourseRepository courses = mock(CourseRepository.class);
        CourseDocumentRepository documents = mock(CourseDocumentRepository.class);
        DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
        Course owner = new Course("EECS3311", "Software Design", null, null);
        CourseDocument document = new CourseDocument(owner, "notes.txt", "stored.txt", "text/plain", "txt", "hash", 5, 1, null);
        setId(owner, 1L);
        setId(document, 7L);
        when(documents.findById(7L)).thenReturn(Optional.of(document));

        var service = new CourseMaterialService(courses, documents, chunks, new DocumentValidator(1000), new HashService(),
                new FileStorageService(tempDir.toString()), new DocumentExtractorFactory(java.util.List.of(new ca.yorku.eecs3311.coursepilot.infrastructure.document.TextDocumentExtractor())),
                new TextNormalizer(), new TextChunker(1200, 150));

        assertThatThrownBy(() -> service.getDocument(2L, 7L))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    private static void setId(CourseDocument document, Long id) throws Exception {
        var field = CourseDocument.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(document, id);
    }

    private static void setId(Course course, Long id) throws Exception {
        var field = Course.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(course, id);
    }
}
