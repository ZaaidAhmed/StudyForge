package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.domain.model.Flashcard;
import ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReview;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardReviewRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlashcardReviewServiceTest {
    @Test
    void reviewPersistsTheRequestedRatingWithoutCallingAi() {
        CourseService courses = mock(CourseService.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardReviewRepository reviews = mock(FlashcardReviewRepository.class);
        Flashcard card = mock(Flashcard.class);
        FlashcardReview saved = mock(FlashcardReview.class);
        when(cards.findByIdAndCourseIdWithSources(7L, 1L)).thenReturn(Optional.of(card));
        when(reviews.save(any())).thenReturn(saved);
        when(saved.getFlashcard()).thenReturn(card);
        when(saved.getRating()).thenReturn(ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReviewRating.GOOD);
        var result = new FlashcardReviewService(courses, cards, reviews).review(1L, 7L, "GOOD");
        assertThat(result.rating()).isEqualTo(ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReviewRating.GOOD);
        verify(reviews).save(any(FlashcardReview.class));
    }

    @Test
    void invalidRatingIsRejectedBeforePersistence() {
        CourseService courses = mock(CourseService.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardReviewRepository reviews = mock(FlashcardReviewRepository.class);
        when(cards.findByIdAndCourseIdWithSources(7L, 1L)).thenReturn(Optional.of(mock(Flashcard.class)));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new FlashcardReviewService(courses, cards, reviews).review(1L, 7L, "MAYBE"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(reviews);
    }
}
