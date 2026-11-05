package ca.yorku.eecs3311.coursepilot.application.dto;

import ca.yorku.eecs3311.coursepilot.domain.model.Flashcard;
import java.time.Instant;
import java.util.List;

public record FlashcardResponse(Long id, Long courseId, String question, String answer, Instant createdAt, List<FlashcardSourceResponse> sources, ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin creationOrigin) {
    public FlashcardResponse(Long id, Long courseId, String question, String answer, Instant createdAt, List<FlashcardSourceResponse> sources) {
        this(id, courseId, question, answer, createdAt, sources, ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.USER);
    }
    public static FlashcardResponse from(Flashcard card) {
        return new FlashcardResponse(card.getId(), card.getCourse().getId(), card.getQuestion(), cleanStudyText(card.getAnswer()), card.getCreatedAt(),
                card.getSources().stream().map(s -> new FlashcardSourceResponse(s.getDocument().getId(), s.getDocument().getOriginalFilename(), s.getChunkId(), s.getChunkIndex(), s.getPageStart(), s.getPageEnd())).toList(), card.getCreationOrigin());
    }

    private static String cleanStudyText(String value) {
        if (value == null) return "";
        return value.replaceAll("(?i)©\\s*\\d{4}[^\\n.]*", "")
                .replaceAll("(?i)\\bSingle\\s+and\\s+Multithreaded\\s+Processes\\s*/\\s*\\d+\\b", "")
                .replaceAll("(?m)^\\s*/\\s*\\d+\\s*$", "")
                .replaceAll("\\s{2,}", " ").trim();
    }
}
