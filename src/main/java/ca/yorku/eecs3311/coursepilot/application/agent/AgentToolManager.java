package ca.yorku.eecs3311.coursepilot.application.agent;

import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanResponse;
import ca.yorku.eecs3311.coursepilot.application.service.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AgentToolManager {
    private static final Logger log = LoggerFactory.getLogger(AgentToolManager.class);
    public static final String SOURCES_MARKER = "\n\n[[STUDYFORGE_SOURCES]]\n";
    public static final String CONCEPTS_MARKER = "\n\n[[STUDYFORGE_CONCEPTS]]\n";
    public static final String ACTION_MARKER = "\n\n[[STUDYFORGE_ACTION|";
    private final ProgressService progress;
    private final LearningHistoryService history;
    private final RecommendationService recommendation;
    private final CourseMaterialService materials;
    private final CourseSearchService search;
    private final GroundedQuestionService groundedQuestions;
    private final FlashcardGenerationService flashcards;
    private final QuizGenerationService quizzes;
    private final SummarizationService summaries;
    private final StudyPlanService studyPlans;

    public AgentToolManager(ProgressService progress, LearningHistoryService history,
                            RecommendationService recommendation, CourseMaterialService materials,
                            CourseSearchService search, GroundedQuestionService groundedQuestions, FlashcardGenerationService flashcards,
                            QuizGenerationService quizzes, SummarizationService summaries,
                            StudyPlanService studyPlans) {
        this.progress = progress;
        this.history = history;
        this.recommendation = recommendation;
        this.materials = materials;
        this.search = search;
        this.groundedQuestions = groundedQuestions;
        this.flashcards = flashcards;
        this.quizzes = quizzes;
        this.summaries = summaries;
        this.studyPlans = studyPlans;
    }

    public AgentTool.ToolResult execute(AgentPlanStep step, Long courseId) {
        try {
            String output = switch (step.tool()) {
                case "COURSE_SEARCH" -> {
                    String searchQuery = step.parameters().getOrDefault("query", "");
                    var answer = groundedQuestions.ask(courseId, searchQuestion(searchQuery));
                    yield answer.answer() + groundingMetadata(answer.sources(), answer.keyConcepts());
                }
                case "ANSWER_COURSE_QUESTION" -> {
                    var answer = groundedQuestions.ask(courseId, step.parameters().getOrDefault("question", ""));
                    yield answer.answer() + groundingMetadata(answer.sources(), answer.keyConcepts());
                }
                case "GET_PROGRESS" -> formatProgress(progress.summarize(courseId));
                case "GET_WEAK_TOPICS" -> formatWeakTopics(progress.summarize(courseId));
                case "GET_HISTORY" -> {
                    String recentEvents = HistoryActivitySummarizer.summarize(history.list(courseId)).stream()
                            .limit(10)
                            .map(event -> "-  " + event.displayTimestamp() + " | " + event.displayType() + ": "
                                    + event.title() + " | " + shorten(event.detail(), 140))
                            .collect(Collectors.joining("\n"));
                    yield recentEvents.isBlank()
                            ? "No learning activity has been recorded for this course yet."
                            : recentEvents;
                }
                case "GET_RECOMMENDATION" -> {
                    var next = recommendation.recommend(courseId);
                    yield next.reason() + "\nNext: " + next.label();
                }
                case "GET_STUDY_PLAN" -> formatPlans(studyPlans.list(courseId));
                case "GENERATE_SUMMARY" -> {
                    String summaryRequest = step.parameters().getOrDefault("request", "");
                    var summary = findRequestedDocument(courseId, summaryRequest)
                            .map(document -> summaries.summarizeDocument(courseId, document.id()))
                            .orElseGet(() -> summaries.summarizeCourse(courseId));
                    String points = summary.keyPoints().stream().limit(5).map(point -> "-  " + point)
                            .collect(Collectors.joining("\n"));
                    yield summary.label() + "\n\n" + summary.summary() + (points.isBlank() ? "" : "\n\nKey points\n" + points)
                            + groundingMetadata(summary.sources(), List.of());
                }
                case "GENERATE_FLASHCARDS" -> {
                    String taskRequest = step.parameters().getOrDefault("request", "");
                    var document = findRequestedDocument(courseId, taskRequest)
                            .orElseThrow(() -> new IllegalArgumentException("Choose a ready course material first."));
                    int requestedCount = Integer.parseInt(step.parameters().getOrDefault("count", "0"));
                    if (requestedCount < 1 || requestedCount > 20)
                        throw new IllegalArgumentException("Choose between 1 and 20 flashcards.");
                    var result = flashcards.generate(courseId, document.id(), requestedCount, ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT);
                    yield "Created " + result.createdCount() + " of " + requestedCount + " requested flashcards from " + readableName(document.originalFilename())
                            + ". " + (result.reason().isBlank() ? "Review the document deck." : result.reason())
                            + ACTION_MARKER + "Review deck|/courses/" + courseId + "/flashcards/review?documentId=" + document.id() + "]]";
                }
                case "GENERATE_QUIZ" -> {
                    String taskRequest = step.parameters().getOrDefault("request", "");
                    var document = findRequestedDocument(courseId, taskRequest)
                            .orElseThrow(() -> new IllegalArgumentException("Choose a ready course material first."));
                    int requestedCount = Integer.parseInt(step.parameters().getOrDefault("count", "0"));
                    if (requestedCount < 1 || requestedCount > 20)
                        throw new IllegalArgumentException("Choose between 1 and 20 quiz questions.");
                    var result = quizzes.generate(courseId, document.id(), requestedCount, Set.of(),
                            ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT);
                    yield "Created a quiz with " + result.createdCount() + " questions from "
                            + readableName(document.originalFilename()) + "." + ACTION_MARKER + "Take quiz|/courses/" + courseId
                            + "/quizzes/" + result.quiz().id() + "]]";
                }
                case "CREATE_STUDY_PLAN" -> {
                    StudyPlanResponse plan = studyPlans.create(courseId, LocalDate.now(), LocalDate.now().plusDays(6),
                            60, "WEAK_TOPIC_FOCUSED");
                    yield "Created a 60-minute weak-topic study plan with " + plan.items().size()
                            + " sessions. Open Insights / Study Plan to review the schedule.";
                }
                default -> throw new IllegalArgumentException("That study action is not available yet.");
            };
            return new AgentTool.ToolResult(step.tool(), true, output.isBlank() ? "No matching course information was found." : output);
        } catch (RuntimeException exception) {
            String userMessage = exception instanceof InsightGenerationException || exception instanceof IllegalArgumentException
                    ? Objects.toString(exception.getMessage(), "The request could not be completed.")
                    : "The study service is temporarily unavailable. Please try again or check AI status.";
            log.warn("Assistant task failed: tool={}, exception={}", step.tool(), exception.getClass().getSimpleName());
            return new AgentTool.ToolResult(step.tool(), false, userMessage);
        }
    }

    private String formatProgress(ProgressSummary summary) {
        String mastery = summary.hasActivity()
                ? String.format(Locale.ROOT, "%.0f%%  |  %s", summary.overallScore(), summary.overallStatus().toLowerCase(Locale.ROOT))
                : "Not started";
        return "Course mastery: " + mastery + "\nLearning activities recorded: " + summary.activityCount();
    }

    private String formatWeakTopics(ProgressSummary summary) {
        List<ProgressSummary.TopicProgress> needsReview = summary.topics().stream()
                .filter(topic -> !"Quiz performance".equalsIgnoreCase(topic.topic()))
                .filter(topic -> !"STRONG".equalsIgnoreCase(topic.status())).toList();
        if (needsReview.isEmpty()) return "No weak or developing course areas are recorded yet.";
        return "These tracked course areas may need review.\n" + needsReview.stream().limit(5)
                .map(topic -> "-  " + readableName(topic.topic()) + "  -  " + Math.round(topic.score()) + "%  |  "
                        + topic.status().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("\n"));
    }

    private String formatPlans(List<StudyPlanResponse> plans) {
        if (plans.isEmpty()) return "There are no saved study plans for this course yet.";
        return plans.stream().limit(5).map(plan -> "-  " + readableName(plan.strategy()) + " plan  |  "
                        + plan.startDate() + " to " + plan.targetDate() + "  |  " + plan.availableMinutes() + " minutes  |  "
                        + plan.items().size() + " sessions")
                .collect(Collectors.joining("\n"));
    }

    private DocumentResponse readyDocument(Long courseId) {
        return materials.listDocuments(courseId).stream()
                .filter(document -> "READY".equals(document.processingStatus())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No ready course material is available."));
    }

    private java.util.Optional<DocumentResponse> findRequestedDocument(Long courseId, String request) {
        String normalizedRequest = normalizeForMatch(request);
        if (normalizedRequest.isBlank() || containsWholeWord(normalizedRequest, "all")
                || containsWholeWord(normalizedRequest, "every") || normalizedRequest.contains("course wide")
                || normalizedRequest.contains("entire course")) return java.util.Optional.empty();
        List<DocumentResponse> matches = materials.listDocuments(courseId).stream()
                .filter(document -> "READY".equals(document.processingStatus()))
                .filter(document -> {
                    String fullName = normalizeForMatch(document.originalFilename());
                    String stem = normalizeForMatch(document.originalFilename().replaceFirst("(?i)\\.[a-z0-9]+$", ""));
                    return (!fullName.isBlank() && normalizedRequest.contains(fullName))
                            || (!stem.isBlank() && normalizedRequest.contains(stem));
                }).toList();
        return matches.size() == 1 ? java.util.Optional.of(matches.getFirst()) : java.util.Optional.empty();
    }

    private String normalizeForMatch(String text) {
        return Objects.toString(text, "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private boolean containsWholeWord(String text, String word) {
        return java.util.Arrays.asList(text.split("\\s+")).contains(word);
    }

    private String pageLabel(Integer start, Integer end) {
        if (start == null) return "";
        return end == null || start.equals(end) ? "  |  p. " + start : "  |  pp. " + start + "-" + end;
    }

    private String searchQuestion(String query) {
        String topic = Objects.toString(query, "").trim()
                .replaceFirst("(?i)^(please\\s+)?(find|search|look\\s+for)\\s+(course\\s+)?(materials?|evidence|passages?)\\s*(about|on|for)?\\s*", "")
                .replaceFirst("(?i)^(please\\s+)?(find|search|look\\s+for)\\s+", "").trim();
        return topic.isBlank() ? query : "Explain the relevant course concepts about " + topic + ".";
    }

    private String groundingMetadata(List<ca.yorku.eecs3311.coursepilot.application.dto.GroundedSource> sources,
                                    List<String> concepts) {
        String conceptBlock = concepts == null || concepts.isEmpty() ? ""
                : CONCEPTS_MARKER + concepts.stream().limit(5).map(String::trim)
                .filter(value -> !value.isBlank()).collect(Collectors.joining("\n"));
        String sourceBlock = sources == null || sources.isEmpty() ? ""
                : SOURCES_MARKER + sources.stream().map(source -> readableName(source.documentName())
                        + pageLabel(source.pageStart(), source.pageEnd())).distinct().collect(Collectors.joining("\n"));
        return conceptBlock + sourceBlock;
    }

    private String readableName(String name) {
        if (name == null || name.isBlank()) return "Course material";
        return name.replaceFirst("(?i)\\.(pdf|txt|md)$", "").replace('_', ' ');
    }

    private String shorten(String text, int maximum) {
        String value = Objects.toString(text, "").replaceAll("\\s+", " ").trim();
        return value.length() <= maximum ? value : value.substring(0, maximum - 1) + "...";
    }
}
