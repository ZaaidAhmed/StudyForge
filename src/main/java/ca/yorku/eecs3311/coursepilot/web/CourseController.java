package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.CreateCourseRequest;
import ca.yorku.eecs3311.coursepilot.application.dto.UpdateCourseRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/courses")
public class CourseController {
    private final StudyAgentFacade facade;

    public CourseController(StudyAgentFacade facade) { this.facade = facade; }

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
        var created = facade.createCourse(request);
        redirectAttributes.addFlashAttribute("successMessage", "Course " + created.courseCode() + " was created.");
        return "redirect:/courses/" + created.id();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("course", facade.getCourse(id));
        model.addAttribute("activeNav", "courses");
        return "courses/detail";
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
        var updated = facade.updateCourse(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Course " + updated.courseCode() + " was updated.");
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        facade.deleteCourse(id);
        redirectAttributes.addFlashAttribute("successMessage", "Course was deleted.");
        return "redirect:/courses";
    }
}
