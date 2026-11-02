package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.FlashcardDeck;
import ca.yorku.eecs3311.coursepilot.application.service.InsightGenerationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/courses/{courseId}/flashcards")
public class FlashcardController {
    private static final Logger log = LoggerFactory.getLogger(FlashcardController.class);
    private final StudyAgentFacade facade;
    public FlashcardController(StudyAgentFacade facade) { this.facade = facade; }
    @GetMapping public String page(@PathVariable Long courseId,
                                   @RequestParam(required = false) Long documentId,
                                   Model model) {
        List<FlashcardResponse> flashcards = populate(courseId, model);
        if (documentId != null) {
            var document = facade.getMaterial(courseId, documentId);
            FlashcardDeck selectedDeck = groupByDocument(flashcards).stream()
                    .filter(deck -> documentId.equals(deck.documentId()))
                    .findFirst()
                    .orElse(new FlashcardDeck(documentId, document.originalFilename(), List.of()));
            model.addAttribute("selectedDocumentId", documentId);
            model.addAttribute("selectedDeck", selectedDeck);
        }
        model.addAttribute("activeNav", "flashcards"); return "flashcards/index";
    }
    @PostMapping("/generate") public String generate(@PathVariable Long courseId, @RequestParam(required = false) Long documentId, @RequestParam(required = false) Integer count, Model model) {
        populate(courseId, model); model.addAttribute("selectedDocumentId", documentId); model.addAttribute("requestedCount", count);
        try {
            if (documentId == null) throw new IllegalArgumentException("Please select a ready document.");
            if (count == null) throw new IllegalArgumentException("Please select a flashcard count.");
            model.addAttribute("generation", facade.generateFlashcards(courseId, documentId, count));
            // Refresh the deck after generation so the newly persisted cards are visible immediately.
            List<FlashcardResponse> refreshed = facade.listFlashcards(courseId);
            model.addAttribute("flashcards", refreshed);
            model.addAttribute("flashcardDecks", groupByDocument(refreshed));
            log.info("Flashcard trace UI refresh: courseId={}, documentId={}, returnedToUi={}, deckCount={}",
                    courseId, documentId, refreshed.size(), groupByDocument(refreshed).stream()
                            .filter(deck -> documentId != null && documentId.equals(deck.documentId()))
                            .mapToInt(deck -> deck.cards().size()).sum());
        }
        catch (InsightGenerationException | IllegalArgumentException exception) { model.addAttribute("flashcardError", exception.getMessage()); }
        return "flashcards/index";
    }
    @GetMapping("/review")
    public String reviewPage(@PathVariable Long courseId,
                            @RequestParam(required = false) Long documentId,
                            @RequestParam(defaultValue = "0") int cardIndex,
                            Model model) {
        List<FlashcardResponse> flashcards = populateReview(courseId, documentId, model);
        return renderReview(model, flashcards, cardIndex);
    }

    @PostMapping("/{cardId}/review")
    public String review(@PathVariable Long courseId,
                         @PathVariable Long cardId,
                         @RequestParam String rating,
                         @RequestParam int cardIndex,
                         @RequestParam(required = false) Long documentId,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        List<FlashcardResponse> flashcards = populateReview(courseId, documentId, model);
        if (cardIndex < 0 || cardIndex >= flashcards.size()
                || !flashcards.get(cardIndex).id().equals(cardId)) {
            model.addAttribute("flashcardError", "The current card does not match this review position.");
            return renderReview(model, flashcards, cardIndex);
        }

        try {
            facade.reviewFlashcard(courseId, cardId, rating);
            redirectAttributes.addFlashAttribute("reviewMessage", "Review saved.");
            String scope = documentId == null ? "" : "&documentId=" + documentId;
            return "redirect:/courses/" + courseId + "/flashcards/review?cardIndex=" + (cardIndex + 1) + scope;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("flashcardError", exception.getMessage());
            return renderReview(model, flashcards, cardIndex);
        }
    }

    @PostMapping("/{cardId}/delete")
    public String delete(@PathVariable Long courseId, @PathVariable Long cardId,
                         RedirectAttributes redirectAttributes) {
        facade.deleteFlashcard(courseId, cardId);
        redirectAttributes.addFlashAttribute("flashcardMessage", "Flashcard deleted.");
        return "redirect:/courses/" + courseId + "/flashcards";
    }

    @PostMapping("/deck/delete")
    public String deleteDeck(@PathVariable Long courseId, @RequestParam Long documentId,
                             RedirectAttributes redirectAttributes) {
        facade.deleteFlashcardDeck(courseId, documentId);
        redirectAttributes.addFlashAttribute("flashcardMessage", "Flashcard deck deleted.");
        return "redirect:/courses/" + courseId + "/flashcards";
    }

    private String renderReview(Model model, List<FlashcardResponse> flashcards, int cardIndex) {
        if (cardIndex < 0 || cardIndex > flashcards.size()) {
            model.addAttribute("flashcardError", "That review position is not available. Restarting the deck.");
            cardIndex = 0;
        }
        model.addAttribute("cardIndex", cardIndex);
        model.addAttribute("reviewComplete", !flashcards.isEmpty() && cardIndex == flashcards.size());
        model.addAttribute("activeNav", "flashcards");
        return "flashcards/review";
    }

    private List<FlashcardResponse> populate(Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("materials", facade.listMaterials(courseId));
        List<FlashcardResponse> flashcards = facade.listFlashcards(courseId);
        model.addAttribute("flashcards", flashcards);
        model.addAttribute("flashcardDecks", groupByDocument(flashcards));
        return flashcards;
    }

    private List<FlashcardResponse> populateReview(Long courseId, Long documentId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("materials", facade.listMaterials(courseId));
        List<FlashcardResponse> flashcards;
        if (documentId == null) {
            model.addAttribute("reviewAll", true);
            flashcards = facade.listFlashcards(courseId);
        } else {
            var document = facade.getMaterial(courseId, documentId);
            model.addAttribute("reviewAll", false);
            model.addAttribute("reviewDocumentId", documentId);
            model.addAttribute("reviewDocumentName", document.originalFilename());
            flashcards = facade.listFlashcards(courseId, documentId);
        }
        model.addAttribute("flashcards", flashcards);
        return flashcards;
    }

    private List<FlashcardDeck> groupByDocument(List<FlashcardResponse> flashcards) {
        Map<Long, FlashcardDeckBuilder> grouped = new LinkedHashMap<>();
        for (FlashcardResponse card : flashcards) {
            if (card.sources().isEmpty()) {
                grouped.computeIfAbsent(null, key -> new FlashcardDeckBuilder(null, "Unlinked cards")).cards.add(card);
                continue;
            }
            var source = card.sources().get(0);
            grouped.computeIfAbsent(source.documentId(), key -> new FlashcardDeckBuilder(source.documentId(), source.documentName())).cards.add(card);
        }
        return grouped.values().stream().map(value -> new FlashcardDeck(value.documentId, value.documentName, value.cards)).toList();
    }

    private static final class FlashcardDeckBuilder {
        private final Long documentId;
        private final String documentName;
        private final List<FlashcardResponse> cards = new ArrayList<>();
        private FlashcardDeckBuilder(Long documentId, String documentName) { this.documentId = documentId; this.documentName = documentName; }
    }
}
