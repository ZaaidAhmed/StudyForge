package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardReviewRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.QuizAttemptRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.StudyPlanRepository;
import ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReviewRating;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class LearningHistoryService {
    private static final int MAX_HISTORY_EVENTS = 100;
    private static final DateTimeFormatter PLAN_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private final FlashcardReviewRepository reviews;
    private final QuizAttemptRepository attempts;
    private final StudyPlanRepository plans;

    public LearningHistoryService(FlashcardReviewRepository reviews, QuizAttemptRepository attempts,
                                  StudyPlanRepository plans) {
        this.reviews = reviews;
        this.attempts = attempts;
        this.plans = plans;
    }

    @Transactional(readOnly = true)
    public List<HistoryEvent> list(Long courseId) {
        List<HistoryEvent> events = new ArrayList<>();
        for (var review : reviews.findByCourseIdOrderByIdAsc(courseId)) {
            var card = review.getFlashcard();
            String question = card.getQuestion();
            String rating = displayRating(review.getRating());
            var document = card.getSources().stream()
                    .map(source -> source.getDocument())
                    .filter(sourceDocument -> sourceDocument.getCourse() != null
                            && courseId.equals(sourceDocument.getCourse().getId()))
                    .filter(sourceDocument -> sourceDocument.getId() != null)
                    .findFirst().orElse(null);
            String documentName = document == null ? null : document.getOriginalFilename();
            String context = documentName == null || documentName.isBlank()
                    ? "Rated " + rating
                    : "Rated " + rating + " · " + documentName;
            String detail = "Rated " + rating + ": " + question
                    + (documentName == null || documentName.isBlank() ? "" : " · " + documentName);
            String reviewUrl = "/courses/" + courseId + "/flashcards/review"
                    + (document == null ? "" : "?documentId=" + document.getId());
            events.add(new HistoryEvent(review.getReviewedAt(), "FLASHCARD_REVIEW", "Flashcard reviewed",
                    detail, question, context, "Review deck", reviewUrl,
                    documentName == null || documentName.isBlank() ? "Unlinked flashcards" : documentName));
        }
        for (var attempt : attempts.findByCourseId(courseId)) {
            var quiz = attempt.getQuiz();
            String quizTitle = quiz.getTitle();
            String score = attempt.getCorrectCount() + " of " + attempt.getTotalCount() + " correct";
            Long quizId = quiz.getId();
            events.add(new HistoryEvent(
                    attempt.getCompletedAt() == null ? attempt.getStartedAt() : attempt.getCompletedAt(),
                    "QUIZ_ATTEMPT", "Quiz completed",
                    score + " · " + quizTitle, score, quizTitle,
                    quizId == null ? null : "Review attempts",
                    quizId == null ? null : "/courses/" + courseId + "/quizzes/" + quizId + "/attempts"));
        }
        for (var plan : plans.findByCourseId(courseId)) {
            int sessionCount = plan.getItems().size();
            int scheduledMinutes = plan.getItems().stream().mapToInt(item -> Math.max(0, item.getMinutes())).sum();
            String strategy = displayStrategy(plan.getStrategy());
            String dateRange = PLAN_DATE_FORMAT.format(plan.getStartDate()) + " to "
                    + PLAN_DATE_FORMAT.format(plan.getTargetDate());
            String planSummary = dateRange + " · " + scheduledMinutes + " min planned · "
                    + sessionCount + (sessionCount == 1 ? " session" : " sessions");
            events.add(new HistoryEvent(plan.getCreatedAt(), "STUDY_PLAN", "Study plan created",
                    strategy + " · " + planSummary, strategy, planSummary,
                    "Open study plans", "/courses/" + courseId + "/study-plan"));
        }

        return events.stream()
                .sorted(Comparator.comparing(HistoryEvent::occurredAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HistoryEvent::type, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HistoryEvent::title, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HistoryEvent::detail, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_HISTORY_EVENTS)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryEvent> recent(Long courseId, int limit) {
        return list(courseId).stream().limit(Math.max(1, Math.min(limit, MAX_HISTORY_EVENTS))).toList();
    }

    private String displayRating(FlashcardReviewRating rating) {
        if (rating == null) return "Reviewed";
        return switch (rating) {
            case INCORRECT -> "Needs practice";
            case HARD -> "Hard";
            case GOOD -> "Good";
            case EASY -> "Easy";
        };
    }

    private String displayStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) return "Study plan";
        return switch (strategy.trim().toUpperCase(Locale.ROOT)) {
            case "BALANCED" -> "Balanced";
            case "WEAK_TOPIC_FOCUSED" -> "Weak-topic focused";
            case "EXAM_CRAM", "EXAM_FOCUSED" -> "Exam focused";
            default -> strategy.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }
}
