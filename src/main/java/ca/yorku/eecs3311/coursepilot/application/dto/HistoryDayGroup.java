package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record HistoryDayGroup(String dateLabel, List<HistoryEvent> events) {
    public HistoryDayGroup {
        events = List.copyOf(events);
    }
}
