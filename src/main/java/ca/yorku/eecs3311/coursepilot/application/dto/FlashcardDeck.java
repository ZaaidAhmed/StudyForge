package ca.yorku.eecs3311.coursepilot.application.dto;

import java.util.List;

public record FlashcardDeck(Long documentId, String documentName, List<FlashcardResponse> cards) {
    public FlashcardDeck {
        cards = List.copyOf(cards);
    }
}
