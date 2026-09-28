package ca.yorku.eecs3311.coursepilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCourseRequest(
        @NotBlank(message = "Course code is required")
        @Size(max = 32, message = "Course code must be at most 32 characters")
        String courseCode,
        @NotBlank(message = "Course name is required")
        @Size(max = 160, message = "Course name must be at most 160 characters")
        String name,
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,
        LocalDate examDate
) {
}
