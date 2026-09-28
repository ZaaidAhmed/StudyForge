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
@RequestMapping("/courses/{courseId}/ask")
public class AskController {
    private final StudyAgentFacade facade;
    public AskController(StudyAgentFacade facade) { this.facade = facade; }

    @GetMapping
    public String form(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("question", "");
        model.addAttribute("activeNav", "ask");
        return "ask/form";
    }

    @PostMapping
    public String ask(@PathVariable Long courseId, @RequestParam(defaultValue = "") String question, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("question", question);
        model.addAttribute("activeNav", "ask");
        try {
            model.addAttribute("answer", facade.askQuestion(courseId, question));
        } catch (GroundedQuestionException | IllegalArgumentException exception) {
            model.addAttribute("askError", exception.getMessage());
        }
        return "ask/form";
    }
}
