package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.UpdateCourseRequest;
import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseDocumentRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.DocumentChunkRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardReviewRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.QuizAttemptRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.QuizRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock CourseRepository repository;
    @Mock CourseDocumentRepository documentRepository;
    @Mock DocumentChunkRepository chunkRepository;
    @Mock FlashcardRepository flashcardRepository;
    @Mock FlashcardReviewRepository reviewRepository;
    @Mock QuizAttemptRepository quizAttemptRepository;
    @Mock QuizRepository quizRepository;
    @Mock StudyPlanRepository studyPlanRepository;
    @InjectMocks CourseService service;

    @Test
    void normalizesCourseCodeBeforeCheckingAndSaving() {
        when(repository.findByCourseCode("EECS3311")).thenReturn(Optional.empty());
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createCourse(new CreateCourseRequest(" eecs3311 ", " Software Design ", " Notes ", null));

        assertThat(result.courseCode()).isEqualTo("EECS3311");
        assertThat(result.name()).isEqualTo("Software Design");
    }

    @Test
    void rejectsDuplicateCourseCode() {
        when(repository.findByCourseCode("EECS3311")).thenReturn(Optional.of(new Course("EECS3311", "Existing", null, null)));

        assertThatThrownBy(() -> service.createCourse(new CreateCourseRequest("EECS3311", "Other", null, null)))
                .isInstanceOf(DuplicateCourseCodeException.class)
                .hasMessageContaining("EECS3311");
    }

    @Test
    void rejectsMissingCourseName() {
        assertThatThrownBy(() -> service.createCourse(new CreateCourseRequest("EECS3311", " ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Course name is required.");
    }

    @Test
    void updatesExistingCourseAndKeepsCodeNormalized() {
        Course existing = new Course("EECS3311", "Old", null, null);
        ReflectionTestUtils.setField(existing, "id", 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.findByCourseCode("EECS3311")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateCourse(7L, new UpdateCourseRequest(" eecs3311 ", "New", null, LocalDate.of(2026, 12, 10)));

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.examDate()).isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    void reportsMissingCourse() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCourse(99L))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deletesCourseOwnedRecordsBeforeDeletingCourse() {
        Course existing = new Course("EECS3311", "Software Design", null, null);
        ReflectionTestUtils.setField(existing, "id", 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(reviewRepository.findByCourseIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(flashcardRepository.findByCourseIdWithSources(7L)).thenReturn(List.of());
        when(quizAttemptRepository.findByCourseId(7L)).thenReturn(List.of());
        when(quizRepository.findByCourseId(7L)).thenReturn(List.of());
        when(studyPlanRepository.findByCourseId(7L)).thenReturn(List.of());
        when(documentRepository.findByCourseIdOrderByUploadedAtDesc(7L)).thenReturn(List.of());

        service.deleteCourse(7L);

        verify(reviewRepository).deleteAll(List.of());
        verify(flashcardRepository).deleteAll(List.of());
        verify(quizAttemptRepository).deleteAll(List.of());
        verify(quizRepository).deleteAll(List.of());
        verify(studyPlanRepository).deleteAll(List.of());
        verify(documentRepository).deleteAll(List.of());
        verify(repository).delete(existing);
    }
}
