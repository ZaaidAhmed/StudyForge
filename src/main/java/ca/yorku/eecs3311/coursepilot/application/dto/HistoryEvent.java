package ca.yorku.eecs3311.coursepilot.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record HistoryEvent(Instant occurredAt, String type, String title, String detail,
                           String primaryDetail, String secondaryDetail,
                           String actionLabel, String actionUrl, String groupTitle) {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    public HistoryEvent(Instant occurredAt, String type, String title, String detail) {
        this(occurredAt, type, title, detail, detail, null, null, null, null);
    }

    public HistoryEvent(Instant occurredAt, String type, String title, String detail,
                        String primaryDetail, String secondaryDetail,
                        String actionLabel, String actionUrl) {
        this(occurredAt, type, title, detail, primaryDetail, secondaryDetail,
                actionLabel, actionUrl, null);
    }

    public String displayType() {
        if (type == null) return "Learning activity";
        return switch (type) {
            case "FLASHCARD_REVIEW" -> "Flashcard review";
            case "QUIZ_ATTEMPT" -> "Quiz attempt";
            case "STUDY_PLAN" -> "Study plan";
            default -> "Learning activity";
        };
    }

    public String styleKey() {
        if (type == null) return "activity";
        return switch (type) {
            case "FLASHCARD_REVIEW" -> "flashcard";
            case "QUIZ_ATTEMPT" -> "quiz";
            case "STUDY_PLAN" -> "plan";
            default -> "activity";
        };
    }

    public String displayTimestamp() {
        return occurredAt == null ? "Date unavailable" : TIMESTAMP_FORMAT.format(occurredAt);
    }

    public String displayTime() {
        return occurredAt == null ? "Time unavailable" : TIME_FORMAT.format(occurredAt);
    }

    public LocalDate localDate() {
        return occurredAt == null ? null : occurredAt.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public String dateGroupKey() {
        LocalDate date = localDate();
        return date == null ? "unknown" : date.toString();
    }

    public String displayDate() {
        LocalDate date = localDate();
        return date == null ? "Date unavailable" : DAY_FORMAT.format(date);
    }

    public boolean hasAction() {
        return actionLabel != null && !actionLabel.isBlank()
                && actionUrl != null && actionUrl.startsWith("/courses/");
    }
}
