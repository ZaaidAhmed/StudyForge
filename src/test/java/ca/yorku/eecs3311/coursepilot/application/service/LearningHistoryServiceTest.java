package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
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
}
