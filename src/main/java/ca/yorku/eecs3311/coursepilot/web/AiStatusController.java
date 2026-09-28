package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AiStatusController {
    private final StudyAgentFacade facade;
    public AiStatusController(StudyAgentFacade facade) { this.facade = facade; }

    @GetMapping("/ai/status")
    public String status(Model model) {
        model.addAttribute("availability", facade.aiStatus());
        model.addAttribute("activeNav", "ai");
        return "ai/status";
    }
}
