package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.domain.repository.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StudyPlanServiceTest {
    private final CourseService courses = mock(CourseService.class);
    private final ProgressService progress = mock(ProgressService.class);
    private final StudyPlanRepository plans = mock(StudyPlanRepository.class);
    private final StudyPlanService service = new StudyPlanService(courses, progress, plans);

    @Test
    void rejectsReversedDatesBeforeLoadingCourseData() {
        assertThatThrownBy(() -> service.create(1L, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 9), 60, "BALANCED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("on or after");
        verifyNoInteractions(courses, progress, plans);
    }

    @Test
    void rejectsNonPositiveMinutesBeforePersistence() {
        assertThatThrownBy(() -> service.create(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), 0, "BALANCED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        verifyNoInteractions(courses, progress, plans);
    }

    @Test
    void rejectsUnknownStrategyBeforePersistence() {
        assertThatThrownBy(() -> service.create(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), 60, "UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported study strategy");
        verifyNoInteractions(courses, progress, plans);
    }
}
