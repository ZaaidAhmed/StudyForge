package ca.yorku.eecs3311.coursepilot.domain.repository;

import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.CoursePilotApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = CoursePilotApplication.class)
class CourseRepositoryTest {
    @Autowired CourseRepository repository;

    @Test
    void persistsAndFindsCourseByNormalizedCode() {
        repository.saveAndFlush(new Course("EECS3311", "Software Design", "Course", null));

        assertThat(repository.findByCourseCode("EECS3311")).isPresent();
        Course course = repository.findByCourseCode("EECS3311").orElseThrow();
        assertThat(course.getName()).isEqualTo("Software Design");
    }

}
