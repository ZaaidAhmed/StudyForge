package ca.yorku.eecs3311.coursepilot.application.dto;

import ca.yorku.eecs3311.coursepilot.domain.model.Course;

import java.time.Instant;
import java.time.LocalDate;

public record CourseResponse(Long id, String courseCode, String name, String description,
                             LocalDate examDate, Instant createdAt, Instant updatedAt) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getCourseCode(), course.getName(),
                course.getDescription(), course.getExamDate(), course.getCreatedAt(), course.getUpdatedAt());
    }
}
