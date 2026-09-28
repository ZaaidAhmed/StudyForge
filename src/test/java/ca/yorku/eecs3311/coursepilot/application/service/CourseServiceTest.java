package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.UpdateCourseRequest;
import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseRepository;
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

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock CourseRepository repository;
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
}
