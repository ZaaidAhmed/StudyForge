package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.domain.model.Flashcard;
import ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReview;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardReviewRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.List;
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

    @Test
    void deleteRemovesReviewsAndOnlyTheOwnedFlashcard() {
        CourseService courses = mock(CourseService.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardReviewRepository reviews = mock(FlashcardReviewRepository.class);
        Flashcard card = mock(Flashcard.class);
        FlashcardReview review = mock(FlashcardReview.class);
        when(cards.findByIdAndCourseIdWithSources(7L, 1L)).thenReturn(Optional.of(card));
        when(reviews.findByFlashcardId(7L)).thenReturn(List.of(review));

        new FlashcardReviewService(courses, cards, reviews).delete(1L, 7L);

        verify(reviews).deleteAll(List.of(review));
        verify(cards).delete(card);
    }

    @Test
    void deleteDeckRemovesOnlyCardsFromTheSelectedDocument() {
        CourseService courses = mock(CourseService.class);
        FlashcardRepository cards = mock(FlashcardRepository.class);
        FlashcardReviewRepository reviews = mock(FlashcardReviewRepository.class);
        Flashcard card = mock(Flashcard.class);
        when(card.getId()).thenReturn(7L);
        when(cards.findByCourseIdAndSourceDocumentIdWithSources(1L, 9L)).thenReturn(List.of(card));

        new FlashcardReviewService(courses, cards, reviews).deleteDeck(1L, 9L);

        verify(reviews).findByFlashcardId(7L);
        verify(cards).deleteAll(List.of(card));
    }
}
