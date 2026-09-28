package ca.yorku.eecs3311.coursepilot.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flashcard_id", nullable = false) private Flashcard flashcard;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private FlashcardReviewRating rating;
    @Column(name = "reviewed_at", nullable = false) private Instant reviewedAt;
    protected FlashcardReview() { }
    public FlashcardReview(Flashcard flashcard, FlashcardReviewRating rating) { this.flashcard = flashcard; this.rating = rating; }
    @PrePersist void onCreate() { reviewedAt = Instant.now(); }
    public Long getId() { return id; } public Flashcard getFlashcard() { return flashcard; }
    public FlashcardReviewRating getRating() { return rating; } public Instant getReviewedAt() { return reviewedAt; }
}
