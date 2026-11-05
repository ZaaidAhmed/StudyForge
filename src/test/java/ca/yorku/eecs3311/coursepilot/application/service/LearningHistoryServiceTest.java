package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.*;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LearningHistoryServiceTest {
    private final FlashcardReviewRepository reviews = mock(FlashcardReviewRepository.class);
    private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
    private final StudyPlanRepository plans = mock(StudyPlanRepository.class);
    private final LearningHistoryService service = new LearningHistoryService(reviews, attempts, plans);

    @Test
    void combinesCourseScopedEventsInReverseChronologicalOrder() {
        var flashcard = mock(Flashcard.class);
        when(flashcard.getQuestion()).thenReturn("What is cohesion?");
        var review = mock(FlashcardReview.class);
        when(review.getFlashcard()).thenReturn(flashcard);
        when(review.getRating()).thenReturn(FlashcardReviewRating.GOOD);
        when(review.getReviewedAt()).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));
        var attempt = mock(QuizAttempt.class);
        when(attempt.getCompletedAt()).thenReturn(Instant.parse("2026-01-02T10:00:00Z"));
        when(attempt.getCorrectCount()).thenReturn(4);
        when(attempt.getTotalCount()).thenReturn(5);
        var quiz = mock(Quiz.class);
        when(quiz.getTitle()).thenReturn("Design quiz");
        when(attempt.getQuiz()).thenReturn(quiz);
        when(reviews.findByCourseIdOrderByIdAsc(7L)).thenReturn(List.of(review));
        when(attempts.findByCourseId(7L)).thenReturn(List.of(attempt));
        when(plans.findByCourseId(7L)).thenReturn(List.of());

        var events = service.list(7L);
        assertThat(events).extracting("type").containsExactly("QUIZ_ATTEMPT", "FLASHCARD_REVIEW");
        verify(reviews).findByCourseIdOrderByIdAsc(7L);
        verify(attempts).findByCourseId(7L);
    }

    @Test
    void appliesBoundedRetentionAndRecentLimit() {
        var events = java.util.stream.IntStream.range(0, 120).mapToObj(i -> {
            var r = mock(FlashcardReview.class);
            var card = mock(Flashcard.class);
            when(card.getQuestion()).thenReturn("Q" + i);
            when(r.getFlashcard()).thenReturn(card);
            when(r.getRating()).thenReturn(FlashcardReviewRating.HARD);
            when(r.getReviewedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i));
            return r;
        }).toList();
        when(reviews.findByCourseIdOrderByIdAsc(1L)).thenReturn(events);
        when(attempts.findByCourseId(1L)).thenReturn(List.of());
        when(plans.findByCourseId(1L)).thenReturn(List.of());
        assertThat(service.list(1L)).hasSize(100);
        assertThat(service.recent(1L, 500)).hasSize(100);
        assertThat(service.recent(1L, 2)).hasSize(2);
    }

    @Test
    void sortsEqualAndMissingTimestampsDeterministically() {
        Instant sameTime = Instant.parse("2026-01-02T10:00:00Z");
        var quizAttempt = mock(QuizAttempt.class);
        when(quizAttempt.getCompletedAt()).thenReturn(sameTime);
        when(quizAttempt.getCorrectCount()).thenReturn(1);
        when(quizAttempt.getTotalCount()).thenReturn(2);
        var quiz = mock(Quiz.class);
        when(quiz.getTitle()).thenReturn("Quiz");
        when(quizAttempt.getQuiz()).thenReturn(quiz);

        var untimedReview = mock(FlashcardReview.class);
        when(untimedReview.getReviewedAt()).thenReturn(null);
        when(untimedReview.getRating()).thenReturn(FlashcardReviewRating.GOOD);
        var card = mock(Flashcard.class);
        when(card.getQuestion()).thenReturn("Question");
        when(untimedReview.getFlashcard()).thenReturn(card);

        when(reviews.findByCourseIdOrderByIdAsc(4L)).thenReturn(List.of(untimedReview));
        when(attempts.findByCourseId(4L)).thenReturn(List.of(quizAttempt));
        when(plans.findByCourseId(4L)).thenReturn(List.of());

        var events = service.list(4L);
        assertThat(events).extracting(HistoryEvent::type).containsExactly("QUIZ_ATTEMPT", "FLASHCARD_REVIEW");
        assertThat(events.getLast().occurredAt()).isNull();
        verify(reviews).findByCourseIdOrderByIdAsc(4L);
        verify(attempts).findByCourseId(4L);
        verify(plans).findByCourseId(4L);
    }

    @Test
    void includesStudyPlanCreationAsOneCourseScopedEvent() {
        Instant createdAt = Instant.parse("2026-02-03T09:15:00Z");
        var plan = mock(StudyPlan.class);
        when(plan.getCreatedAt()).thenReturn(createdAt);
        when(plan.getStrategy()).thenReturn("BALANCED");
        when(plan.getStartDate()).thenReturn(LocalDate.of(2026, 2, 3));
        when(plan.getTargetDate()).thenReturn(LocalDate.of(2026, 2, 10));
        when(plan.getItems()).thenReturn(List.of(mock(StudyPlanItem.class), mock(StudyPlanItem.class)));
        when(reviews.findByCourseIdOrderByIdAsc(8L)).thenReturn(List.of());
        when(attempts.findByCourseId(8L)).thenReturn(List.of());
        when(plans.findByCourseId(8L)).thenReturn(List.of(plan));

        var events = service.list(8L);

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo("STUDY_PLAN");
            assertThat(event.title()).isEqualTo("Study plan created");
            assertThat(event.primaryDetail()).isEqualTo("Balanced");
            assertThat(event.secondaryDetail()).isEqualTo("Feb 3, 2026 to Feb 10, 2026 · 0 min planned · 2 sessions");
            assertThat(event.actionLabel()).isEqualTo("Open study plans");
            assertThat(event.actionUrl()).isEqualTo("/courses/8/study-plan");
        });
        verify(plans).findByCourseId(8L);
    }

    @Test
    void presentsFriendlyLabelsAndHandlesMissingTime() {
        var event = new HistoryEvent(null, "QUIZ_ATTEMPT", "Quiz completed", "4 / 5 correct");

        assertThat(event.displayType()).isEqualTo("Quiz attempt");
        assertThat(event.styleKey()).isEqualTo("quiz");
        assertThat(event.displayTimestamp()).isEqualTo("Date unavailable");
    }

    @Test
    void buildsEventSpecificDetailsAndCourseScopedDestinations() {
        var course = mock(Course.class);
        when(course.getId()).thenReturn(9L);
        var document = mock(CourseDocument.class);
        when(document.getId()).thenReturn(47L);
        when(document.getOriginalFilename()).thenReturn("Threads.pdf");
        when(document.getCourse()).thenReturn(course);
        var source = mock(FlashcardSource.class);
        when(source.getDocument()).thenReturn(document);
        var card = mock(Flashcard.class);
        when(card.getQuestion()).thenReturn("What is a thread?");
        when(card.getSources()).thenReturn(List.of(source));
        var review = mock(FlashcardReview.class);
        when(review.getFlashcard()).thenReturn(card);
        when(review.getRating()).thenReturn(FlashcardReviewRating.EASY);
        when(review.getReviewedAt()).thenReturn(Instant.parse("2026-02-01T10:00:00Z"));

        var quiz = mock(Quiz.class);
        when(quiz.getId()).thenReturn(81L);
        when(quiz.getTitle()).thenReturn("Threads quiz");
        var attempt = mock(QuizAttempt.class);
        when(attempt.getQuiz()).thenReturn(quiz);
        when(attempt.getCompletedAt()).thenReturn(Instant.parse("2026-02-02T10:00:00Z"));
        when(attempt.getCorrectCount()).thenReturn(7);
        when(attempt.getTotalCount()).thenReturn(10);

        var itemA = mock(StudyPlanItem.class);
        var itemB = mock(StudyPlanItem.class);
        when(itemA.getMinutes()).thenReturn(25);
        when(itemB.getMinutes()).thenReturn(35);
        var plan = mock(StudyPlan.class);
        when(plan.getCreatedAt()).thenReturn(Instant.parse("2026-02-03T10:00:00Z"));
        when(plan.getStrategy()).thenReturn("WEAK_TOPIC_FOCUSED");
        when(plan.getStartDate()).thenReturn(LocalDate.of(2026, 2, 3));
        when(plan.getTargetDate()).thenReturn(LocalDate.of(2026, 2, 17));
        when(plan.getItems()).thenReturn(List.of(itemA, itemB));

        when(reviews.findByCourseIdOrderByIdAsc(9L)).thenReturn(List.of(review));
        when(attempts.findByCourseId(9L)).thenReturn(List.of(attempt));
        when(plans.findByCourseId(9L)).thenReturn(List.of(plan));

        var events = service.list(9L);
        var flashcardEvent = events.get(2);
        var quizEvent = events.get(1);
        var planEvent = events.get(0);

        assertThat(flashcardEvent.primaryDetail()).isEqualTo("What is a thread?");
        assertThat(flashcardEvent.secondaryDetail()).isEqualTo("Rated Easy · Threads.pdf");
        assertThat(flashcardEvent.actionLabel()).isEqualTo("Review deck");
        assertThat(flashcardEvent.actionUrl()).isEqualTo("/courses/9/flashcards/review?documentId=47");
        assertThat(quizEvent.primaryDetail()).isEqualTo("7 of 10 correct");
        assertThat(quizEvent.secondaryDetail()).isEqualTo("Threads quiz");
        assertThat(quizEvent.actionUrl()).isEqualTo("/courses/9/quizzes/81/attempts");
        assertThat(planEvent.primaryDetail()).isEqualTo("Weak-topic focused");
        assertThat(planEvent.secondaryDetail()).isEqualTo("Feb 3, 2026 to Feb 17, 2026 · 60 min planned · 2 sessions");
        assertThat(planEvent.actionUrl()).isEqualTo("/courses/9/study-plan");
    }

    @Test
    void usesCourseWideFlashcardReviewWhenNoOwnedDocumentSourceExists() {
        var card = mock(Flashcard.class);
        when(card.getQuestion()).thenReturn("What is scheduling?");
        when(card.getSources()).thenReturn(List.of());
        var review = mock(FlashcardReview.class);
        when(review.getFlashcard()).thenReturn(card);
        when(review.getRating()).thenReturn(FlashcardReviewRating.GOOD);
        when(reviews.findByCourseIdOrderByIdAsc(5L)).thenReturn(List.of(review));
        when(attempts.findByCourseId(5L)).thenReturn(List.of());
        when(plans.findByCourseId(5L)).thenReturn(List.of());

        assertThat(service.list(5L)).singleElement().satisfies(event -> {
            assertThat(event.secondaryDetail()).isEqualTo("Rated Good");
            assertThat(event.actionUrl()).isEqualTo("/courses/5/flashcards/review");
        });
    }
}
