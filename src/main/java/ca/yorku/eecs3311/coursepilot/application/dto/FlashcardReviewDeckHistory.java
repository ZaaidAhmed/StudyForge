package ca.yorku.eecs3311.coursepilot.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public record FlashcardReviewDeckHistory(String documentName, List<HistoryEvent> reviews) {
    public FlashcardReviewDeckHistory {
        documentName = documentName == null || documentName.isBlank() ? "Unlinked flashcards" : documentName;
        reviews = List.copyOf(reviews == null ? List.of() : reviews);
    }

    public int reviewCount() {
        return reviews.size();
    }

    public String latestReviewLabel() {
        if (reviews.isEmpty()) return "No review date";
        return "Latest review: " + reviews.getFirst().displayDate();
    }

    public Instant latestOccurredAt() {
        return reviews.isEmpty() ? null : reviews.getFirst().occurredAt();
    }

    public List<HistoryDayGroup> reviewDays() {
        Map<String, List<HistoryEvent>> byDate = new LinkedHashMap<>();
        reviews.forEach(review -> byDate.computeIfAbsent(review.dateGroupKey(), ignored -> new ArrayList<>()).add(review));
        return byDate.values().stream()
                .map(dayReviews -> new HistoryDayGroup(dayReviews.getFirst().displayDate(), dayReviews))
                .toList();
    }

    public String actionUrl() {
        return reviews.stream().filter(HistoryEvent::hasAction)
                .map(HistoryEvent::actionUrl).findFirst().orElse(null);
    }
}
