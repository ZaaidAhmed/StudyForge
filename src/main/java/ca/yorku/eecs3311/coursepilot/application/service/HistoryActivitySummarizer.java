package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Collapses per-card review records into one recent activity per material. */
public final class HistoryActivitySummarizer {
    private HistoryActivitySummarizer() { }

    public static List<HistoryEvent> summarize(List<HistoryEvent> events) {
        Map<String, List<HistoryEvent>> reviewsByMaterial = new LinkedHashMap<>();
        List<HistoryEvent> activities = new ArrayList<>();
        for (HistoryEvent event : events == null ? List.<HistoryEvent>of() : events) {
            if ("FLASHCARD_REVIEW".equals(event.type())) {
                String material = event.groupTitle() == null || event.groupTitle().isBlank()
                        ? "Unlinked flashcards" : event.groupTitle();
                reviewsByMaterial.computeIfAbsent(material, ignored -> new ArrayList<>()).add(event);
            } else {
                activities.add(event);
            }
        }
        reviewsByMaterial.forEach((material, reviews) -> {
            HistoryEvent latest = reviews.stream()
                    .max(Comparator.comparing(HistoryEvent::occurredAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElseThrow();
            String count = reviews.size() + (reviews.size() == 1 ? " card review" : " card reviews");
            activities.add(new HistoryEvent(latest.occurredAt(), "FLASHCARD_REVIEW",
                    "Flashcard deck reviewed", count + " | " + material,
                    material, count, latest.actionLabel(), latest.actionUrl(), material));
        });
        return activities.stream()
                .sorted(Comparator.comparing(HistoryEvent::occurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
