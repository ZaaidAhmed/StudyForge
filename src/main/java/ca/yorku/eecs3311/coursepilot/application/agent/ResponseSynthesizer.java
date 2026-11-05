package ca.yorku.eecs3311.coursepilot.application.agent;

import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ResponseSynthesizer {
    public String synthesize(AgentIntent intent, List<AgentTool.ToolResult> results, List<HistoryEvent> memory) {
        if (intent == AgentIntent.UNSUPPORTED_REQUEST) {
            return "I can't delete saved resources from this chat. Open Practice / Flashcards or Practice / Quizzes to manage them.";
        }
        String details = results.stream()
                .map(result -> java.util.Objects.toString(result.output(), ""))
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n\n"));
        if (details.isBlank()) return "I couldn't find a useful result for that request. Try another course task.";
        return switch (intent) {
            case PROGRESS_REVIEW -> withRecentContext(
                    "Here is your course progress.\n\n" + details, memory);
            case WEAK_TOPIC_SUPPORT -> withRecentContext(
                    "These course areas may need more review.\n\n" + details, memory);
            case HISTORY_REVIEW -> historyResponse(details, memory);
            case NEXT_STEP -> withRecentContext(
                    "Here is a suggested next step based on your course activity.\n\n" + details, memory);
            case COURSE_SEARCH, COURSE_QUESTION -> details;
            case SUMMARY_GENERATION -> details;
            case FLASHCARD_GENERATION, QUIZ_GENERATION, STUDY_PLAN_CREATION -> details;
            case UNSUPPORTED_REQUEST -> details;
        };
    }

    private String historyResponse(String toolDetails, List<HistoryEvent> memory) {
        if (memory.isEmpty()) return "Here is your recent learning activity.\n\n" + toolDetails;
        String recent = memory.stream().limit(10)
                .map(event -> "-  " + event.displayTimestamp() + " | " + event.displayType()
                        + ": " + event.title() + " | " + shorten(event.detail(), 140))
                .collect(Collectors.joining("\n"));
        return "Here is your recent learning activity.\n\n" + recent;
    }

    private String withRecentContext(String response, List<HistoryEvent> memory) {
        if (memory.isEmpty()) return response;
        String recent = memory.stream().limit(3)
                .map(event -> event.displayType() + ": " + event.title() + " (" + shorten(event.detail(), 120) + ")")
                .collect(Collectors.joining("\n"));
        return response + "\n\nRecent course activity considered:\n" + recent;
    }

    private String shorten(String value, int maxLength) {
        if (value == null) return "";
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxLength ? compact : compact.substring(0, maxLength - 3) + "...";
    }
}
