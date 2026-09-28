package ca.yorku.eecs3311.coursepilot.domain.repository;

import ca.yorku.eecs3311.coursepilot.domain.model.CourseDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseDocumentRepository extends JpaRepository<CourseDocument, Long> {
    List<CourseDocument> findByCourseIdOrderByUploadedAtDesc(Long courseId);
    Optional<CourseDocument> findByCourseIdAndSha256Hash(Long courseId, String sha256Hash);
}
