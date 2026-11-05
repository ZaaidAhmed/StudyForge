package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

/** Presentation data for a saved plan and its actionable session timeline. */
public record StudyPlanCardView(
        StudyPlanResponse plan,
        int completedSessions,
        int completedMinutes,
        int remainingMinutes,
        int progressPercent,
        String primaryActionUrl,
        String primaryActionLabel,
        List<Session> sessions) {

    public StudyPlanCardView {
        sessions = List.copyOf(sessions);
    }

    public record Session(
            StudyPlanResponse.Item item,
            String dayLabel,
            String title,
            String topic,
            String kind,
            String status,
            String priority,
            String reason,
            String actionUrl,
            String actionLabel) { }
}
