package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryDayGroup;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardReviewDeckHistory;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryDisplaySection;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses/{courseId}/history")
public class HistoryController {
    private static final Set<String> EVENT_TYPES = Set.of("FLASHCARD_REVIEW", "QUIZ_ATTEMPT", "STUDY_PLAN");

    private final StudyAgentFacade facade;
    private final LearningHistoryService history;

    public HistoryController(StudyAgentFacade facade, LearningHistoryService history) {
        this.facade = facade;
        this.history = history;
    }

    @GetMapping
    public String page(@PathVariable Long courseId,
                       @RequestParam(required = false) String type,
                       Model model) {
        var course = facade.getCourse(courseId);
        List<HistoryEvent> allEvents = history.list(courseId);
        String selectedType = normalizeType(type);
        List<HistoryEvent> visibleEvents = selectedType.equals("NONE") ? List.of()
                : selectedType.equals("ALL") ? allEvents
                : allEvents.stream().filter(event -> selectedType.equals(event.type())).toList();
        List<HistoryEvent> timelineEvents = selectedType.equals("FLASHCARD_REVIEW") ? List.of()
                : selectedType.equals("ALL") ? visibleEvents.stream()
                .filter(event -> !"FLASHCARD_REVIEW".equals(event.type())).toList()
                : visibleEvents;

        model.addAttribute("course", course);
        model.addAttribute("history", visibleEvents);
        model.addAttribute("timelineHistory", timelineEvents);
        List<HistoryDayGroup> days = groupByDate(timelineEvents);
        List<FlashcardReviewDeckHistory> decks = groupFlashcardReviews(visibleEvents);
        model.addAttribute("historyGroups", days);
        model.addAttribute("flashcardReviewDecks", decks);
        model.addAttribute("historySections", orderedSections(selectedType, days, decks));
        model.addAttribute("selectedHistoryType", selectedType);
        model.addAttribute("historyExpanded", !selectedType.equals("NONE"));
        model.addAttribute("totalHistoryCount", allEvents.size());
        model.addAttribute("flashcardReviewCount", count(allEvents, "FLASHCARD_REVIEW"));
        model.addAttribute("quizAttemptCount", count(allEvents, "QUIZ_ATTEMPT"));
        model.addAttribute("studyPlanCount", count(allEvents, "STUDY_PLAN"));
        model.addAttribute("activeNav", "history");
        return "history/index";
    }

    private String normalizeType(String type) {
        String normalized = type == null || type.isBlank() ? "NONE" : type.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ALL") || EVENT_TYPES.contains(normalized) ? normalized : "NONE";
    }

    private long count(List<HistoryEvent> events, String type) {
        return events.stream().filter(event -> type.equals(event.type())).count();
    }

    private List<HistoryDayGroup> groupByDate(List<HistoryEvent> events) {
        Map<String, List<HistoryEvent>> groups = new LinkedHashMap<>();
        events.forEach(event -> groups.computeIfAbsent(event.dateGroupKey(), ignored -> new ArrayList<>()).add(event));
        return groups.values().stream()
                .map(dayEvents -> new HistoryDayGroup(dayEvents.getFirst().displayDate(), dayEvents))
                .toList();
    }

    private List<FlashcardReviewDeckHistory> groupFlashcardReviews(List<HistoryEvent> events) {
        Map<String, List<HistoryEvent>> groups = new LinkedHashMap<>();
        events.stream().filter(event -> "FLASHCARD_REVIEW".equals(event.type()))
                .forEach(event -> {
                    String title = event.groupTitle() == null || event.groupTitle().isBlank()
                            ? "Unlinked flashcards" : event.groupTitle();
                    groups.computeIfAbsent(title, ignored -> new ArrayList<>()).add(event);
                });
        return groups.entrySet().stream()
                .map(entry -> new FlashcardReviewDeckHistory(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<HistoryDisplaySection> orderedSections(String selectedType, List<HistoryDayGroup> days,
                                                        List<FlashcardReviewDeckHistory> decks) {
        List<HistoryDisplaySection> sections = new ArrayList<>();
        days.forEach(day -> sections.add(HistoryDisplaySection.forDay(day)));
        if (selectedType.equals("ALL") || selectedType.equals("FLASHCARD_REVIEW"))
            decks.forEach(deck -> sections.add(HistoryDisplaySection.forDeck(deck)));
        return sections.stream()
                .sorted(Comparator.comparing(this::sectionTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Instant sectionTimestamp(HistoryDisplaySection section) {
        if (section.deck() != null) return section.deck().latestOccurredAt();
        return section.day().events().stream().map(HistoryEvent::occurredAt)
                .filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }
}
