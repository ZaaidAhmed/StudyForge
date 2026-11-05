package ca.yorku.eecs3311.coursepilot.application.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TaskPlanner {
    public AgentPlan plan(AgentIntent intent) { return plan(intent, ""); }

    public AgentPlan plan(AgentIntent intent, String request) {
        return switch (intent) {
            case WEAK_TOPIC_SUPPORT -> new AgentPlan(intent, List.of(
                    new AgentPlanStep("GET_PROGRESS", Map.of()),
                    new AgentPlanStep("GET_WEAK_TOPICS", Map.of())));
            case HISTORY_REVIEW -> new AgentPlan(intent, List.of(new AgentPlanStep("GET_HISTORY", Map.of())));
            case NEXT_STEP -> new AgentPlan(intent, List.of(
                    new AgentPlanStep("GET_PROGRESS", Map.of()),
                    new AgentPlanStep("GET_RECOMMENDATION", Map.of())));
            case COURSE_SEARCH -> new AgentPlan(intent, List.of(new AgentPlanStep("COURSE_SEARCH",
                    Map.of("query", Objects.toString(request, "").trim()))));
            case COURSE_QUESTION -> new AgentPlan(intent, List.of(new AgentPlanStep("ANSWER_COURSE_QUESTION",
                    Map.of("question", Objects.toString(request, "").trim()))));
            case SUMMARY_GENERATION -> new AgentPlan(intent, List.of(new AgentPlanStep("GENERATE_SUMMARY",
                    Map.of("request", Objects.toString(request, "").trim()))));
            case FLASHCARD_GENERATION -> {
                String count = java.util.regex.Pattern.compile("\\b(\\d{1,2})\\b")
                        .matcher(Objects.toString(request, "")).results()
                        .map(match -> match.group(1)).findFirst().orElse("");
                yield new AgentPlan(intent, List.of(new AgentPlanStep("GENERATE_FLASHCARDS",
                        Map.of("count", count, "request", Objects.toString(request, "")))));
            }
            case QUIZ_GENERATION -> {
                String count = java.util.regex.Pattern.compile("(?i)\\b(\\d{1,2})\\s*[- ]?\\s*(?:quiz(?:zes)?|questions?)\\b|\\b(?:quiz(?:zes)?|questions?)\\D{0,12}(\\d{1,2})\\b")
                        .matcher(Objects.toString(request, "")).results()
                        .map(match -> match.group(1) == null ? match.group(2) : match.group(1)).findFirst().orElse("");
                yield new AgentPlan(intent, List.of(new AgentPlanStep("GENERATE_QUIZ",
                        Map.of("count", count, "request", Objects.toString(request, "")))));
            }
            case STUDY_PLAN_CREATION -> new AgentPlan(intent, List.of(new AgentPlanStep("CREATE_STUDY_PLAN", Map.of())));
            case PROGRESS_REVIEW -> new AgentPlan(intent, List.of(new AgentPlanStep("GET_PROGRESS", Map.of())));
            case UNSUPPORTED_REQUEST -> new AgentPlan(intent, List.of());
        };
    }
}
