package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final StudyAgentFacade facade;

    public DashboardController(StudyAgentFacade facade) { this.facade = facade; }

    @GetMapping("/")
    public String dashboard(Model model) {
        var courses = facade.listCourses();
        model.addAttribute("courses", courses);
        model.addAttribute("courseCount", courses.size());
        model.addAttribute("activeNav", "dashboard");
        return "dashboard/index";
    }
}
