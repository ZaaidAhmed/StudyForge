package ca.yorku.eecs3311.coursepilot.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class TemplateSafetyTest {
    @Test
    void templatesDoNotUseUnescapedThymeleafOutput() throws Exception {
        Path templates = Path.of("src/main/resources/templates");
        try (var paths = Files.walk(templates)) {
            var unsafe = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html"))
                    .filter(path -> {
                        try { return Files.readString(path).contains("th:utext"); }
                        catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
                    })
                    .toList();
            assertThat(unsafe).as("templates with unescaped dynamic output").isEmpty();
        }
    }

    @Test
    void coreStudySurfacesExposeRecoveryAndContextGuidance() throws Exception {
        String materials = Files.readString(Path.of("src/main/resources/templates/materials/list.html"));
        String ask = Files.readString(Path.of("src/main/resources/templates/ask/form.html"));
        assertThat(materials).contains("Upload and process", "Search materials", "Ask this course", "No materials uploaded yet");
        assertThat(ask).contains("Search materials", "Upload or search material", "Check AI status", "aria-live");
    }

    @Test
    void generatedLearningToolsExposeReadyFilteringAndDuplicateSubmitProtection() throws Exception {
        String flashcards = Files.readString(Path.of("src/main/resources/templates/flashcards/index.html"));
        String quizzes = Files.readString(Path.of("src/main/resources/templates/quizzes/index.html"));
        assertThat(flashcards).contains("processingStatus == 'READY'", "data-progress-operation=\"flashcards\"", "Upload material");
        assertThat(quizzes).contains("processingStatus() == 'READY'", "data-progress-operation=\"quiz\"", "Upload material");
        String progress = Files.readString(Path.of("src/main/resources/static/js/generation-progress.js"));
        assertThat(progress).contains("button.disabled = true", "data-progress-percent", "data-progress-bar");
    }

    @Test
    void quizTakingRendersControlsForEverySupportedQuestionType() throws Exception {
        String quiz = Files.readString(Path.of("src/main/resources/templates/quizzes/take.html"));
        assertThat(quiz).contains("question.type().name() == 'MULTIPLE_CHOICE'",
                "question.type().name() == 'TRUE_FALSE'", "value=\"true\"", "value=\"false\"")
                .contains("Correct answer:", "Explain this answer", "aria-live=\"polite\"")
                .contains("row.feedback().explanation()", "row.feedback().correctAnswer()")
                .doesNotContain("Grounded explanation", "row.explanation()", "sourceId()", "snippet()")
                .doesNotContain("Next question", "Previous", "Skip for now", "currentQuestion");
    }

    @Test
    void quizGenerationExposesLoadingProgressWithoutDurationDetails() throws Exception {
        String quiz = Files.readString(Path.of("src/main/resources/templates/quizzes/index.html"));
        assertThat(quiz).contains("quiz-generation-progress", "quiz-progress-bar", "aria-label=\"Quiz generation progress\"",
                "Generating grounded questions");
        assertThat(quiz).doesNotContain("Remaining:", "quiz-progress-elapsed", "exact token progress is not available", "Elapsed:");
    }

    @Test
    void flashcardPracticeUsesRevealNavigationAndThreeSelfRatings() throws Exception {
        String review = Files.readString(Path.of("src/main/resources/templates/flashcards/review.html"));
        String deck = Files.readString(Path.of("src/main/resources/templates/flashcards/index.html"));
        assertThat(review).contains("data-reveal", "data-previous", "data-next", "data-progress-fill",
                "requestSubmit(rateButton)", "value=\"HARD\"", "value=\"GOOD\"", "value=\"EASY\"",
                "Learning", "Familiar", "Mastered", "Practice complete",
                "Click or press Space to show the question", "card.querySelectorAll('[data-reveal]')");
        assertThat(deck).doesNotContain("${card.answer}");
    }

    @Test
    void implementedLearningFeaturesAreNotPresentedAsPlanned() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/fragments/layout.html"));
        assertThat(layout).contains("Study", "/concepts", "/study-plan", "/progress", "/recommendation", "/history", "/agent");
        assertThat(layout).doesNotContain("Planned features", "planned-link", "nav-status");
    }

    @Test
    void aiQuestionSurfaceExposesAnimatedAccessibleProgressState() throws Exception {
        String ask = Files.readString(Path.of("src/main/resources/templates/ask/form.html"));
        String css = Files.readString(Path.of("src/main/resources/static/css/ui-ux-contract.css"));
        assertThat(ask).contains("ai-progress", "ai-progress-spinner", "data-progress-operation=\"question\"", "aria-live",
                "data-progress-percent", "data-progress-bar");
        assertThat(css).contains("studyforge-pulse", "studyforge-dots", "prefers-reduced-motion");
    }

    @Test
    void studyStudioKeepsOptionalPanelsHiddenUntilSelected() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/static/css/study-studio.css"));
        assertThat(css).contains(".studio-tool-panel[hidden]{display:none!important}",
                ".studio-tutor-pane>.studio-tool-panel:not([hidden]) .studio-chat-form");
    }

    @Test
    void courseAssistantUsesChatFirstLayoutAndAccessibleIcons() throws Exception {
        String agent = Files.readString(Path.of("src/main/resources/templates/agent/index.html"));
        String css = Files.readString(Path.of("src/main/resources/static/css/agent-workspace.css"));
        assertThat(agent).contains("StudyForge assistant", "Course conversation", "agent-onboarding",
                "agent-suggestions", "agent-rerun-form", "agent-material-scope", "agent-mini-metrics",
                "<symbol id=\"agent-book\"", "<symbol id=\"agent-quiz\"", "<symbol id=\"agent-send\"",
                "th:text=\"${message.prompt}\"", "th:text=\"${message.result}\"", "aria-live=\"polite\"")
                .doesNotContain("agent-hero-art", "agent-quick-actions", "agent-snapshot", "Recent AI tasks",
                        "Study streak", "th:utext", "aria-hidden=\"true\">DOC", "aria-hidden=\"true\">CARD",
                        "aria-hidden=\"true\">PLAN", "aria-hidden=\"true\">PROG");
        assertThat(css).contains("grid-template-columns:minmax(0,1fr) 310px", "@media(max-width:850px)",
                "@media(max-width:560px)", "prefers-reduced-motion", ":focus-visible");
    }

    @Test
    void learningHistoryUsesEscapedTimelineAndClearEventMetadata() throws Exception {
        String history = Files.readString(Path.of("src/main/resources/templates/history/index.html"));
        String css = Files.readString(Path.of("src/main/resources/static/css/history.css"));
        assertThat(history).contains("Learning history", "history-timeline", "historySections", "day.dateLabel()",
                "event.displayType()", "event.displayTimestamp()", "event.primaryDetail()", "event.secondaryDetail()",
                "event.actionLabel()", "event.actionUrl()", "event.detail",
                "flashcardReviewCount", "quizAttemptCount", "studyPlanCount", "No matching activity", "No learning history yet",
                "@{/courses/{id}(id=${course.id})}", "@{/courses/{id}/practice(id=${course.id})}")
                .doesNotContain("th:utext", "event.type}", "Â·", "â€œ");
        assertThat(css).contains(".history-timeline-list::before", ".history-summary", ".history-filter-empty", ".history-empty", ".history-event-action", "@media (max-width: 620px)",
                "prefers-reduced-motion", ".history-event--flashcard", ".history-event--quiz", ".history-event--plan");
    }
}
