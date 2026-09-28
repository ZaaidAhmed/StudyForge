package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.service.CourseNotFoundException;
import ca.yorku.eecs3311.coursepilot.application.service.DuplicateCourseCodeException;
import ca.yorku.eecs3311.coursepilot.application.service.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CourseNotFoundException.class)
    public String courseNotFound(CourseNotFoundException exception, Model model) {
        model.addAttribute("errorTitle", "Course not found");
        model.addAttribute("errorMessage", exception.getMessage());
        return "error/application-error";
    }

    @ExceptionHandler(DuplicateCourseCodeException.class)
    public String duplicateCourse(DuplicateCourseCodeException exception, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/courses";
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public String documentNotFound(DocumentNotFoundException exception, Model model) {
        model.addAttribute("errorTitle", "Material not found");
        model.addAttribute("errorMessage", exception.getMessage());
        return "error/application-error";
    }

    @ExceptionHandler(Exception.class)
    public String unexpected(Exception exception, Model model) {
        log.error("Unexpected web error", exception);
        model.addAttribute("errorTitle", "Something went wrong");
        model.addAttribute("errorMessage", "CoursePilot could not complete that request.");
        return "error/application-error";
    }
}
