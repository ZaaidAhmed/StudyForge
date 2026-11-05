package ca.yorku.eecs3311.coursepilot.application.dto;

import java.time.Instant;
import java.util.List;

/** A short-lived, course-scoped assistant interaction stored in the current web session. */
public record AgentChatEntry(String prompt, String result, Instant createdAt,
                             List<String> keyConcepts, List<String> sources,
                             String actionLabel, String actionUrl, List<HistoryEvent> historyEvents) {
    public AgentChatEntry {
        keyConcepts = List.copyOf(keyConcepts == null ? List.of() : keyConcepts);
        sources = List.copyOf(sources == null ? List.of() : sources);
        historyEvents = List.copyOf(historyEvents == null ? List.of() : historyEvents);
    }

    public AgentChatEntry(String prompt, String result, Instant createdAt,
                          List<String> keyConcepts, List<String> sources,
                          String actionLabel, String actionUrl) {
        this(prompt, result, createdAt, keyConcepts, sources, actionLabel, actionUrl, List.of());
    }

    public AgentChatEntry(String prompt, String result, Instant createdAt) {
        this(prompt, result, createdAt, List.of(), List.of(), "", "", List.of());
    }

    public AgentChatEntry(String prompt, String result, Instant createdAt,
                          List<String> keyConcepts, List<String> sources) {
        this(prompt, result, createdAt, keyConcepts, sources, "", "", List.of());
    }
}
