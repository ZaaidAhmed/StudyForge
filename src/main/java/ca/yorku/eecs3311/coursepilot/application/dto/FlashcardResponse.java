package ca.yorku.eecs3311.coursepilot.application.dto;

import ca.yorku.eecs3311.coursepilot.domain.model.Flashcard;
import java.time.Instant;
import java.util.List;

public record FlashcardResponse(Long id, Long courseId, String question, String answer, Instant createdAt, List<FlashcardSourceResponse> sources) {
    public static FlashcardResponse from(Flashcard card) {
        return new FlashcardResponse(card.getId(), card.getCourse().getId(), card.getQuestion(), card.getAnswer(), card.getCreatedAt(),
                card.getSources().stream().map(s -> new FlashcardSourceResponse(s.getDocument().getId(), s.getDocument().getOriginalFilename(), s.getChunkId(), s.getChunkIndex(), s.getPageStart(), s.getPageEnd())).toList());
    }
}
