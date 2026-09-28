package ca.yorku.eecs3311.coursepilot.domain.repository;

import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    boolean existsByCourseCode(String courseCode);
}
