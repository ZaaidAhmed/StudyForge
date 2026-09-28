package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.service.InsightGenerationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/courses/{courseId}")
public class LearningController {
    private final StudyAgentFacade facade;
    public LearningController(StudyAgentFacade facade) { this.facade = facade; }

    @GetMapping("/concepts")
    public String concepts(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("materials", facade.listMaterials(courseId));
        model.addAttribute("activeNav", "concepts");
        return "concepts/index";
    }

    @GetMapping("/materials/{documentId}/summary")
    public String summaryPage(@PathVariable Long courseId, @PathVariable Long documentId, Model model) {
        populateDocument(courseId, documentId, model);
        return "learning/summary";
    }

    @PostMapping("/materials/{documentId}/summary")
    public String summarize(@PathVariable Long courseId, @PathVariable Long documentId, Model model) {
        populateDocument(courseId, documentId, model);
        try { model.addAttribute("summary", facade.summarizeDocument(courseId, documentId)); }
        catch (InsightGenerationException | IllegalArgumentException exception) { model.addAttribute("insightError", exception.getMessage()); }
        return "learning/summary";
    }

    @GetMapping("/materials/{documentId}/concepts")
    public String conceptsPage(@PathVariable Long courseId, @PathVariable Long documentId, Model model) {
        populateDocument(courseId, documentId, model);
        return "learning/concepts";
    }

    @PostMapping("/materials/{documentId}/concepts")
    public String extractConcepts(@PathVariable Long courseId, @PathVariable Long documentId, Model model) {
        populateDocument(courseId, documentId, model);
        try { model.addAttribute("concepts", facade.extractConcepts(courseId, documentId)); }
        catch (InsightGenerationException | IllegalArgumentException exception) { model.addAttribute("insightError", exception.getMessage()); }
        return "learning/concepts";
    }

    @GetMapping("/summary/topic")
    public String topicPage(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("topic", "");
        model.addAttribute("activeNav", "materials");
        return "learning/topic-summary";
    }

    @PostMapping("/summary/topic")
    public String summarizeTopic(@PathVariable Long courseId, @RequestParam(defaultValue = "") String topic, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("topic", topic);
        model.addAttribute("activeNav", "materials");
        try { model.addAttribute("summary", facade.summarizeTopic(courseId, topic)); }
        catch (InsightGenerationException | IllegalArgumentException exception) { model.addAttribute("insightError", exception.getMessage()); }
        return "learning/topic-summary";
    }

    private void populateDocument(Long courseId, Long documentId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("material", facade.getMaterial(courseId, documentId));
        model.addAttribute("activeNav", "materials");
    }
}
