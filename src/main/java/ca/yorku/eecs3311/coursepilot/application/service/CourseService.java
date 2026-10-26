package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.UpdateCourseRequest;
import ca.yorku.eecs3311.coursepilot.domain.model.Course;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.CourseDocumentRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.DocumentChunkRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.FlashcardReviewRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.QuizAttemptRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.QuizRepository;
import ca.yorku.eecs3311.coursepilot.domain.repository.StudyPlanRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardReviewRepository reviewRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final StudyPlanRepository studyPlanRepository;

    public CourseService(CourseRepository courseRepository, CourseDocumentRepository documentRepository,
                         DocumentChunkRepository chunkRepository, FlashcardRepository flashcardRepository,
                         FlashcardReviewRepository reviewRepository, QuizAttemptRepository quizAttemptRepository,
                         QuizRepository quizRepository, StudyPlanRepository studyPlanRepository) {
        this.courseRepository = courseRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.flashcardRepository = flashcardRepository;
        this.reviewRepository = reviewRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    public CourseResponse createCourse(CreateCourseRequest request) {
        validate(request.courseCode(), request.name(), request.description());
        String code = normalizeCode(request.courseCode());
        ensureUnique(code, null);
        Course course = new Course(code, clean(request.name()), cleanOptional(request.description()), request.examDate());
        try {
            return CourseResponse.from(courseRepository.save(course));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCourseCodeException("A course with code '" + code + "' already exists.");
        }
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream().map(CourseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long id) {
        return CourseResponse.from(findEntity(id));
    }
    public Course getCourseEntity(Long id) { return courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException("Course not found: " + id)); }

    @Transactional(readOnly = true)
    public CourseResponse getCourseByCode(String courseCode) {
        String code = normalizeCode(courseCode);
        return CourseResponse.from(courseRepository.findByCourseCode(code)
                .orElseThrow(() -> notFound(code)));
    }

    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
        validate(request.courseCode(), request.name(), request.description());
        Course course = findEntity(id);
        String code = normalizeCode(request.courseCode());
        ensureUnique(code, id);
        course.update(code, clean(request.name()), cleanOptional(request.description()), request.examDate());
        try {
            return CourseResponse.from(courseRepository.save(course));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCourseCodeException("A course with code '" + code + "' already exists.");
        }
    }

    public void deleteCourse(Long id) {
        Course course = findEntity(id);
        reviewRepository.deleteAll(reviewRepository.findByCourseIdOrderByIdAsc(id));
        flashcardRepository.deleteAll(flashcardRepository.findByCourseIdWithSources(id));
        quizAttemptRepository.deleteAll(quizAttemptRepository.findByCourseId(id));
        quizRepository.deleteAll(quizRepository.findByCourseId(id));
        studyPlanRepository.deleteAll(studyPlanRepository.findByCourseId(id));
        for (var document : documentRepository.findByCourseIdOrderByUploadedAtDesc(id)) {
            chunkRepository.deleteAll(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId()));
        }
        documentRepository.deleteAll(documentRepository.findByCourseIdOrderByUploadedAtDesc(id));
        courseRepository.delete(course);
    }

    public String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private void ensureUnique(String code, Long currentId) {
        courseRepository.findByCourseCode(code).ifPresent(existing -> {
            if (currentId == null || !Objects.equals(existing.getId(), currentId)) {
                throw new DuplicateCourseCodeException("A course with code '" + code + "' already exists.");
            }
        });
    }

    private Course findEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course with id '" + id + "' was not found."));
    }

    private CourseNotFoundException notFound(String code) {
        return new CourseNotFoundException("Course '" + code + "' was not found.");
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String cleanOptional(String value) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void validate(String code, String name, String description) {
        if (normalizeCode(code).isBlank()) throw new IllegalArgumentException("Course code is required.");
        if (clean(name).isBlank()) throw new IllegalArgumentException("Course name is required.");
        if (normalizeCode(code).length() > 32) throw new IllegalArgumentException("Course code must be at most 32 characters.");
        if (clean(name).length() > 160) throw new IllegalArgumentException("Course name must be at most 160 characters.");
        if (description != null && description.trim().length() > 2000) throw new IllegalArgumentException("Description must be at most 2000 characters.");
    }
}
