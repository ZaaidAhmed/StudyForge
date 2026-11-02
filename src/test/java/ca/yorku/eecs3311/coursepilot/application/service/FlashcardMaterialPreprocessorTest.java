package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;
import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlashcardMaterialPreprocessorTest {
    private final FlashcardMaterialPreprocessor preprocessor = new FlashcardMaterialPreprocessor();

    @Test
    void removesRepeatedHeadersPageNumbersProfessorLinesAndOcrArtifacts() {
        Course course = new Course("EECS3221", "Operating Systems", null, null);
        CourseDocument document = new CourseDocument(course, "lecture.pdf", "stored.pdf", "application/pdf", "pdf", "hash", 240, 2, 10);
        ReflectionTestUtils.setField(document, "id", 4L);
        DocumentChunk first = new DocumentChunk(document, 0,
                "York University\nR. AlOmari\nPage 1 of 2\nThreads are the basic unit of CPU utilization.\n@@@ 7", 1, 1);
        DocumentChunk second = new DocumentChunk(document, 1,
                "York University\nR. AlOmari\nPage 2 of 2\nThreads share the code and data sections of their process, which makes communication efficient.", 2, 2);
        ReflectionTestUtils.setField(first, "id", 10L);
        ReflectionTestUtils.setField(second, "id", 11L);

        var cleaned = preprocessor.clean(List.of(first, second));

        assertThat(cleaned).hasSize(2);
        assertThat(cleaned).allSatisfy(result -> assertThat(result.snippet())
                .doesNotContain("York University", "AlOmari", "Page 1", "Page 2", "@@@"));
        assertThat(cleaned.get(0).snippet()).contains("Threads are the basic unit");
    }
}
