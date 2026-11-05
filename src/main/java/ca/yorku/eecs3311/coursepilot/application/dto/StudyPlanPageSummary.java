package ca.yorku.eecs3311.coursepilot.application.dto;

import java.time.LocalDate;
import java.util.List;

/** Derived, presentation-only data for the study-plan workspace. */
public record StudyPlanPageSummary(
        int savedPlanCount,
        int scheduledMinutes,
        int scheduledSessionCount,
        StudyPlanResponse latestPlan,
        NextSession nextSession,
        boolean hasProgress,
        double masteryScore,
        String masteryStatus,
        int trackedTopicCount,
        List<WeeklyWorkload> weeklyWorkload,
        String courseName,
        String courseCode,
        List<StudyPlanReviewItem> reviewItems) {

    public StudyPlanPageSummary {
        weeklyWorkload = List.copyOf(weeklyWorkload);
        reviewItems = List.copyOf(reviewItems);
    }

    public record NextSession(LocalDate date, String activity, int minutes) { }

    public record WeeklyWorkload(String weekLabel, int scheduledMinutes, int sessionCount, int barPercent) { }
}
