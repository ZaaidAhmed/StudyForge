package ca.yorku.eecs3311.coursepilot.application.dto;

/** A course-scoped, actionable recommendation for the study-plan page. */
public record StudyPlanReviewItem(
        String type,
        String title,
        String courseName,
        Long documentId,
        String documentName,
        Long quizId,
        String reason,
        int priority,
        String status,
        String actionLabel,
        String actionUrl) {
}
