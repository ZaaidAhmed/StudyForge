package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.RecommendationService;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/courses/{courseId}")
public class CourseWorkspaceController {
    private final StudyAgentFacade facade;
    private final ProgressService progressService;
    private final RecommendationService recommendationService;
    private final StudyPlanService studyPlanService;
    private final LearningHistoryService historyService;

    public CourseWorkspaceController(StudyAgentFacade facade,
                                     ProgressService progressService,
                                     RecommendationService recommendationService,
                                     StudyPlanService studyPlanService,
                                     LearningHistoryService historyService) {
        this.facade = facade;
        this.progressService = progressService;
        this.recommendationService = recommendationService;
        this.studyPlanService = studyPlanService;
        this.historyService = historyService;
    }

    @GetMapping("/tools")
    public String tools(@PathVariable Long courseId, Model model) {
        populateCommon(courseId, model);
        model.addAttribute("activeNav", "tools");
        return "workspace/tools";
    }

    @GetMapping("/practice")
    public String practice(@PathVariable Long courseId, Model model) {
        populateCommon(courseId, model);
        model.addAttribute("flashcardCount", facade.listFlashcards(courseId).size());
        model.addAttribute("quizCount", facade.listQuizzes(courseId).size());
        model.addAttribute("activeNav", "practice");
        return "workspace/practice";
    }

    @GetMapping("/insights")
    public String insights(@PathVariable Long courseId, Model model) {
        populateCommon(courseId, model);
        model.addAttribute("progress", progressService.summarize(courseId));
        model.addAttribute("recommendation", recommendationService.recommend(courseId));
        model.addAttribute("planCount", studyPlanService.list(courseId).size());
        model.addAttribute("historyCount", historyService.list(courseId).size());
        model.addAttribute("activeNav", "insights");
        return "workspace/insights";
    }

    private void populateCommon(Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        var materials = facade.listMaterials(courseId);
        model.addAttribute("materials", materials);
        model.addAttribute("readyMaterialCount", materials.stream()
                .filter(material -> "READY".equals(material.processingStatus())).count());
    }
}
