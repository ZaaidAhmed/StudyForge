package ca.yorku.eecs3311.coursepilot.domain.repository;

import ca.yorku.eecs3311.coursepilot.domain.model.Flashcard;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    @Query("select distinct f from Flashcard f left join fetch f.sources s left join fetch s.document where f.course.id = :courseId order by f.id asc")
    List<Flashcard> findByCourseIdWithSources(@Param("courseId") Long courseId);
    @Query("select distinct f from Flashcard f left join fetch f.sources s left join fetch s.document where f.course.id = :courseId and exists (select 1 from FlashcardSource selected where selected.flashcard = f and selected.document.id = :documentId) order by f.id asc")
    List<Flashcard> findByCourseIdAndSourceDocumentIdWithSources(@Param("courseId") Long courseId, @Param("documentId") Long documentId);
    @Query("select count(distinct f.id) from Flashcard f where f.course.id = :courseId and exists (select 1 from FlashcardSource selected where selected.flashcard = f and selected.document.id = :documentId)")
    long countByCourseIdAndSourceDocumentId(@Param("courseId") Long courseId, @Param("documentId") Long documentId);
    @Query("select distinct f from Flashcard f left join fetch f.sources s left join fetch s.document where f.id = :id and f.course.id = :courseId")
    Optional<Flashcard> findByIdAndCourseIdWithSources(@Param("id") Long id, @Param("courseId") Long courseId);
    @Query("select distinct f from Flashcard f left join fetch f.sources s left join fetch s.document where f.course.id=:courseId and f.creationOrigin=:origin order by f.id asc")
    List<Flashcard> findByCourseIdAndCreationOrigin(@Param("courseId") Long courseId,
                                                      @Param("origin") ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin origin);
}
