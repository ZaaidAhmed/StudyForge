package ca.yorku.eecs3311.coursepilot.domain.repository;

import ca.yorku.eecs3311.coursepilot.domain.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import ca.yorku.eecs3311.coursepilot.domain.model.ProcessingStatus;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    @Query("select chunk from DocumentChunk chunk join fetch chunk.document document where document.id = :documentId order by chunk.chunkIndex asc")
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(@Param("documentId") Long documentId);

    @Query("select chunk from DocumentChunk chunk join fetch chunk.document document " +
            "where document.course.id = :courseId and document.processingStatus = :status " +
            "order by document.id asc, chunk.chunkIndex asc")
    List<DocumentChunk> findSearchableByCourseId(@Param("courseId") Long courseId,
                                                 @Param("status") ProcessingStatus status);

    @Query("select chunk from DocumentChunk chunk join fetch chunk.document document " +
            "where document.course.id = :courseId and document.id = :documentId and document.processingStatus = :status " +
            "order by chunk.chunkIndex asc")
    List<DocumentChunk> findSearchableByCourseIdAndDocumentId(@Param("courseId") Long courseId,
                                                               @Param("documentId") Long documentId,
                                                               @Param("status") ProcessingStatus status);
}
