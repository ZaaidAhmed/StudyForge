package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProgressServiceTest {
    private final CourseService courses = mock(CourseService.class);
    private final FlashcardReviewRepository reviews = mock(FlashcardReviewRepository.class);
    private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
    private final ProgressService service = new ProgressService(courses, reviews, attempts);

    @Test
    void calculatesEmptyProgressWithoutInventingActivity() {
        when(courses.getCourse(1L)).thenReturn(new CourseResponse(1L, "EECS3311", "Design", "", null, null, null));
        when(reviews.findByCourseIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(attempts.findByCourseId(1L)).thenReturn(List.of());
        var result = service.summarize(1L);
        assertThat(result.hasActivity()).isFalse();
        assertThat(result.overallScore()).isZero();
        assertThat(result.topics()).isEmpty();
    }

    @Test
    void combinesFlashcardAndQuizScoresForTheSelectedCourse() {
        when(courses.getCourse(2L)).thenReturn(new CourseResponse(2L, "EECS3311", "Design", "", null, null, null));
        var card = mock(Flashcard.class);
        when(card.getSources()).thenReturn(List.of());
        var review = mock(FlashcardReview.class);
        when(review.getFlashcard()).thenReturn(card);
        when(review.getRating()).thenReturn(FlashcardReviewRating.EASY);
        var attempt = mock(QuizAttempt.class);
        when(attempt.getCorrectCount()).thenReturn(3);
        when(attempt.getTotalCount()).thenReturn(4);
        when(reviews.findByCourseIdOrderByIdAsc(2L)).thenReturn(List.of(review));
        when(attempts.findByCourseId(2L)).thenReturn(List.of(attempt));
        var result = service.summarize(2L);
        assertThat(result.activityCount()).isEqualTo(2);
        assertThat(result.overallScore()).isEqualTo(87.5);
        assertThat(result.topics()).extracting("topic").containsExactly("Course material", "Quiz performance");
    }
}
