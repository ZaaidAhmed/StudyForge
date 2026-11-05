package ca.yorku.eecs3311.coursepilot.application.dto;

public record HistoryDisplaySection(String kind, HistoryDayGroup day, FlashcardReviewDeckHistory deck) {
    public static HistoryDisplaySection forDay(HistoryDayGroup day) {
        return new HistoryDisplaySection("DAY", day, null);
    }

    public static HistoryDisplaySection forDeck(FlashcardReviewDeckHistory deck) {
        return new HistoryDisplaySection("FLASHCARD_DECK", null, deck);
    }
}
