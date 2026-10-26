package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.UpdateCourseRequest;
import jakarta.validation.Valid;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ca.yorku.eecs3311.coursepilot.application.service.DuplicateCourseCodeException;

@Controller
@RequestMapping("/courses")
public class CourseController {
    private final StudyAgentFacade facade;
    private final ProgressService progressService;
    private final RecommendationService recommendationService;

    public CourseController(StudyAgentFacade facade, ProgressService progressService, RecommendationService recommendationService) {
        this.facade = facade;
        this.progressService = progressService;
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("courses", facade.listCourses());
        model.addAttribute("activeNav", "courses");
        return "courses/list";
    }

    @GetMapping("/new")
    public String newCourse(Model model) {
        model.addAttribute("courseForm", new CreateCourseRequest("", "", "", null));
        model.addAttribute("formAction", "/courses");
        model.addAttribute("pageTitle", "Create course");
        model.addAttribute("activeNav", "courses");
        return "courses/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("courseForm") CreateCourseRequest request, BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courseForm", request);
            model.addAttribute("formAction", "/courses");
            model.addAttribute("pageTitle", "Create course");
            return "courses/form";
        }
        try {
            var created = facade.createCourse(request);
            redirectAttributes.addFlashAttribute("successMessage", "Course " + created.courseCode() + " was created.");
            return "redirect:/courses/" + created.id();
        } catch (DuplicateCourseCodeException | IllegalArgumentException exception) {
            model.addAttribute("courseError", exception.getMessage());
            model.addAttribute("formAction", "/courses");
            model.addAttribute("pageTitle", "Create course");
            return "courses/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var course = facade.getCourse(id);
        var materials = facade.listMaterials(id);
        var readyMaterials = materials.stream().filter(material -> "READY".equals(material.processingStatus())).toList();
        var processingMaterials = materials.stream().filter(material -> "PROCESSING".equals(material.processingStatus())).count();
        model.addAttribute("course", course);
        model.addAttribute("materialCount", materials.size());
        model.addAttribute("readyMaterialCount", readyMaterials.size());
        model.addAttribute("processingMaterialCount", processingMaterials);
        model.addAttribute("indexedChunkCount", readyMaterials.stream().mapToInt(material -> material.chunkCount()).sum());
        model.addAttribute("progress", progressService.summarize(id));
        model.addAttribute("recommendation", recommendationService.recommend(id));
        if (readyMaterials.isEmpty() && materials.isEmpty()) {
            model.addAttribute("workspaceState", "EMPTY");
            model.addAttribute("primaryActionLabel", "Upload material");
            model.addAttribute("primaryActionHref", "/courses/" + id + "/materials");
            model.addAttribute("primaryActionReason", "Add a PDF, TXT, or Markdown file before searching or asking grounded questions.");
        } else if (readyMaterials.isEmpty()) {
            model.addAttribute("workspaceState", "PROCESSING");
            model.addAttribute("primaryActionLabel", "Check materials");
            model.addAttribute("primaryActionHref", "/courses/" + id + "/materials");
            model.addAttribute("primaryActionReason", "Your material is still being prepared. You can check its status from the Materials page.");
        } else {
            model.addAttribute("workspaceState", "READY");
            model.addAttribute("primaryActionLabel", "Open Study Studio");
            model.addAttribute("primaryActionHref", "/courses/" + id + "/studio");
            model.addAttribute("primaryActionReason", "Read a ready document and ask questions grounded in this course.");
        }
        model.addAttribute("activeNav", "overview");
        return "courses/detail";
    }

    @GetMapping("/{id}/studio")
    public String studyStudio(@PathVariable Long id) {
        var readyMaterial = facade.listMaterials(id).stream()
                .filter(material -> "READY".equals(material.processingStatus()))
                .findFirst();
        if (readyMaterial.isEmpty()) {
            return "redirect:/courses/" + id + "/materials";
        }
        return "redirect:/courses/" + id + "/materials/" + readyMaterial.get().id();
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        var course = facade.getCourse(id);
        model.addAttribute("courseForm", new UpdateCourseRequest(course.courseCode(), course.name(), course.description(), course.examDate()));
        model.addAttribute("formAction", "/courses/" + id + "/edit");
        model.addAttribute("pageTitle", "Edit " + course.courseCode());
        model.addAttribute("activeNav", "courses");
        return "courses/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("courseForm") UpdateCourseRequest request, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courseForm", request);
            model.addAttribute("formAction", "/courses/" + id + "/edit");
            model.addAttribute("pageTitle", "Edit course");
            return "courses/form";
        }
        try {
            var updated = facade.updateCourse(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Course " + updated.courseCode() + " was updated.");
            return "redirect:/courses/" + id;
        } catch (DuplicateCourseCodeException | IllegalArgumentException exception) {
            model.addAttribute("courseError", exception.getMessage());
            model.addAttribute("formAction", "/courses/" + id + "/edit");
            model.addAttribute("pageTitle", "Edit course");
            return "courses/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        facade.deleteCourse(id);
        redirectAttributes.addFlashAttribute("successMessage", "Course was deleted.");
        return "redirect:/courses";
    }
}
