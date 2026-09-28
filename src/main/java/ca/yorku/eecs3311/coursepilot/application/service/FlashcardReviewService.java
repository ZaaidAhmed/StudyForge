package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.*;
import ca.yorku.eecs3311.coursepilot.domain.model.*;
import ca.yorku.eecs3311.coursepilot.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FlashcardReviewService {
    private final CourseService courseService; private final FlashcardRepository flashcards; private final FlashcardReviewRepository reviews;
    public FlashcardReviewService(CourseService courseService, FlashcardRepository flashcards, FlashcardReviewRepository reviews) { this.courseService = courseService; this.flashcards = flashcards; this.reviews = reviews; }
    @Transactional(readOnly = true) public List<FlashcardResponse> list(Long courseId) { courseService.getCourse(courseId); return flashcards.findByCourseIdWithSources(courseId).stream().map(FlashcardResponse::from).toList(); }
    @Transactional
    public FlashcardReviewResponse review(Long courseId, Long cardId, String rating) {
        courseService.getCourse(courseId); Flashcard card = flashcards.findByIdAndCourseIdWithSources(cardId, courseId).orElseThrow(() -> new DocumentNotFoundException("Flashcard with id '" + cardId + "' was not found for this course."));
        FlashcardReviewRating parsed; try { parsed = FlashcardReviewRating.valueOf(rating == null ? "" : rating.trim().toUpperCase()); } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Rating must be Incorrect, Hard, Good, or Easy."); }
        return FlashcardReviewResponse.from(reviews.save(new FlashcardReview(card, parsed)));
    }
    @Transactional(readOnly = true) public List<FlashcardReviewResponse> history(Long courseId) { courseService.getCourse(courseId); return reviews.findByCourseIdOrderByIdAsc(courseId).stream().map(FlashcardReviewResponse::from).toList(); }
}
