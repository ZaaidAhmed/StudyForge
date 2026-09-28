package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.service.GroundedQuestionException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/courses/{courseId}/materials/{documentId}/chat")
public class MaterialChatController {
    private final StudyAgentFacade facade;
    public MaterialChatController(StudyAgentFacade facade) { this.facade = facade; }

    @GetMapping
    public String chat(@PathVariable Long courseId, @PathVariable Long documentId, Model model) {
        populate(courseId, documentId, "", model);
        return "materials/detail";
    }

    @PostMapping
    public String ask(@PathVariable Long courseId, @PathVariable Long documentId,
                      @RequestParam(defaultValue = "") String question, Model model) {
        populate(courseId, documentId, question, model);
        try {
            model.addAttribute("answer", facade.askQuestionAboutMaterial(courseId, documentId, question));
        } catch (GroundedQuestionException | IllegalArgumentException exception) {
            model.addAttribute("chatError", exception.getMessage());
        }
        return "materials/detail";
    }

    private void populate(Long courseId, Long documentId, String question, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("material", facade.getMaterial(courseId, documentId));
        model.addAttribute("question", question);
        model.addAttribute("activeNav", "materials");
    }
}
