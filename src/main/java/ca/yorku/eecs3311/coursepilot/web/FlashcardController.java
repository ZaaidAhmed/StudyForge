package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.service.InsightGenerationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/courses/{courseId}/flashcards")
public class FlashcardController {
    private final StudyAgentFacade facade;
    public FlashcardController(StudyAgentFacade facade) { this.facade = facade; }
    @GetMapping public String page(@PathVariable Long courseId, Model model) {
        populate(courseId, model); model.addAttribute("activeNav", "flashcards"); return "flashcards/index";
    }
    @PostMapping("/generate") public String generate(@PathVariable Long courseId, @RequestParam Long documentId, @RequestParam Integer count, Model model) {
        populate(courseId, model); model.addAttribute("selectedDocumentId", documentId); model.addAttribute("requestedCount", count);
        try { model.addAttribute("generation", facade.generateFlashcards(courseId, documentId, count)); }
        catch (InsightGenerationException | IllegalArgumentException exception) { model.addAttribute("flashcardError", exception.getMessage()); }
        return "flashcards/index";
    }
    @GetMapping("/review") public String reviewPage(@PathVariable Long courseId, Model model) { populate(courseId, model); model.addAttribute("activeNav", "flashcards"); return "flashcards/review"; }
    @PostMapping("/{cardId}/review") public String review(@PathVariable Long courseId, @PathVariable Long cardId, @RequestParam String rating, Model model) {
        try { facade.reviewFlashcard(courseId, cardId, rating); model.addAttribute("reviewMessage", "Review saved: " + rating + "."); }
        catch (IllegalArgumentException exception) { model.addAttribute("flashcardError", exception.getMessage()); }
        populate(courseId, model); model.addAttribute("activeNav", "flashcards"); return "flashcards/review";
    }
    private void populate(Long courseId, Model model) { model.addAttribute("course", facade.getCourse(courseId)); model.addAttribute("materials", facade.listMaterials(courseId)); model.addAttribute("flashcards", facade.listFlashcards(courseId)); }
}
