package ca.yorku.eecs3311.coursepilot.application.agent;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class IntentAnalyzer {
    private static final Pattern DESTRUCTIVE_ACTION = Pattern.compile(
            "\\b(delete|remove|erase|clear|discard|wipe|trash)\\b", Pattern.CASE_INSENSITIVE);
    public AgentIntent analyze(String request) {
        String text = Objects.toString(request, "").trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) return AgentIntent.PROGRESS_REVIEW;

        // Resource deletion is not an agent capability. Check before keyword matching so a
        // request such as "delete my flashcards" can never trigger flashcard generation.
        if (DESTRUCTIVE_ACTION.matcher(text).find()) return AgentIntent.UNSUPPORTED_REQUEST;

        if (containsAny(text, "study plan", "schedule") && containsAny(text, "create", "build", "make", "generate", "plan my"))
            return AgentIntent.STUDY_PLAN_CREATION;
        if ((containsAny(text, "quiz", "test me") && containsAny(text, "create", "build", "make", "generate", "quiz me", "test me")))
            return AgentIntent.QUIZ_GENERATION;
        if ((containsAny(text, "flashcard", "flash card") && containsAny(text, "create", "build", "make", "generate")))
            return AgentIntent.FLASHCARD_GENERATION;
        if (text.contains("summar")) return AgentIntent.SUMMARY_GENERATION;
        if (containsAny(text, "weak", "struggl", "need to review", "should i review"))
            return AgentIntent.WEAK_TOPIC_SUPPORT;
        if (containsAny(text, "history", "recent activity", "recent learning")) return AgentIntent.HISTORY_REVIEW;
        if (containsAny(text, "search", "find ")) return AgentIntent.COURSE_SEARCH;
        if (containsAny(text, "next", "recommend", "focus on")) return AgentIntent.NEXT_STEP;
        if (containsAny(text, "progress", "performance", "how am i doing", "how am i performing"))
            return AgentIntent.PROGRESS_REVIEW;
        // Free-form prompts default to grounded course Q&A. Returning course progress for an
        // unrelated subject question creates a confident but plainly irrelevant answer.
        return AgentIntent.COURSE_QUESTION;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }
}
