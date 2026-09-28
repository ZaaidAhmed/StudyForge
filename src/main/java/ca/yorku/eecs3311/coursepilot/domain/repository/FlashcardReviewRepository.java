package ca.yorku.eecs3311.coursepilot.domain.repository;

import ca.yorku.eecs3311.coursepilot.domain.model.FlashcardReview;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, Long> {
    @Query("select r from FlashcardReview r join fetch r.flashcard f where f.course.id = :courseId order by r.id asc")
    List<FlashcardReview> findByCourseIdOrderByIdAsc(@Param("courseId") Long courseId);
}
