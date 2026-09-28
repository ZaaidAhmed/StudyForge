package ca.yorku.eecs3311.coursepilot.domain.repository;

import ca.yorku.eecs3311.coursepilot.domain.model.Flashcard;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    @Query("select distinct f from Flashcard f left join fetch f.sources s left join fetch s.document where f.course.id = :courseId order by f.id asc")
    List<Flashcard> findByCourseIdWithSources(@Param("courseId") Long courseId);
    @Query("select distinct f from Flashcard f left join fetch f.sources s left join fetch s.document where f.id = :id and f.course.id = :courseId")
    Optional<Flashcard> findByIdAndCourseIdWithSources(@Param("id") Long id, @Param("courseId") Long courseId);
}
