package ca.yorku.eecs3311.coursepilot.application.service;

import ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardReviewResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizAttemptSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto;
import ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanReviewItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StudyPlanReviewService {
    private final CourseMaterialService materials;
    private final ProgressService progress;
    private final FlashcardReviewService flashcards;
    private final QuizAttemptService quizzes;

    public StudyPlanReviewService(CourseMaterialService materials, ProgressService progress,
                                  FlashcardReviewService flashcards, QuizAttemptService quizzes) {
        this.materials = materials;
        this.progress = progress;
        this.flashcards = flashcards;
        this.quizzes = quizzes;
    }

    @Transactional(readOnly = true)
    public List<StudyPlanReviewItem> build(Long courseId, String courseName) {
        List<DocumentResponse> readyDocuments = materials.listDocuments(courseId).stream()
                .filter(document -> "READY".equals(document.processingStatus()))
                .toList();
        List<FlashcardResponse> cards = flashcards.list(courseId);
        List<FlashcardReviewResponse> reviews = flashcards.history(courseId);
        Set<Long> reviewedCardIds = reviews.stream().map(FlashcardReviewResponse::flashcardId).collect(HashSet::new, Set::add, Set::addAll);
        Set<Long> reviewedDocumentIds = cards.stream()
                .filter(card -> reviewedCardIds.contains(card.id()))
                .flatMap(card -> card.sources().stream().map(source -> source.documentId()))
                .collect(HashSet::new, Set::add, Set::addAll);

        ProgressSummary summary = progress.summarize(courseId);
        List<StudyPlanReviewItem> items = new ArrayList<>();
        for (ProgressSummary.TopicProgress topic : summary.topics()) {
            if ("STRONG".equalsIgnoreCase(topic.status())) continue;
            DocumentResponse document = matchingDocument(readyDocuments, topic.topic());
            String title = document == null ? topic.topic() : readableTitle(document.originalFilename());
            String actionUrl = document == null
                    ? "/courses/" + courseId + "/progress"
                    : "/courses/" + courseId + "/materials/" + document.id() + "/chat";
            items.add(new StudyPlanReviewItem(
                    "TOPIC", title, courseName, document == null ? null : document.id(),
                    document == null ? null : document.originalFilename(), null,
                    "Weak topic · " + formatScore(topic.score()) + "% mastery",
                    10, topic.status(), document == null ? "View progress" : "Review material", actionUrl));
        }

        for (DocumentResponse document : readyDocuments) {
            if (reviewedDocumentIds.contains(document.id()) || hasTopicItem(items, document.originalFilename())) continue;
            items.add(new StudyPlanReviewItem(
                    "MATERIAL", readableTitle(document.originalFilename()), courseName, document.id(),
                    document.originalFilename(), null, "Not reviewed · ready to study", 20, "READY",
                    "Open document", "/courses/" + courseId + "/materials/" + document.id()));
        }

        for (QuizResponseDto quiz : quizzes.list(courseId)) {
            List<QuizAttemptSummary> attempts = quizzes.listAttempts(courseId, quiz.id());
            if (attempts.isEmpty()) {
                items.add(new StudyPlanReviewItem(
                        "QUIZ", quiz.title(), courseName, null, null, quiz.id(),
                        "Practice needed · not attempted", 30, "NEW", "Take quiz",
                        "/courses/" + courseId + "/quizzes/" + quiz.id()));
            } else {
                QuizAttemptSummary latest = attempts.getFirst();
                double score = latest.totalCount() == 0 ? 0 : latest.correctCount() * 100d / latest.totalCount();
                if (score < 80) {
                    items.add(new StudyPlanReviewItem(
                            "QUIZ", quiz.title(), courseName, null, null, quiz.id(),
                            "Practice needed · latest score " + formatScore(score) + "%", 40, "NEEDS_REVIEW",
                            "Retry quiz", "/courses/" + courseId + "/quizzes/" + quiz.id()));
                }
            }
        }
        return items.stream()
                .sorted(Comparator.comparingInt(StudyPlanReviewItem::priority).thenComparing(StudyPlanReviewItem::title, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .toList();
    }

    private boolean hasTopicItem(List<StudyPlanReviewItem> items, String filename) {
        return items.stream().anyMatch(item -> filename.equalsIgnoreCase(item.documentName()));
    }

    private DocumentResponse matchingDocument(List<DocumentResponse> documents, String topic) {
        String normalized = topic == null ? "" : topic.toLowerCase(Locale.ROOT);
        return documents.stream().filter(document -> document.originalFilename().toLowerCase(Locale.ROOT).equals(normalized)).findFirst().orElse(null);
    }

    private String readableTitle(String filename) {
        if (filename == null || filename.isBlank()) return "Course material";
        return filename.replaceFirst("(?i)\\.(pdf|txt|md)$", "").replace('_', ' ');
    }

    private String formatScore(double score) {
        return String.valueOf(Math.round(score * 10d) / 10d).replace(".0", "");
    }
}
