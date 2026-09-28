package ca.yorku.eecs3311.coursepilot.application.dto;

import ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReview;
import ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReviewRating;
import java.time.Instant;

public record FlashcardReviewResponse(Long id, Long flashcardId, FlashcardReviewRating rating, Instant reviewedAt) {
    public static FlashcardReviewResponse from(FlashcardReview review) { return new FlashcardReviewResponse(review.getId(), review.getFlashcard().getId(), review.getRating(), review.getReviewedAt()); }
}
