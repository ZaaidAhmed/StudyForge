package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanResponse;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

@Controller
@RequestMapping("/courses/{courseId}/study-plan")
public class StudyPlanController {
    private final StudyAgentFacade facade;
    private final StudyPlanService plans;
    public StudyPlanController(StudyAgentFacade facade, StudyPlanService plans) { this.facade = facade; this.plans = plans; }
    @GetMapping
    public String page(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        var savedPlans = plans.list(courseId);
        model.addAttribute("plans", savedPlans);
        model.addAttribute("planCount", savedPlans.size());
        model.addAttribute("latestPlan", savedPlans.isEmpty() ? null : savedPlans.getLast());
        model.addAttribute("scheduledMinutes", savedPlans.stream().flatMap(plan -> plan.items().stream()).mapToInt(StudyPlanResponse.Item::minutes).sum());
        model.addAttribute("activeNav", "study-plan");
        return "study-plan/index";
    }
    @PostMapping
    public String create(@PathVariable Long courseId, @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String targetDate, @RequestParam(required = false) String minutes,
                         @RequestParam(defaultValue = "BALANCED") String strategy, Model model) {
        try {
            plans.create(courseId, parseDate(startDate, "Start date is required."),
                    parseDate(targetDate, "Target date is required."), parseMinutes(minutes), strategy);
            model.addAttribute("planMessage", "Study plan created.");
        } catch (RuntimeException exception) {
            model.addAttribute("planError", exception.getMessage() == null ? "Please check the study plan fields." : exception.getMessage());
            model.addAttribute("startDate", startDate);
            model.addAttribute("targetDate", targetDate);
            model.addAttribute("minutes", minutes);
            model.addAttribute("strategy", strategy);
        }
        return page(courseId, model);
    }
    private LocalDate parseDate(String value, String missingMessage) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(missingMessage);
        try { return LocalDate.parse(value); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("Dates must use the YYYY-MM-DD format."); }
    }
    private Integer parseMinutes(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Available study time is required.");
        try { int parsed = Integer.parseInt(value); if (parsed < 1) throw new IllegalArgumentException("Available study time must be positive."); return parsed; }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Available study time must be a whole number."); }
    }
}
